package io.github.dantte_lp.mutaktor.report

import io.github.dantte_lp.mutaktor.util.JsonBuilder.escapeJson
import io.github.dantte_lp.mutaktor.util.JsonBuilder.quote
import io.github.dantte_lp.mutaktor.util.SourcePathResolver
import io.github.dantte_lp.mutaktor.util.XmlParser
import io.github.dantte_lp.mutaktor.util.optionalTextOf
import io.github.dantte_lp.mutaktor.util.textOf
import org.w3c.dom.Element
import java.io.File

/**
 * Converts PIT `mutations.xml` to mutation-testing-elements JSON
 * (Stryker Dashboard compatible, schema version 2).
 *
 * No external JSON library is used — output is assembled via [StringBuilder].
 */
public object MutationElementsConverter {

    /**
     * Parses [mutationsXml] and returns a mutation-testing-elements JSON string.
     *
     * Mutations are grouped by source file. The [sourceRoot] directory is used
     * to reconstruct relative file paths from PIT's `mutatedClass` and
     * `sourceFile` elements.
     */
    public fun convert(mutationsXml: File, sourceRoot: File): String {
        val mutations = parseMutations(mutationsXml)
        val grouped = mutations.groupBy { it.relativePath }
        return buildJsonReport(grouped, sourceRoot)
    }

    // ── XML parsing ──────────────────────────────────────────────

    private data class Mutation(
        val relativePath: String,
        val mutatedClass: String,
        val mutatedMethod: String,
        val lineNumber: Int,
        val mutator: String,
        val description: String,
        val status: String,
        val killingTest: String?,
        val detected: Boolean,
    )

    private fun parseMutations(xmlFile: File): List<Mutation> {
        val doc = XmlParser.parseSecureXml(xmlFile)

        val nodeList = doc.getElementsByTagName("mutation")
        val result = mutableListOf<Mutation>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val sourceFile = element.textOf("sourceFile")
            val mutatedClass = element.textOf("mutatedClass")
            val relativePath = SourcePathResolver.resolveRelativePath(mutatedClass, sourceFile)

            result += Mutation(
                relativePath = relativePath,
                mutatedClass = mutatedClass,
                mutatedMethod = element.textOf("mutatedMethod"),
                lineNumber = element.textOf("lineNumber").toInt(),
                mutator = element.textOf("mutator"),
                description = element.textOf("description"),
                status = element.getAttribute("status"),
                killingTest = element.optionalTextOf("killingTest"),
                detected = element.getAttribute("detected").toBoolean(),
            )
        }
        return result
    }

    // ── JSON generation ──────────────────────────────────────────

    private fun buildJsonReport(
        grouped: Map<String, List<Mutation>>,
        sourceRoot: File,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "schemaVersion": "2",""")
        sb.appendLine("""  "thresholds": { "high": 80, "low": 60 },""")
        sb.appendLine("""  "projectRoot": ".",""")
        sb.appendLine("""  "files": {""")

        val fileEntries = grouped.entries.toList()
        for ((fileIndex, entry) in fileEntries.withIndex()) {
            val (filePath, mutations) = entry
            val sourceFile = sourceRoot.resolve(filePath)
            val source = if (sourceFile.exists()) escapeJson(sourceFile.readText()) else ""

            sb.appendLine("""    ${quote(filePath)}: {""")
            sb.appendLine("""      "language": "java",""")
            sb.appendLine("""      "source": ${quote(source)},""")
            sb.appendLine("""      "mutants": [""")

            for ((mutantIndex, mutation) in mutations.withIndex()) {
                val trailing = if (mutantIndex < mutations.lastIndex) "," else ""
                sb.appendLine("        {")
                sb.appendLine("""          "id": "${fileIndex * 1000 + mutantIndex + 1}",""")
                sb.appendLine("""          "mutatorName": ${quote(simplifyMutatorName(mutation.mutator))},""")
                sb.appendLine("""          "replacement": ${quote(escapeJson(mutation.description))},""")
                sb.appendLine("""          "location": { "start": { "line": ${mutation.lineNumber}, "column": 1 }, "end": { "line": ${mutation.lineNumber}, "column": 100 } },""")
                sb.appendLine("""          "status": ${quote(mapStatus(mutation.status))},""")
                val killedBy = mutation.killingTest?.takeIf { it.isNotBlank() }
                if (killedBy != null) {
                    val testName = killedBy.substringBefore("(")
                    sb.appendLine("""          "killedBy": [${quote(escapeJson(testName))}]""")
                } else {
                    sb.appendLine("""          "killedBy": []""")
                }
                sb.appendLine("        }$trailing")
            }

            sb.appendLine("      ]")
            val fileTrailing = if (fileIndex < fileEntries.lastIndex) "," else ""
            sb.appendLine("    }$fileTrailing")
        }

        sb.appendLine("  }")
        sb.appendLine("}")
        return sb.toString()
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun mapStatus(pitStatus: String): String = when (pitStatus) {
        "KILLED" -> "Killed"
        "SURVIVED" -> "Survived"
        "NO_COVERAGE" -> "NoCoverage"
        "TIMED_OUT" -> "Timeout"
        "MEMORY_ERROR" -> "RuntimeError"
        "RUN_ERROR" -> "RuntimeError"
        else -> pitStatus
    }

    private fun simplifyMutatorName(fqn: String): String =
        fqn.substringAfterLast('.')
}
