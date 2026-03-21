package io.github.dantte_lp.mutaktor.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element
import org.xml.sax.SAXParseException
import java.io.File
import java.nio.file.Path

class XmlParserTest {

    @TempDir
    lateinit var tempDir: Path

    private fun writeXml(content: String): File {
        val file = tempDir.resolve("test.xml").toFile()
        file.writeText(content)
        return file
    }

    // ── parseSecureXml ──────────────────────────────────────────

    @Test
    fun `parses valid XML document`() {
        val file = writeXml(
            """
            <root>
              <child>hello</child>
            </root>
            """.trimIndent(),
        )

        val doc = XmlParser.parseSecureXml(file)
        doc.documentElement.tagName shouldBe "root"
        doc.getElementsByTagName("child").length shouldBe 1
    }

    @Test
    fun `rejects DOCTYPE declaration`() {
        val file = writeXml(
            """
            <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <root>&xxe;</root>
            """.trimIndent(),
        )

        shouldThrow<SAXParseException> {
            XmlParser.parseSecureXml(file)
        }
    }

    @Test
    fun `parses PIT mutations XML`() {
        val file = writeXml(
            """
            <mutations>
              <mutation detected="true" status="KILLED">
                <sourceFile>Foo.java</sourceFile>
                <mutatedClass>com.example.Foo</mutatedClass>
                <lineNumber>10</lineNumber>
              </mutation>
            </mutations>
            """.trimIndent(),
        )

        val doc = XmlParser.parseSecureXml(file)
        val mutations = doc.getElementsByTagName("mutation")
        mutations.length shouldBe 1

        val element = mutations.item(0) as Element
        element.getAttribute("status") shouldBe "KILLED"
    }

    // ── textOf ──────────────────────────────────────────────────

    @Test
    fun `textOf returns trimmed text content`() {
        val file = writeXml(
            """
            <root>
              <name>  hello world  </name>
            </root>
            """.trimIndent(),
        )

        val doc = XmlParser.parseSecureXml(file)
        val root = doc.documentElement
        root.textOf("name") shouldBe "hello world"
    }

    @Test
    fun `textOf returns nested element text`() {
        val file = writeXml(
            """
            <mutations>
              <mutation>
                <sourceFile>Calculator.java</sourceFile>
                <mutatedClass>com.example.Calculator</mutatedClass>
                <lineNumber>42</lineNumber>
              </mutation>
            </mutations>
            """.trimIndent(),
        )

        val doc = XmlParser.parseSecureXml(file)
        val mutation = doc.getElementsByTagName("mutation").item(0) as Element
        mutation.textOf("sourceFile") shouldBe "Calculator.java"
        mutation.textOf("mutatedClass") shouldBe "com.example.Calculator"
        mutation.textOf("lineNumber") shouldBe "42"
    }

    // ── optionalTextOf ──────────────────────────────────────────

    @Test
    fun `optionalTextOf returns null for missing element`() {
        val file = writeXml("<root><a>text</a></root>")
        val doc = XmlParser.parseSecureXml(file)
        doc.documentElement.optionalTextOf("missing") shouldBe null
    }

    @Test
    fun `optionalTextOf returns null for blank content`() {
        val file = writeXml("<root><a>  </a></root>")
        val doc = XmlParser.parseSecureXml(file)
        doc.documentElement.optionalTextOf("a") shouldBe null
    }

    @Test
    fun `optionalTextOf returns text when present`() {
        val file = writeXml("<root><a>hello</a></root>")
        val doc = XmlParser.parseSecureXml(file)
        doc.documentElement.optionalTextOf("a") shouldBe "hello"
    }

    // ── childElements ───────────────────────────────────────────

    @Test
    fun `childElements returns only element nodes`() {
        val file = writeXml(
            """
            <root>
              <a>1</a>
              <b>2</b>
              <c>3</c>
            </root>
            """.trimIndent(),
        )

        val doc = XmlParser.parseSecureXml(file)
        val children = doc.documentElement.childElements()
        children shouldHaveSize 3
        children[0].tagName shouldBe "a"
        children[1].tagName shouldBe "b"
        children[2].tagName shouldBe "c"
    }

    @Test
    fun `childElements returns empty list for leaf element`() {
        val file = writeXml("<root>text only</root>")
        val doc = XmlParser.parseSecureXml(file)
        doc.documentElement.childElements() shouldHaveSize 0
    }
}
