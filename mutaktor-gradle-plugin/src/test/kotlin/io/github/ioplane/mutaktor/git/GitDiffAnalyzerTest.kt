package io.github.ioplane.mutaktor.git

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GitDiffAnalyzerTest {

    /**
     * Tests for the internal [GitDiffAnalyzer.filePathToClassName] conversion logic.
     * These do not require a real git repository.
     */
    @Nested
    inner class FilePathToClassName {

        @Test
        fun `parses simple java file path`(@TempDir tempDir: File) {
            val sourceDir = tempDir.resolve("src/main/java").also { it.mkdirs() }
            // Create the file so canonical paths resolve
            sourceDir.resolve("com/example/Foo.java").also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

            val result = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "src/main/java/com/example/Foo.java",
                sourceDirs = setOf(sourceDir),
            )

            result shouldBe "com.example.Foo*"
        }

        @Test
        fun `parses kotlin file path`(@TempDir tempDir: File) {
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }
            sourceDir.resolve("com/example/Bar.kt").also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

            val result = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "src/main/kotlin/com/example/Bar.kt",
                sourceDirs = setOf(sourceDir),
            )

            result shouldBe "com.example.Bar*"
        }

        @Test
        fun `skips non-source files`(@TempDir tempDir: File) {
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }

            val resultGradle = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "build.gradle.kts",
                sourceDirs = setOf(sourceDir),
            )
            val resultReadme = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "README.md",
                sourceDirs = setOf(sourceDir),
            )

            resultGradle shouldBe null
            resultReadme shouldBe null
        }

        @Test
        fun `skips files outside source dirs`(@TempDir tempDir: File) {
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }
            // File exists but under a different directory
            tempDir.resolve("other/com/example/Baz.kt").also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

            val result = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "other/com/example/Baz.kt",
                sourceDirs = setOf(sourceDir),
            )

            result shouldBe null
        }

        @Test
        fun `handles nested packages`(@TempDir tempDir: File) {
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }
            sourceDir.resolve("com/example/deeply/nested/Class.kt").also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

            val result = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "src/main/kotlin/com/example/deeply/nested/Class.kt",
                sourceDirs = setOf(sourceDir),
            )

            result shouldBe "com.example.deeply.nested.Class*"
        }

        @Test
        fun `handles multiple source dirs and picks the matching one`(@TempDir tempDir: File) {
            val javaSrcDir = tempDir.resolve("src/main/java").also { it.mkdirs() }
            val kotlinSrcDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }
            kotlinSrcDir.resolve("com/example/Service.kt").also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

            val result = GitDiffAnalyzer.filePathToClassName(
                projectDir = tempDir,
                relativePath = "src/main/kotlin/com/example/Service.kt",
                sourceDirs = setOf(javaSrcDir, kotlinSrcDir),
            )

            result shouldBe "com.example.Service*"
        }
    }

    @Nested
    inner class InputValidation {

        @Test
        fun `rejects sinceRef starting with dash`(@TempDir tempDir: File) {
            shouldThrow<IllegalArgumentException> {
                GitDiffAnalyzer.changedClasses(tempDir, "--output=/etc/passwd", setOf(tempDir.resolve("src")))
            }.message shouldContain "must not start with '-'"
        }
    }

    /**
     * Integration tests that create a real temporary git repository
     * to test the full [GitDiffAnalyzer.changedClasses] flow.
     */
    @Nested
    inner class ChangedClassesIntegration {

        @Test
        fun `returns empty for no changes`(@TempDir tempDir: File) {
            initGitRepo(tempDir)
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }

            // Create initial commit with a file
            sourceDir.resolve("com/example/Initial.kt").also {
                it.parentFile.mkdirs()
                it.writeText("class Initial")
            }
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "initial commit")

            // No changes since HEAD — empty result
            val result = GitDiffAnalyzer.changedClasses(
                projectDir = tempDir,
                sinceRef = "HEAD",
                sourceDirs = setOf(sourceDir),
            )

            result.shouldBeEmpty()
        }

        @Test
        fun `detects added kotlin file since branch point`(@TempDir tempDir: File) {
            initGitRepo(tempDir)
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }

            // Initial commit on main
            sourceDir.resolve("com/example/Existing.kt").also {
                it.parentFile.mkdirs()
                it.writeText("class Existing")
            }
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "initial")
            git(tempDir, "branch", "baseline")

            // Add a new file after baseline
            sourceDir.resolve("com/example/NewClass.kt").also {
                it.writeText("class NewClass")
            }
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "add new class")

            val result = GitDiffAnalyzer.changedClasses(
                projectDir = tempDir,
                sinceRef = "baseline",
                sourceDirs = setOf(sourceDir),
            )

            result.shouldContainExactlyInAnyOrder("com.example.NewClass*")
        }

        @Test
        fun `detects modified java file`(@TempDir tempDir: File) {
            initGitRepo(tempDir)
            val sourceDir = tempDir.resolve("src/main/java").also { it.mkdirs() }

            sourceDir.resolve("com/example/Service.java").also {
                it.parentFile.mkdirs()
                it.writeText("class Service {}")
            }
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "initial")
            git(tempDir, "branch", "base-ref")

            // Modify the file
            sourceDir.resolve("com/example/Service.java").writeText("class Service { int x; }")
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "modify service")

            val result = GitDiffAnalyzer.changedClasses(
                projectDir = tempDir,
                sinceRef = "base-ref",
                sourceDirs = setOf(sourceDir),
            )

            result.shouldContainExactlyInAnyOrder("com.example.Service*")
        }

        @Test
        fun `ignores non-source files in diff`(@TempDir tempDir: File) {
            initGitRepo(tempDir)
            val sourceDir = tempDir.resolve("src/main/kotlin").also { it.mkdirs() }

            tempDir.resolve("README.md").writeText("# Hello")
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "initial")
            git(tempDir, "branch", "base")

            // Change only non-source file
            tempDir.resolve("README.md").writeText("# Updated")
            git(tempDir, "add", ".")
            git(tempDir, "commit", "-m", "update readme")

            val result = GitDiffAnalyzer.changedClasses(
                projectDir = tempDir,
                sinceRef = "base",
                sourceDirs = setOf(sourceDir),
            )

            result.shouldBeEmpty()
        }

        private fun initGitRepo(dir: File) {
            git(dir, "init")
            git(dir, "config", "user.email", "test@test.com")
            git(dir, "config", "user.name", "Test")
        }

        private fun git(dir: File, vararg args: String): String {
            val process = ProcessBuilder("git", *args)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            check(exitCode == 0) { "git ${args.toList()} failed: $output" }
            return output
        }
    }
}
