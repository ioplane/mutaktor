package io.github.dantte_lp.mutaktor.ratchet

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RatchetBaselineTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `saves and loads baseline`() {
        val file = File(tempDir, ".mutaktor-baseline.json")
        val scores = mapOf(
            "com.example.service" to MutationRatchet.PackageScore("com.example.service", 80, 50, 40),
            "com.example.model" to MutationRatchet.PackageScore("com.example.model", 100, 10, 10),
        )

        RatchetBaseline.save(file, scores)
        val loaded = RatchetBaseline.load(file)

        loaded shouldHaveSize 2

        loaded["com.example.service"]!!.score shouldBe 80
        loaded["com.example.service"]!!.total shouldBe 50
        loaded["com.example.service"]!!.killed shouldBe 40
        loaded["com.example.service"]!!.packageName shouldBe "com.example.service"

        loaded["com.example.model"]!!.score shouldBe 100
        loaded["com.example.model"]!!.total shouldBe 10
        loaded["com.example.model"]!!.killed shouldBe 10
    }

    @Test
    fun `handles empty baseline file`() {
        val file = File(tempDir, ".mutaktor-baseline.json")
        file.writeText("{}")

        val loaded = RatchetBaseline.load(file)

        loaded.shouldBeEmpty()
    }

    @Test
    fun `handles missing file`() {
        val file = File(tempDir, "nonexistent.json")

        val loaded = RatchetBaseline.load(file)

        loaded.shouldBeEmpty()
    }
}
