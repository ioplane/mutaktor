package io.github.ioplane.mutaktor.ratchet

import io.github.ioplane.mutaktor.util.XmlParser
import io.github.ioplane.mutaktor.util.textOf
import org.w3c.dom.Element
import java.io.File

/**
 * Per-package mutation score ratchet.
 * Ensures mutation scores never decrease — only go up.
 */
public object MutationRatchet {

    public data class PackageScore(
        val packageName: String,
        val score: Int,
        val total: Int,
        val killed: Int,
    )

    public data class RatchetResult(
        val passed: Boolean,
        val regressions: List<Regression>,
        val improvements: List<Improvement>,
        val newPackages: List<PackageScore>,
    )

    public data class Regression(
        val packageName: String,
        val previousScore: Int,
        val currentScore: Int,
    )

    public data class Improvement(
        val packageName: String,
        val previousScore: Int,
        val currentScore: Int,
    )

    /**
     * Statuses that count as "effectively killed" — the mutant was detected
     * by the test suite, whether by assertion failure, timeout, or crash.
     */
    private val KILLED_STATUSES = setOf("KILLED", "TIMED_OUT", "MEMORY_ERROR")

    /**
     * Parse PIT mutations.xml and compute per-package scores.
     *
     * Groups mutations by package (derived from `mutatedClass` FQCN),
     * counts total and killed, and computes an integer percentage score.
     */
    public fun computeScores(mutationsXml: File): Map<String, PackageScore> {
        val doc = XmlParser.parseSecureXml(mutationsXml)
        val nodeList = doc.getElementsByTagName("mutation")

        // Collect (package -> list of statuses)
        val packageMutations = mutableMapOf<String, MutableList<String>>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val status = element.getAttribute("status")
            val mutatedClass = element.textOf("mutatedClass")
            val packageName = extractPackage(mutatedClass)

            packageMutations.getOrPut(packageName) { mutableListOf() }.add(status)
        }

        return buildMap {
            for ((pkg, statuses) in packageMutations) {
                val total = statuses.size
                val killed = statuses.count { it in KILLED_STATUSES }
                val score = if (total == 0) 100 else killed * 100 / total
                put(pkg, PackageScore(
                    packageName = pkg,
                    score = score,
                    total = total,
                    killed = killed,
                ))
            }
        }
    }

    /**
     * Compare current scores against baseline. Fail if any package score dropped.
     */
    public fun evaluate(
        current: Map<String, PackageScore>,
        baseline: Map<String, PackageScore>,
    ): RatchetResult {
        val regressions = mutableListOf<Regression>()
        val improvements = mutableListOf<Improvement>()
        val newPackages = mutableListOf<PackageScore>()

        for ((pkg, currentScore) in current) {
            val baselineScore = baseline[pkg]
            when {
                baselineScore == null -> newPackages.add(currentScore)
                currentScore.score < baselineScore.score -> regressions.add(
                    Regression(
                        packageName = pkg,
                        previousScore = baselineScore.score,
                        currentScore = currentScore.score,
                    )
                )
                currentScore.score > baselineScore.score -> improvements.add(
                    Improvement(
                        packageName = pkg,
                        previousScore = baselineScore.score,
                        currentScore = currentScore.score,
                    )
                )
            }
        }

        return RatchetResult(
            passed = regressions.isEmpty(),
            regressions = regressions.toList(),
            improvements = improvements.toList(),
            newPackages = newPackages.toList(),
        )
    }

    /**
     * Extracts the package name from a fully-qualified class name.
     * e.g. `com.example.service.MyClass` -> `com.example.service`
     * If there is no package (no dot), returns `(default)`.
     */
    private fun extractPackage(fqcn: String): String {
        val lastDot = fqcn.lastIndexOf('.')
        return if (lastDot < 0) "(default)" else fqcn.substring(0, lastDot)
    }
}
