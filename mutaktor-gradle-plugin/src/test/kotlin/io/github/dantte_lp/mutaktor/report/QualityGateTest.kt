package io.github.dantte_lp.mutaktor.report

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class QualityGateTest {

    @TempDir
    lateinit var tempDir: Path

    private fun writeMutationsXml(content: String): File {
        val file = tempDir.resolve("mutations.xml").toFile()
        file.writeText(content)
        return file
    }

    private fun mutationXml(
        status: String,
        detected: Boolean,
        sourceFile: String = "Calculator.java",
        mutatedClass: String = "com.example.Calculator",
        method: String = "add",
        line: Int = 5,
        mutator: String = "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
        description: String = "Replaced integer addition with subtraction",
    ): String = """
        <mutation detected="$detected" status="$status" numberOfTestsRun="1">
          <sourceFile>$sourceFile</sourceFile>
          <mutatedClass>$mutatedClass</mutatedClass>
          <mutatedMethod>$method</mutatedMethod>
          <methodDescription>(II)I</methodDescription>
          <lineNumber>$line</lineNumber>
          <mutator>$mutator</mutator>
          <indexes><index>6</index></indexes>
          <blocks><block>0</block></blocks>
          <killingTest/>
          <description>$description</description>
        </mutation>
    """.trimIndent()

    // ── Tests ────────────────────────────────────────────────────

    @Test
    fun `evaluates passing quality gate`() {
        // 4 killed + 1 survived = 80% kill rate, threshold 60 -> pass
        val xml = writeMutationsXml(
            """
            <mutations>
              ${mutationXml("KILLED", true, line = 1)}
              ${mutationXml("KILLED", true, line = 2)}
              ${mutationXml("KILLED", true, line = 3)}
              ${mutationXml("KILLED", true, line = 4)}
              ${mutationXml("SURVIVED", false, line = 5)}
            </mutations>
            """.trimIndent(),
        )

        val result = QualityGate.evaluate(xml, threshold = 60)

        result.totalMutations shouldBe 5
        result.killedMutations shouldBe 4
        result.survivedMutations shouldBe 1
        result.mutationScore shouldBe 80
        result.passed shouldBe true
        result.threshold shouldBe 60
    }

    @Test
    fun `evaluates failing quality gate`() {
        // 2 killed + 3 survived = 40% kill rate, threshold 60 -> fail
        val xml = writeMutationsXml(
            """
            <mutations>
              ${mutationXml("KILLED", true, line = 1)}
              ${mutationXml("KILLED", true, line = 2)}
              ${mutationXml("SURVIVED", false, line = 3)}
              ${mutationXml("SURVIVED", false, line = 4)}
              ${mutationXml("SURVIVED", false, line = 5)}
            </mutations>
            """.trimIndent(),
        )

        val result = QualityGate.evaluate(xml, threshold = 60)

        result.totalMutations shouldBe 5
        result.killedMutations shouldBe 2
        result.survivedMutations shouldBe 3
        result.mutationScore shouldBe 40
        result.passed shouldBe false
        result.threshold shouldBe 60
    }

    @Test
    fun `handles empty mutations`() {
        val xml = writeMutationsXml(
            """
            <mutations>
            </mutations>
            """.trimIndent(),
        )

        val result = QualityGate.evaluate(xml, threshold = 60)

        result.totalMutations shouldBe 0
        result.killedMutations shouldBe 0
        result.survivedMutations shouldBe 0
        result.mutationScore shouldBe 100
        result.passed shouldBe true
    }

    @Test
    fun `counts survived mutants correctly`() {
        val xml = writeMutationsXml(
            """
            <mutations>
              ${mutationXml("KILLED", true, line = 1)}
              ${mutationXml("SURVIVED", false, line = 10, description = "Replaced integer subtraction with addition")}
              ${mutationXml("KILLED", true, line = 3)}
              ${mutationXml("SURVIVED", false, line = 20, description = "removed call to println")}
            </mutations>
            """.trimIndent(),
        )

        val survived = QualityGate.survivedMutants(xml)

        survived shouldHaveSize 2
        survived[0].line shouldBe 10
        survived[0].description shouldBe "Replaced integer subtraction with addition"
        survived[0].file shouldBe "src/main/java/com/example/Calculator.java"
        survived[1].line shouldBe 20
        survived[1].description shouldBe "removed call to println"
    }

    @Test
    fun `calculates correct mutation score`() {
        // 3 killed out of 4 = 75%
        val xml = writeMutationsXml(
            """
            <mutations>
              ${mutationXml("KILLED", true, line = 1)}
              ${mutationXml("KILLED", true, line = 2)}
              ${mutationXml("KILLED", true, line = 3)}
              ${mutationXml("SURVIVED", false, line = 4)}
            </mutations>
            """.trimIndent(),
        )

        val result = QualityGate.evaluate(xml, threshold = 70)

        result.totalMutations shouldBe 4
        result.killedMutations shouldBe 3
        result.survivedMutations shouldBe 1
        result.mutationScore shouldBe 75
        result.passed shouldBe true
    }
}
