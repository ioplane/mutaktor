package io.github.ioplane.mutaktor.ratchet

import java.io.File

/**
 * Reads and writes `.mutaktor-baseline.json` — per-package mutation scores.
 *
 * Uses simple string parsing (no JSON library) to maintain the project's
 * zero-external-dependency policy.
 */
public object RatchetBaseline {

    /**
     * Loads a baseline file and returns per-package scores.
     *
     * Expected JSON format:
     * ```json
     * {
     *   "com.example.service": { "score": 80, "total": 50, "killed": 40 },
     *   "com.example.model": { "score": 100, "total": 10, "killed": 10 }
     * }
     * ```
     *
     * Returns an empty map if the file does not exist or is empty.
     */
    public fun load(file: File): Map<String, MutationRatchet.PackageScore> {
        if (!file.exists()) return emptyMap()

        val content = file.readText().trim()
        if (content.isEmpty() || content == "{}") return emptyMap()

        val entryPattern = Regex(
            """"([^"]+)"\s*:\s*\{\s*"score"\s*:\s*(\d+)\s*,\s*"total"\s*:\s*(\d+)\s*,\s*"killed"\s*:\s*(\d+)\s*\}"""
        )

        return buildMap {
            for (match in entryPattern.findAll(content)) {
                val (packageName, score, total, killed) = match.destructured
                put(packageName, MutationRatchet.PackageScore(
                    packageName = packageName,
                    score = score.toInt(),
                    total = total.toInt(),
                    killed = killed.toInt(),
                ))
            }
        }
    }

    /**
     * Saves per-package scores to the baseline file as JSON.
     */
    public fun save(file: File, scores: Map<String, MutationRatchet.PackageScore>) {
        val sb = StringBuilder()
        sb.appendLine("{")

        val entries = scores.entries.sortedBy { it.key }
        for ((index, entry) in entries.withIndex()) {
            val (pkg, score) = entry
            sb.append("""  "${pkg}": { "score": ${score.score}, "total": ${score.total}, "killed": ${score.killed} }""")
            if (index < entries.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine()
            }
        }

        sb.appendLine("}")
        file.writeText(sb.toString())
    }
}
