package io.github.ioplane.mutaktor.ratchet

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MutationRatchetTest {

    @TempDir
    lateinit var tempDir: File

    private fun writeMutationsXml(content: String): File {
        val file = File(tempDir, "mutations.xml")
        file.writeText(content)
        return file
    }

    private val sampleXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <mutations>
            <mutation detected="true" status="KILLED" numberOfTestsRun="3">
                <sourceFile>UserService.kt</sourceFile>
                <mutatedClass>com.example.service.UserService</mutatedClass>
                <mutatedMethod>findById</mutatedMethod>
                <methodDescription>(Ljava/lang/Long;)Lcom/example/model/User;</methodDescription>
                <lineNumber>42</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator</mutator>
                <indexes><index>12</index></indexes>
                <blocks><block>3</block></blocks>
                <killingTest>com.example.service.UserServiceTest.testFindById</killingTest>
                <description>negated conditional</description>
            </mutation>
            <mutation detected="false" status="SURVIVED" numberOfTestsRun="3">
                <sourceFile>UserService.kt</sourceFile>
                <mutatedClass>com.example.service.UserService</mutatedClass>
                <mutatedMethod>delete</mutatedMethod>
                <methodDescription>(Ljava/lang/Long;)V</methodDescription>
                <lineNumber>55</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator</mutator>
                <indexes><index>5</index></indexes>
                <blocks><block>1</block></blocks>
                <killingTest/>
                <description>removed call to repository.delete</description>
            </mutation>
            <mutation detected="true" status="KILLED" numberOfTestsRun="2">
                <sourceFile>OrderService.kt</sourceFile>
                <mutatedClass>com.example.order.OrderService</mutatedClass>
                <mutatedMethod>placeOrder</mutatedMethod>
                <methodDescription>()V</methodDescription>
                <lineNumber>10</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator</mutator>
                <indexes><index>8</index></indexes>
                <blocks><block>2</block></blocks>
                <killingTest>com.example.order.OrderServiceTest.testPlaceOrder</killingTest>
                <description>replaced return value</description>
            </mutation>
            <mutation detected="true" status="TIMED_OUT" numberOfTestsRun="1">
                <sourceFile>OrderService.kt</sourceFile>
                <mutatedClass>com.example.order.OrderService</mutatedClass>
                <mutatedMethod>cancelOrder</mutatedMethod>
                <methodDescription>()V</methodDescription>
                <lineNumber>20</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator</mutator>
                <indexes><index>3</index></indexes>
                <blocks><block>1</block></blocks>
                <killingTest/>
                <description>negated conditional</description>
            </mutation>
            <mutation detected="false" status="SURVIVED" numberOfTestsRun="2">
                <sourceFile>OrderService.kt</sourceFile>
                <mutatedClass>com.example.order.OrderService</mutatedClass>
                <mutatedMethod>getTotal</mutatedMethod>
                <methodDescription>()I</methodDescription>
                <lineNumber>30</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                <indexes><index>6</index></indexes>
                <blocks><block>2</block></blocks>
                <killingTest/>
                <description>replaced math operator</description>
            </mutation>
        </mutations>
    """.trimIndent()

    @Test
    fun `computes scores from PIT XML`() {
        val file = writeMutationsXml(sampleXml)
        val scores = MutationRatchet.computeScores(file)

        scores shouldHaveSize 2

        val serviceScore = scores["com.example.service"]!!
        serviceScore.packageName shouldBe "com.example.service"
        serviceScore.total shouldBe 2
        serviceScore.killed shouldBe 1
        serviceScore.score shouldBe 50

        val orderScore = scores["com.example.order"]!!
        orderScore.packageName shouldBe "com.example.order"
        orderScore.total shouldBe 3
        orderScore.killed shouldBe 2
        orderScore.score shouldBe 66 // 2*100/3 = 66
    }

    @Test
    fun `detects regression`() {
        val current = mapOf(
            "com.example.service" to MutationRatchet.PackageScore("com.example.service", 50, 10, 5),
        )
        val baseline = mapOf(
            "com.example.service" to MutationRatchet.PackageScore("com.example.service", 80, 10, 8),
        )

        val result = MutationRatchet.evaluate(current, baseline)

        result.passed.shouldBeFalse()
        result.regressions shouldHaveSize 1
        result.regressions[0].packageName shouldBe "com.example.service"
        result.regressions[0].previousScore shouldBe 80
        result.regressions[0].currentScore shouldBe 50
    }

    @Test
    fun `detects improvement`() {
        val current = mapOf(
            "com.example.service" to MutationRatchet.PackageScore("com.example.service", 90, 10, 9),
        )
        val baseline = mapOf(
            "com.example.service" to MutationRatchet.PackageScore("com.example.service", 80, 10, 8),
        )

        val result = MutationRatchet.evaluate(current, baseline)

        result.passed.shouldBeTrue()
        result.improvements shouldHaveSize 1
        result.improvements[0].packageName shouldBe "com.example.service"
        result.improvements[0].previousScore shouldBe 80
        result.improvements[0].currentScore shouldBe 90
        result.regressions.shouldBeEmpty()
    }

    @Test
    fun `handles new package`() {
        val current = mapOf(
            "com.example.newpkg" to MutationRatchet.PackageScore("com.example.newpkg", 70, 10, 7),
        )
        val baseline = emptyMap<String, MutationRatchet.PackageScore>()

        val result = MutationRatchet.evaluate(current, baseline)

        result.passed.shouldBeTrue()
        result.newPackages shouldHaveSize 1
        result.newPackages[0].packageName shouldBe "com.example.newpkg"
        result.regressions.shouldBeEmpty()
        result.improvements.shouldBeEmpty()
    }

    @Test
    fun `passes when no regressions`() {
        val current = mapOf(
            "com.example.a" to MutationRatchet.PackageScore("com.example.a", 80, 10, 8),
            "com.example.b" to MutationRatchet.PackageScore("com.example.b", 90, 10, 9),
        )
        val baseline = mapOf(
            "com.example.a" to MutationRatchet.PackageScore("com.example.a", 80, 10, 8),
            "com.example.b" to MutationRatchet.PackageScore("com.example.b", 85, 10, 8),
        )

        val result = MutationRatchet.evaluate(current, baseline)

        result.passed.shouldBeTrue()
        result.regressions.shouldBeEmpty()
        // b improved from 85 to 90
        result.improvements shouldHaveSize 1
    }

    @Test
    fun `handles empty baseline`() {
        val current = mapOf(
            "com.example.a" to MutationRatchet.PackageScore("com.example.a", 80, 10, 8),
            "com.example.b" to MutationRatchet.PackageScore("com.example.b", 60, 5, 3),
        )
        val baseline = emptyMap<String, MutationRatchet.PackageScore>()

        val result = MutationRatchet.evaluate(current, baseline)

        result.passed.shouldBeTrue()
        result.regressions.shouldBeEmpty()
        result.improvements.shouldBeEmpty()
        result.newPackages shouldHaveSize 2
    }
}
