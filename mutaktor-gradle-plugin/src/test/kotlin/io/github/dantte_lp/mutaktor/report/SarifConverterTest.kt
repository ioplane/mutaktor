package io.github.dantte_lp.mutaktor.report

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SarifConverterTest {

    @TempDir
    lateinit var tempDir: Path

    private fun writeMutationsXml(content: String): File {
        val file = tempDir.resolve("mutations.xml").toFile()
        file.writeText(content)
        return file
    }

    // ── Tests ────────────────────────────────────────────────────

    @Test
    fun `generates valid SARIF structure`() {
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

        val sarif = SarifConverter.convert(xml, "1.23.0")

        sarif shouldContain """"version": "2.1.0""""
        sarif shouldContain "sarif-schema-2.1.0.json"
        sarif shouldContain """"name": "Mutaktor (PIT)""""
        sarif shouldContain """"version": "1.23.0""""
        sarif shouldContain "https://github.com/dantte-lp/mutaktor"
    }

    @Test
    fun `only reports survived mutations`() {
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

        val sarif = SarifConverter.convert(xml, "1.23.0")

        // Survived mutation should be present
        sarif shouldContain "Survived mutation: Replaced integer subtraction with addition"
        // Killed mutation should NOT be present
        sarif shouldNotContain "Replaced integer addition with subtraction"
    }

    @Test
    fun `includes correct file location`() {
        val xml = writeMutationsXml(
            """
            <mutations>
              <mutation detected="false" status="SURVIVED" numberOfTestsRun="1">
                <sourceFile>Parser.java</sourceFile>
                <mutatedClass>com.example.util.Parser</mutatedClass>
                <mutatedMethod>parse</mutatedMethod>
                <methodDescription>(Ljava/lang/String;)I</methodDescription>
                <lineNumber>42</lineNumber>
                <mutator>org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator</mutator>
                <indexes><index>12</index></indexes>
                <blocks><block>1</block></blocks>
                <killingTest/>
                <description>replaced int return with 0</description>
              </mutation>
            </mutations>
            """.trimIndent()
        )

        val sarif = SarifConverter.convert(xml, "1.23.0")

        sarif shouldContain "src/main/java/com/example/util/Parser.java"
        sarif shouldContain """"startLine": 42"""
        sarif shouldContain """"ruleId": "mutation/survived""""
        sarif shouldContain """"level": "warning""""
    }

    @Test
    fun `handles empty report`() {
        val xml = writeMutationsXml(
            """
            <mutations>
            </mutations>
            """.trimIndent()
        )

        val sarif = SarifConverter.convert(xml, "1.23.0")

        sarif shouldContain """"version": "2.1.0""""
        sarif shouldContain """"results": ["""
        sarif shouldContain """"name": "Mutaktor (PIT)""""
        // No results entries
        sarif shouldNotContain "mutation/survived"
    }
}
