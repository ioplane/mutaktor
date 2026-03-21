package io.github.dantte_lp.mutaktor.report

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converts PIT `mutations.xml` to SARIF 2.1.0 for GitHub Code Scanning.
 *
 * Only **survived** mutations are emitted as results — killed mutations
 * are working correctly and do not need developer attention.
 */
public object SarifConverter {

    private const val SARIF_SCHEMA =
        "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json"

    /**
     * Parses [mutationsXml] and returns a SARIF 2.1.0 JSON string.
     *
     * The [pitVersion] is recorded in the tool driver metadata.
     */
    public fun convert(mutationsXml: File, pitVersion: String): String {
        val survived = parseSurvivedMutations(mutationsXml)
        return buildSarifReport(survived, pitVersion)
    }

    // ── XML parsing ──────────────────────────────────────────────

    private data class SurvivedMutation(
        val relativePath: String,
        val lineNumber: Int,
        val mutator: String,
        val description: String,
    )

    private fun parseSurvivedMutations(xmlFile: File): List<SurvivedMutation> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        doc.documentElement.normalize()

        val nodeList = doc.getElementsByTagName("mutation")
        val result = mutableListOf<SurvivedMutation>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val status = element.getAttribute("status")
            if (status != "SURVIVED") continue

            val sourceFile = element.textOf("sourceFile")
            val mutatedClass = element.textOf("mutatedClass")
            val packagePath = mutatedClass
                .substringBeforeLast('.')
                .replace('.', '/')
            val relativePath = "src/main/java/$packagePath/$sourceFile"

            result += SurvivedMutation(
                relativePath = relativePath,
                lineNumber = element.textOf("lineNumber").toInt(),
                mutator = element.textOf("mutator"),
                description = element.textOf("description"),
            )
        }
        return result
    }

    // ── SARIF generation ─────────────────────────────────────────

    private fun buildSarifReport(mutations: List<SurvivedMutation>, pitVersion: String): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "${"$"}schema": ${quote(SARIF_SCHEMA)},""")
        sb.appendLine("""  "version": "2.1.0",""")
        sb.appendLine("""  "runs": [{""")
        sb.appendLine("""    "tool": {""")
        sb.appendLine("""      "driver": {""")
        sb.appendLine("""        "name": "Mutaktor (PIT)",""")
        sb.appendLine("""        "version": ${quote(escapeJson(pitVersion))},""")
        sb.appendLine("""        "informationUri": "https://github.com/dantte-lp/mutaktor"""")
        sb.appendLine("      }")
        sb.appendLine("    },")
        sb.appendLine("""    "results": [""")

        for ((index, mutation) in mutations.withIndex()) {
            val trailing = if (index < mutations.lastIndex) "," else ""
            sb.appendLine("      {")
            sb.appendLine("""        "ruleId": "mutation/survived",""")
            sb.appendLine("""        "level": "warning",""")
            sb.appendLine("""        "message": { "text": "Survived mutation: ${escapeJson(mutation.description)}" },""")
            sb.appendLine("""        "locations": [{""")
            sb.appendLine("""          "physicalLocation": {""")
            sb.appendLine("""            "artifactLocation": { "uri": ${quote(mutation.relativePath)} },""")
            sb.appendLine("""            "region": { "startLine": ${mutation.lineNumber} }""")
            sb.appendLine("          }")
            sb.appendLine("        }]")
            sb.appendLine("      }$trailing")
        }

        sb.appendLine("    ]")
        sb.appendLine("  }]")
        sb.appendLine("}")
        return sb.toString()
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun quote(value: String): String = "\"$value\""

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun Element.textOf(tag: String): String =
        getElementsByTagName(tag).item(0).textContent.trim()
}
