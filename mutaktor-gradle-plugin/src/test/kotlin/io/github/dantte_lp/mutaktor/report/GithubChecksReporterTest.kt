package io.github.dantte_lp.mutaktor.report

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class GithubChecksReporterTest {

    private fun mutant(
        file: String = "src/main/java/com/example/Foo.java",
        line: Int = 10,
        mutator: String = "org.pitest.mutationtest.engine.gregor.mutators.MathMutator",
        description: String = "Replaced integer addition with subtraction",
    ) = GithubChecksReporter.SurvivedMutant(
        file = file,
        line = line,
        mutator = mutator,
        description = description,
    )

    // ── 1. buildSummary ─────────────────────────────────────────

    @Test
    fun `buildSummary produces correct markdown with survived mutants`() {
        val summary = GithubChecksReporter.buildSummary(
            survivedCount = 3,
            score = 70,
            threshold = 80,
        )

        summary shouldContain "**Mutation Score:** 70%"
        summary shouldContain "threshold: 80%"
        summary shouldContain "3 survived mutant(s) detected"
        summary shouldContain "Review the annotations below for details."
    }

    @Test
    fun `buildSummary produces clean output with zero survived`() {
        val summary = GithubChecksReporter.buildSummary(
            survivedCount = 0,
            score = 100,
            threshold = 80,
        )

        summary shouldContain "**Mutation Score:** 100%"
        summary shouldContain "All mutants were killed. Great test coverage!"
        summary shouldNotContain "survived mutant(s)"
    }

    // ── 2. annotationsJson produces valid JSON array ────────────

    @Test
    fun `annotationsJson produces valid JSON for single mutant`() {
        val json = GithubChecksReporter.annotationsJson(listOf(mutant()))

        json shouldContain """"path":"src/main/java/com/example/Foo.java""""
        json shouldContain """"start_line":10"""
        json shouldContain """"end_line":10"""
        json shouldContain """"annotation_level":"warning""""
        json shouldContain """"message":"""
        json shouldContain "[MathMutator]"
        json shouldContain "Replaced integer addition with subtraction"
    }

    @Test
    fun `annotationsJson produces comma-separated entries for multiple mutants`() {
        val mutants = listOf(
            mutant(line = 10),
            mutant(line = 20),
        )
        val json = GithubChecksReporter.annotationsJson(mutants)

        // Should have exactly one comma between the two objects
        val parts = json.split("},{")
        parts.size shouldBe 2
    }

    // ── 3. annotations are batched in groups of 50 ──────────────

    @Test
    fun `annotations batch size constant is 50`() {
        GithubChecksReporter.ANNOTATION_BATCH_SIZE shouldBe 50
    }

    @Test
    fun `large list would be chunked into batches of 50`() {
        val mutants = (1..120).map { mutant(line = it) }
        val batches = mutants.chunked(GithubChecksReporter.ANNOTATION_BATCH_SIZE)

        batches.size shouldBe 3
        batches[0].size shouldBe 50
        batches[1].size shouldBe 50
        batches[2].size shouldBe 20
    }

    // ── 4. escapeJson handles special characters ────────────────

    @Test
    fun `annotationsJson escapes special characters in mutation descriptions`() {
        val m = mutant(description = "Replaced \"value\" with\nnewline\\backslash")
        val json = GithubChecksReporter.annotationsJson(listOf(m))

        json shouldContain "\\\"value\\\""
        json shouldContain "\\n"
        json shouldContain "\\\\"
        // Should not contain raw unescaped characters
        json shouldNotContain "\n"
    }

    @Test
    fun `annotationsJson escapes special characters in file paths`() {
        val m = mutant(file = "src/main/java/com/example/\"Special\".java")
        val json = GithubChecksReporter.annotationsJson(listOf(m))

        json shouldContain "\\\"Special\\\""
    }

    // ── 5. empty mutants list ───────────────────────────────────

    @Test
    fun `empty mutants list produces empty annotations string`() {
        val json = GithubChecksReporter.annotationsJson(emptyList())

        json shouldBe ""
    }

    @Test
    fun `buildCreateBody with empty annotations produces clean output`() {
        val body = GithubChecksReporter.buildCreateBody(
            sha = "abc123",
            conclusion = "success",
            title = "Mutation Score: 100%",
            summary = "All mutants killed.",
            annotationsJson = "",
        )

        body shouldContain """"name":"Mutaktor""""
        body shouldContain """"head_sha":"abc123""""
        body shouldContain """"status":"completed""""
        body shouldContain """"conclusion":"success""""
        body shouldContain """"annotations":[]"""
    }

    @Test
    fun `buildUpdateBody produces correct JSON structure`() {
        val body = GithubChecksReporter.buildUpdateBody(
            title = "title",
            summary = "summary",
            annotationsJson = "",
        )

        body shouldContain """"output":{"""
        body shouldContain """"title":"title""""
        body shouldContain """"summary":"summary""""
        body shouldContain """"annotations":[]"""
        // Should NOT contain name, head_sha, status, conclusion
        body shouldNotContain "name"
        body shouldNotContain "head_sha"
        body shouldNotContain "status"
        body shouldNotContain "conclusion"
    }
}
