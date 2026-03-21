package io.github.dantte_lp.mutaktor.report

import io.github.dantte_lp.mutaktor.util.SourcePathResolver
import io.github.dantte_lp.mutaktor.util.XmlParser
import io.github.dantte_lp.mutaktor.util.textOf
import org.w3c.dom.Element
import java.io.File

/**
 * Checks mutation score against a configurable threshold.
 *
 * Parses a PIT `mutations.xml` report, computes the kill ratio,
 * and decides whether the quality gate passes.
 */
public object QualityGate {

    public data class Result(
        val totalMutations: Int,
        val killedMutations: Int,
        val survivedMutations: Int,
        val mutationScore: Int,  // 0-100
        val passed: Boolean,
        val threshold: Int,
    )

    /**
     * Statuses that count as "effectively killed" — the mutant was detected
     * by the test suite, whether by assertion failure, timeout, or crash.
     */
    private val KILLED_STATUSES = setOf("KILLED", "TIMED_OUT", "MEMORY_ERROR")

    /**
     * Parses PIT `mutations.xml` and evaluates the quality gate.
     *
     * The mutation score is `killed * 100 / total` (integer division).
     * If there are zero mutations the score is 100 (nothing to test).
     *
     * @param mutationsXml the PIT XML report file
     * @param threshold minimum required mutation score (0-100)
     */
    public fun evaluate(mutationsXml: File, threshold: Int): Result {
        val statuses = parseMutationStatuses(mutationsXml)
        val total = statuses.size
        val killed = statuses.count { it in KILLED_STATUSES }
        val survived = statuses.count { it == "SURVIVED" }
        val score = if (total == 0) 100 else killed * 100 / total

        return Result(
            totalMutations = total,
            killedMutations = killed,
            survivedMutations = survived,
            mutationScore = score,
            passed = score >= threshold,
            threshold = threshold,
        )
    }

    /**
     * Returns the list of survived mutants for reporting (e.g. GitHub annotations).
     *
     * @param mutationsXml the PIT XML report file
     */
    public fun survivedMutants(mutationsXml: File): List<GithubChecksReporter.SurvivedMutant> {
        val doc = XmlParser.parseSecureXml(mutationsXml)

        val nodeList = doc.getElementsByTagName("mutation")
        val result = mutableListOf<GithubChecksReporter.SurvivedMutant>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val status = element.getAttribute("status")
            if (status != "SURVIVED") continue

            val sourceFile = element.textOf("sourceFile")
            val mutatedClass = element.textOf("mutatedClass")
            val relativePath = SourcePathResolver.resolveRelativePath(mutatedClass, sourceFile)

            result += GithubChecksReporter.SurvivedMutant(
                file = relativePath,
                line = element.textOf("lineNumber").toInt(),
                mutator = element.textOf("mutator"),
                description = element.textOf("description"),
            )
        }
        return result
    }

    // -- Internal helpers -----------------------------------------------------

    private fun parseMutationStatuses(xmlFile: File): List<String> {
        val doc = XmlParser.parseSecureXml(xmlFile)

        val nodeList = doc.getElementsByTagName("mutation")
        val statuses = mutableListOf<String>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            statuses += element.getAttribute("status")
        }
        return statuses
    }
}
