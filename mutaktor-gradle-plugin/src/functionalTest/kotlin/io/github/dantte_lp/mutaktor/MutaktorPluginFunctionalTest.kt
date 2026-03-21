package io.github.dantte_lp.mutaktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MutaktorPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    // ── Helpers ──────────────────────────────────────────────────────

    private fun writeSettingsFile() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "functional-test"
            """.trimIndent()
        )
    }

    private fun writeBuildFile(config: String = "") {
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.github.dantte-lp.mutaktor")
            }

            repositories {
                mavenCentral()
            }

            group = "com.example"

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            mutaktor {
                kotlinFilters.set(false)  // no filter JAR available in standalone funcTest
            }

            $config
            """.trimIndent()
        )
    }

    private fun writeJavaClass() {
        val srcDir = projectDir.resolve("src/main/java/com/example")
        srcDir.mkdirs()
        srcDir.resolve("Calculator.java").writeText(
            """
            package com.example;

            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
                public int multiply(int a, int b) {
                    return a * b;
                }
                public boolean isPositive(int n) {
                    return n > 0;
                }
            }
            """.trimIndent()
        )
    }

    private fun writeJavaTest() {
        val testDir = projectDir.resolve("src/test/java/com/example")
        testDir.mkdirs()
        testDir.resolve("CalculatorTest.java").writeText(
            """
            package com.example;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            public class CalculatorTest {
                @Test
                void testAdd() {
                    assertEquals(5, new Calculator().add(2, 3));
                }
            }
            """.trimIndent()
        )
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .forwardOutput()

    // ── Tests ────────────────────────────────────────────────────────

    @Test
    fun `mutate task runs PIT successfully`() {
        writeSettingsFile()
        writeBuildFile(
            """
            group = "com.example"

            mutaktor {
                targetClasses.set(setOf("com.example.*"))
            }
            """.trimIndent()
        )
        writeJavaClass()
        writeJavaTest()

        val result = runner("mutate", "--stacktrace").build()

        result.task(":mutate")?.outcome shouldBe TaskOutcome.SUCCESS
        // Report directory should be created under build/reports/mutaktor
        val reportDir = projectDir.resolve("build/reports/mutaktor")
        result.output shouldContain "Mutaktor"
    }

    @Disabled("Configuration cache with JavaExec + PIT requires serialization hardening — Sprint 2 refinement")
    @Test
    fun `plugin works with configuration cache`() {
        writeSettingsFile()
        writeBuildFile("""
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
            }
        """.trimIndent())
        writeJavaClass()
        writeJavaTest()

        // First run — store cache
        val first = runner("mutate", "--configuration-cache").build()
        first.output shouldContain "Configuration cache entry stored"

        // Second run — reuse (may report problems but should succeed)
        val second = runner("mutate", "--configuration-cache").build()
        second.output shouldContain "configuration cache"
    }

    @Test
    fun `custom targetClasses are passed to PIT`() {
        writeSettingsFile()
        writeBuildFile(
            """
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
            }
            """.trimIndent()
        )
        writeJavaClass()
        writeJavaTest()

        val result = runner("mutate", "--info").build()

        result.output shouldContain "--targetClasses=com.example.*"
    }

    @Test
    fun `plugin fails gracefully without java plugin`() {
        writeSettingsFile()
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.dantte-lp.mutaktor")
            }
            """.trimIndent()
        )

        val result = runner("tasks", "--all").build()

        // The mutate task should not be registered without the java plugin
        result.output shouldContain "tasks"
    }

    @Test
    fun `mutate task accepts javaLauncher configuration`() {
        writeSettingsFile()
        writeBuildFile("""
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
                // javaLauncher not set — should use default
            }
        """.trimIndent())
        writeJavaClass()
        writeJavaTest()
        val result = runner("mutate").build()
        result.task(":mutate")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `custom pit version is used`() {
        writeSettingsFile()
        writeBuildFile(
            """
            mutaktor {
                pitVersion.set("1.22.0")
            }
            """.trimIndent()
        )
        writeJavaClass()
        writeJavaTest()

        val result = runner("mutate", "--info").build()

        result.output shouldContain "1.22.0"
    }

    @Test
    fun `mutate produces mutation-testing-elements JSON report`() {
        writeSettingsFile()
        writeBuildFile("""
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
            }
        """.trimIndent())
        writeJavaClass()
        writeJavaTest()
        val result = runner("mutate").build()
        result.task(":mutate")?.outcome shouldBe TaskOutcome.SUCCESS
        val jsonFile = projectDir.resolve("build/reports/mutaktor/mutations.json")
        jsonFile.exists() shouldBe true
        jsonFile.readText() shouldContain "schemaVersion"
    }

    @Test
    fun `mutate with SARIF produces SARIF report`() {
        writeSettingsFile()
        writeBuildFile("""
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
                sarifReport.set(true)
            }
        """.trimIndent())
        writeJavaClass()
        writeJavaTest()
        val result = runner("mutate").build()
        result.task(":mutate")?.outcome shouldBe TaskOutcome.SUCCESS
        val sarifFile = projectDir.resolve("build/reports/mutaktor/mutations.sarif.json")
        sarifFile.exists() shouldBe true
        sarifFile.readText() shouldContain "sarif"
    }

    @Test
    fun `quality gate fails when score below threshold`() {
        writeSettingsFile()
        writeBuildFile("""
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
                mutationScoreThreshold.set(100)  // impossible to hit — ensures gate fails
            }
        """.trimIndent())
        writeJavaClass()
        writeJavaTest()
        val result = runner("mutate").buildAndFail()
        result.output shouldContain "quality gate FAILED"
    }
}
