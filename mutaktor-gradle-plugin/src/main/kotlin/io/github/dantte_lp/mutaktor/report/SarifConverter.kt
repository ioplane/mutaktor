package io.github.dantte_lp.mutaktor.report

import io.github.dantte_lp.mutaktor.util.JsonBuilder.escapeJson
import io.github.dantte_lp.mutaktor.util.JsonBuilder.quote
import io.github.dantte_lp.mutaktor.util.SourcePathResolver
import io.github.dantte_lp.mutaktor.util.XmlParser
import io.github.dantte_lp.mutaktor.util.textOf
import org.w3c.dom.Element
import java.io.File

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
        val doc = XmlParser.parseSecureXml(xmlFile)

        val nodeList = doc.getElementsByTagName("mutation")
        val result = mutableListOf<SurvivedMutation>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val status = element.getAttribute("status")
            if (status != "SURVIVED") continue

            val sourceFile = element.textOf("sourceFile")
            val mutatedClass = element.textOf("mutatedClass")
            val relativePath = SourcePathResolver.resolveRelativePath(mutatedClass, sourceFile)

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
}
