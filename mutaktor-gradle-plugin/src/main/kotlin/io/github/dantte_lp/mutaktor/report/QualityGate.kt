package io.github.dantte_lp.mutaktor.report

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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
        val killed = statuses.count { it == "KILLED" }
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
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(mutationsXml)
        doc.documentElement.normalize()

        val nodeList = doc.getElementsByTagName("mutation")
        val result = mutableListOf<GithubChecksReporter.SurvivedMutant>()

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
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        doc.documentElement.normalize()

        val nodeList = doc.getElementsByTagName("mutation")
        val statuses = mutableListOf<String>()

        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            statuses += element.getAttribute("status")
        }
        return statuses
    }

    private fun Element.textOf(tag: String): String =
        getElementsByTagName(tag).item(0).textContent.trim()
}
