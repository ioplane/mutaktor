package io.github.ioplane.mutaktor.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Shared XML parsing utilities with secure defaults (DTD disabled).
 */
public object XmlParser {

    /**
     * Parses [file] into a DOM [Document] with external entities and DTDs disabled.
     *
     * @throws org.xml.sax.SAXParseException if the document contains a DOCTYPE declaration
     */
    public fun parseSecureXml(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        doc.documentElement.normalize()
        return doc
    }
}

/**
 * Returns the trimmed text content of the first child element matching [tag].
 *
 * @throws NullPointerException if no child element with the given tag exists
 */
public fun Element.textOf(tag: String): String =
    getElementsByTagName(tag).item(0).textContent.trim()

/**
 * Returns the trimmed text content of the first child element matching [tag],
 * or `null` if no such element exists or its content is blank.
 */
public fun Element.optionalTextOf(tag: String): String? {
    val nodes = getElementsByTagName(tag)
    if (nodes.length == 0) return null
    val text = nodes.item(0).textContent.trim()
    return text.ifBlank { null }
}

/**
 * Returns all direct child nodes that are [Element]s.
 */
public fun Element.childElements(): List<Element> {
    val result = mutableListOf<Element>()
    val nodes = childNodes
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            result += node as Element
        }
    }
    return result
}
