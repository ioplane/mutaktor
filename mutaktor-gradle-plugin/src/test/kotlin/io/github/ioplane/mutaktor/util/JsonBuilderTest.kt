package io.github.ioplane.mutaktor.util

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test

class JsonBuilderTest {

    // ── escapeJson ──────────────────────────────────────────────

    @Test
    fun `escapes backslash`() {
        JsonBuilder.escapeJson("""foo\bar""") shouldBe """foo\\bar"""
    }

    @Test
    fun `escapes double quotes`() {
        JsonBuilder.escapeJson("""say "hello"""") shouldBe """say \"hello\""""
    }

    @Test
    fun `escapes newline`() {
        JsonBuilder.escapeJson("line1\nline2") shouldBe "line1\\nline2"
    }

    @Test
    fun `escapes carriage return`() {
        JsonBuilder.escapeJson("line1\rline2") shouldBe "line1\\rline2"
    }

    @Test
    fun `escapes tab`() {
        JsonBuilder.escapeJson("col1\tcol2") shouldBe "col1\\tcol2"
    }

    @Test
    fun `escapes control characters`() {
        val input = "hello\u0000world\u001F"
        val result = JsonBuilder.escapeJson(input)
        result shouldContain "\\u0000"
        result shouldContain "\\u001f"
    }

    @Test
    fun `preserves unicode characters`() {
        JsonBuilder.escapeJson("hello \u00E9\u00E8\u00EA") shouldBe "hello \u00E9\u00E8\u00EA"
    }

    @Test
    fun `handles empty string`() {
        JsonBuilder.escapeJson("") shouldBe ""
    }

    @Test
    fun `handles plain string without special chars`() {
        JsonBuilder.escapeJson("hello world 123") shouldBe "hello world 123"
    }

    @Test
    fun `escapes combined special characters`() {
        val input = "path\\to\\file\n\"quoted\"\ttab"
        val result = JsonBuilder.escapeJson(input)
        result shouldBe "path\\\\to\\\\file\\n\\\"quoted\\\"\\ttab"
    }

    // ── quote ───────────────────────────────────────────────────

    @Test
    fun `quote wraps in double quotes`() {
        val result = JsonBuilder.quote("hello")
        result shouldStartWith "\""
        result shouldEndWith "\""
        result shouldBe "\"hello\""
    }

    @Test
    fun `quote escapes content`() {
        JsonBuilder.quote("say \"hi\"") shouldBe "\"say \\\"hi\\\"\""
    }

    @Test
    fun `quote handles empty string`() {
        JsonBuilder.quote("") shouldBe "\"\""
    }
}
