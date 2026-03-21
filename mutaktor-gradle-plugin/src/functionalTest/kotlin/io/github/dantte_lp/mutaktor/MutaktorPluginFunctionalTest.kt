package io.github.dantte_lp.mutaktor

import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MutaktorPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `mutate task runs successfully`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                java
                id("io.github.dantte-lp.mutaktor")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("mutate")
            .withPluginClasspath()
            .build()

        result.output shouldContain "Mutaktor"
    }

    @Test
    fun `plugin works with configuration cache`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                java
                id("io.github.dantte-lp.mutaktor")
            }
        """.trimIndent())

        // First run — store cache
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("mutate", "--configuration-cache")
            .withPluginClasspath()
            .build()

        // Second run — reuse cache
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("mutate", "--configuration-cache")
            .withPluginClasspath()
            .build()

        result.output shouldContain "Reusing configuration cache"
    }
}
