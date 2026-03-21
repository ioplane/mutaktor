package io.github.dantte_lp.mutaktor.report

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Logger

/**
 * Reports surviving mutations as GitHub Check Run annotations.
 * Each survived mutant becomes a warning annotation on the source line.
 *
 * Requires: GITHUB_TOKEN, GITHUB_REPOSITORY, GITHUB_SHA environment variables.
 */
public object GithubChecksReporter {

    private val logger: Logger = Logger.getLogger(GithubChecksReporter::class.java.name)

    /** Maximum annotations per GitHub API request. */
    private const val ANNOTATION_BATCH_SIZE = 50

    public data class SurvivedMutant(
        val file: String,       // relative path: src/main/kotlin/com/example/Foo.kt
        val line: Int,
        val mutator: String,
        val description: String,
    )

    /**
     * Creates a Check Run named "Mutaktor" with annotations for each survived mutant.
     *
     * If more than 50 mutants are provided, the initial request creates the check run
     * with the first batch and subsequent PATCH requests add the remaining annotations.
     *
     * @param token GitHub token (GITHUB_TOKEN)
     * @param repository owner/repo (GITHUB_REPOSITORY)
     * @param sha commit SHA (GITHUB_SHA)
     * @param mutants list of survived mutants to annotate
     * @param mutationScore overall mutation score (0-100)
     * @param threshold minimum required score (fails check if below)
     */
    public fun report(
        token: String,
        repository: String,
        sha: String,
        mutants: List<SurvivedMutant>,
        mutationScore: Int,
        threshold: Int,
    ) {
        val client = HttpClient.newHttpClient()
        val conclusion = if (mutationScore >= threshold) "success" else "failure"
        val title = "Mutation Score: $mutationScore% (threshold: $threshold%)"
        val summary = buildSummary(mutants.size, mutationScore, threshold)

        val batches = if (mutants.isEmpty()) {
            listOf(emptyList())
        } else {
            mutants.chunked(ANNOTATION_BATCH_SIZE)
        }

        // First request: create the check run
        val firstAnnotationsJson = annotationsJson(batches.first())
        val createBody = buildCreateBody(sha, conclusion, title, summary, firstAnnotationsJson)

        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$repository/check-runs"))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .build()

        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        if (createResponse.statusCode() !in 200..201) {
            logger.severe("Failed to create check run: HTTP ${createResponse.statusCode()} — ${createResponse.body()}")
            return
        }

        val checkRunId = extractJsonField(createResponse.body(), "id")
        val htmlUrl = extractJsonStringField(createResponse.body(), "html_url")
        logger.info("Created check run: $htmlUrl")

        // Remaining batches: update the check run with additional annotations
        for (batchIndex in 1 until batches.size) {
            val batchAnnotationsJson = annotationsJson(batches[batchIndex])
            val updateBody = buildUpdateBody(title, summary, batchAnnotationsJson)

            val updateRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/$repository/check-runs/$checkRunId"))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(updateBody))
                .build()

            val updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())
            if (updateResponse.statusCode() !in 200..201) {
                logger.warning(
                    "Failed to update check run with batch ${batchIndex + 1}: " +
                        "HTTP ${updateResponse.statusCode()} — ${updateResponse.body()}",
                )
            }
        }
    }

    // -- JSON body builders ---------------------------------------------------

    private fun buildCreateBody(
        sha: String,
        conclusion: String,
        title: String,
        summary: String,
        annotationsJson: String,
    ): String = buildString {
        append('{')
        append(""""name":"Mutaktor",""")
        append(""""head_sha":${quote(escapeJson(sha))},""")
        append(""""status":"completed",""")
        append(""""conclusion":${quote(conclusion)},""")
        append(""""output":{""")
        append(""""title":${quote(escapeJson(title))},""")
        append(""""summary":${quote(escapeJson(summary))},""")
        append(""""annotations":[$annotationsJson]""")
        append('}')
        append('}')
    }

    private fun buildUpdateBody(
        title: String,
        summary: String,
        annotationsJson: String,
    ): String = buildString {
        append('{')
        append(""""output":{""")
        append(""""title":${quote(escapeJson(title))},""")
        append(""""summary":${quote(escapeJson(summary))},""")
        append(""""annotations":[$annotationsJson]""")
        append('}')
        append('}')
    }

    private fun annotationsJson(mutants: List<SurvivedMutant>): String =
        mutants.joinToString(",") { mutant ->
            buildString {
                append('{')
                append(""""path":${quote(escapeJson(mutant.file))},""")
                append(""""start_line":${mutant.line},""")
                append(""""end_line":${mutant.line},""")
                append(""""annotation_level":"warning",""")
                append(""""message":${quote(escapeJson("[${simplifyMutator(mutant.mutator)}] ${mutant.description}"))}""")
                append('}')
            }
        }

    private fun buildSummary(survivedCount: Int, score: Int, threshold: Int): String = buildString {
        append("**Mutation Score:** $score% (threshold: $threshold%)\\n\\n")
        if (survivedCount > 0) {
            append("$survivedCount survived mutant(s) detected. ")
            append("Review the annotations below for details.")
        } else {
            append("All mutants were killed. Great test coverage!")
        }
    }

    // -- Helpers --------------------------------------------------------------

    private fun simplifyMutator(fqn: String): String =
        fqn.substringAfterLast('.')

    private fun quote(value: String): String = "\"$value\""

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    /** Extracts a numeric JSON field value (e.g. "id": 123). */
    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*(\d+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
            ?: error("Could not extract '$field' from response")
    }

    /** Extracts a string JSON field value (e.g. "html_url": "..."). */
    private fun extractJsonStringField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
            ?: error("Could not extract '$field' from response")
    }
}
