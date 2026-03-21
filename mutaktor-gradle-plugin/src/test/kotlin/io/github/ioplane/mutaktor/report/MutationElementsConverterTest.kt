package io.github.ioplane.mutaktor.report

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MutationElementsConverterTest {

    @TempDir
    lateinit var tempDir: Path

    private fun writeMutationsXml(content: String): File {
        val file = tempDir.resolve("mutations.xml").toFile()
        file.writeText(content)
        return file
    }

    // ── Tests ────────────────────────────────────────────────────

    @Test
    fun `converts single killed mutation`() {
        val xml = writeMutationsXml(
            """
            <mutations>
              <mutation detected="true" status="KILLED" numberOfTestsRun="1">
                <sourceFile>Calculator.java</sourceFile>
                <mutatedClass>com.example.Calculator</mutatedClass>
                <mutatedMethod>add</mutatedMethod>
                <methodDescription>(II)I</methodDescription>
                <lineNumber>5</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                <indexes><index>6</index></indexes>
                <blocks><block>0</block></blocks>
                <killingTest>com.example.CalculatorTest.testAdd(com.example.CalculatorTest)</killingTest>
                <description>Replaced integer addition with subtraction</description>
              </mutation>
            </mutations>
            """.trimIndent()
        )

        val json = MutationElementsConverter.convert(xml, tempDir.toFile())

        json shouldContain """"schemaVersion": "2""""
        json shouldContain """"thresholds": { "high": 80, "low": 60 }"""
        json shouldContain """"projectRoot": ".""""
        json shouldContain "src/main/java/com/example/Calculator.java"
        json shouldContain """"mutatorName": "MathMutator""""
        json shouldContain """"status": "Killed""""
        json shouldContain """"replacement": "Replaced integer addition with subtraction""""
        json shouldContain """"line": 5"""
        json shouldContain "com.example.CalculatorTest.testAdd"
    }

    @Test
    fun `converts survived mutation`() {
        val xml = writeMutationsXml(
            """
            <mutations>
              <mutation detected="false" status="SURVIVED" numberOfTestsRun="3">
                <sourceFile>Calculator.java</sourceFile>
                <mutatedClass>com.example.Calculator</mutatedClass>
                <mutatedMethod>subtract</mutatedMethod>
                <methodDescription>(II)I</methodDescription>
                <lineNumber>10</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                <indexes><index>6</index></indexes>
                <blocks><block>0</block></blocks>
                <killingTest/>
                <description>Replaced integer subtraction with addition</description>
              </mutation>
            </mutations>
            """.trimIndent()
        )

        val json = MutationElementsConverter.convert(xml, tempDir.toFile())

        json shouldContain """"status": "Survived""""
        json shouldContain """"killedBy": []"""
    }

    @Test
    fun `groups mutations by source file`() {
        val xml = writeMutationsXml(
            """
            <mutations>
              <mutation detected="true" status="KILLED" numberOfTestsRun="1">
                <sourceFile>Calculator.java</sourceFile>
                <mutatedClass>com.example.Calculator</mutatedClass>
                <mutatedMethod>add</mutatedMethod>
                <methodDescription>(II)I</methodDescription>
                <lineNumber>5</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
                <indexes><index>6</index></indexes>
                <blocks><block>0</block></blocks>
                <killingTest>com.example.CalculatorTest.testAdd(com.example.CalculatorTest)</killingTest>
                <description>Replaced integer addition with subtraction</description>
              </mutation>
              <mutation detected="true" status="KILLED" numberOfTestsRun="1">
                <sourceFile>Parser.java</sourceFile>
                <mutatedClass>com.example.util.Parser</mutatedClass>
                <mutatedMethod>parse</mutatedMethod>
                <methodDescription>(Ljava/lang/String;)I</methodDescription>
                <lineNumber>20</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator</mutator>
                <indexes><index>12</index></indexes>
                <blocks><block>1</block></blocks>
                <killingTest>com.example.util.ParserTest.testParse(com.example.util.ParserTest)</killingTest>
                <description>replaced int return with 0</description>
              </mutation>
            </mutations>
            """.trimIndent()
        )

        val json = MutationElementsConverter.convert(xml, tempDir.toFile())

        json shouldContain "src/main/java/com/example/Calculator.java"
        json shouldContain "src/main/java/com/example/util/Parser.java"
        // Each file section should have its own mutants array
        val calculatorSection = json.substringAfter("Calculator.java")
        calculatorSection shouldContain "MathMutator"
        val parserSection = json.substringAfter("Parser.java")
        parserSection shouldContain "ReturnValsMutator"
    }

    @Test
    fun `handles empty mutations file`() {
        val xml = writeMutationsXml(
            """
            <mutations>
            </mutations>
            """.trimIndent()
        )

        val json = MutationElementsConverter.convert(xml, tempDir.toFile())

        json shouldContain """"schemaVersion": "2""""
        json shouldContain """"files": {"""
        // No file entries inside files block
        json shouldNotContain "mutants"
    }

    @Test
    fun `maps PIT status correctly`() {
        fun xmlWithStatus(status: String, detected: Boolean): String = """
            <mutations>
              <mutation detected="$detected" status="$status" numberOfTestsRun="1">
                <sourceFile>Foo.java</sourceFile>
                <mutatedClass>com.example.Foo</mutatedClass>
                <mutatedMethod>bar</mutatedMethod>
                <methodDescription>()V</methodDescription>
                <lineNumber>1</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator</mutator>
                <indexes><index>1</index></indexes>
                <blocks><block>0</block></blocks>
                <killingTest/>
                <description>removed call</description>
              </mutation>
            </mutations>
        """.trimIndent()

        val cases = listOf(
            "KILLED" to "Killed",
            "SURVIVED" to "Survived",
            "NO_COVERAGE" to "NoCoverage",
        )

        for ((pitStatus, expectedStatus) in cases) {
            val detected = pitStatus == "KILLED"
            val file = tempDir.resolve("mutations_$pitStatus.xml").toFile()
            file.writeText(xmlWithStatus(pitStatus, detected))

            val json = MutationElementsConverter.convert(file, tempDir.toFile())
            json shouldContain """"status": "$expectedStatus""""
        }
    }
}
