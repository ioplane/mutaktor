package io.github.dantte_lp.mutaktor.extreme

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class ExtremeMutationConfigTest {

    @Test
    fun `extreme mutators contains expected set`() {
        ExtremeMutationConfig.EXTREME_MUTATORS shouldContainExactlyInAnyOrder listOf(
            "VOID_METHOD_CALLS",
            "EMPTY_RETURNS",
            "FALSE_RETURNS",
            "TRUE_RETURNS",
            "NULL_RETURNS",
            "PRIMITIVE_RETURNS",
        )
    }

    @Test
    fun `applyTo replaces existing mutators`() {
        val result = ExtremeMutationConfig.applyTo(setOf("DEFAULTS"))
        result shouldBe ExtremeMutationConfig.EXTREME_MUTATORS
    }

    @Test
    fun `description is not empty`() {
        ExtremeMutationConfig.DESCRIPTION.shouldNotBeEmpty()
    }
}
