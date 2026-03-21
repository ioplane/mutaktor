package io.github.dantte_lp.mutaktor.git

import java.io.File

/**
 * Analyzes `git diff` to determine which classes changed since a given ref.
 * Used by MutaktorPlugin to scope targetClasses when `since` is set.
 */
public object GitDiffAnalyzer {

    private val SOURCE_EXTENSIONS = setOf("kt", "java")

    /**
     * Returns set of glob patterns (e.g. "com.example.Foo*") for classes
     * whose source files changed between [sinceRef] and HEAD.
     *
     * @param projectDir the project root directory
     * @param sinceRef git ref to diff against (branch, tag, SHA)
     * @param sourceDirs source directories to map file paths to class names
     * @return set of fully-qualified class name patterns suitable for PIT targetClasses
     */
    public fun changedClasses(
        projectDir: File,
        sinceRef: String,
        sourceDirs: Set<File>,
    ): Set<String> {
        require(!sinceRef.startsWith("-")) { "sinceRef must not start with '-': $sinceRef" }
        val diffOutput = runGitDiff(projectDir, sinceRef)
        if (diffOutput.isBlank()) return emptySet()

        val changedFiles = diffOutput.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return changedFiles.mapNotNull { relativePath ->
            filePathToClassName(projectDir, relativePath, sourceDirs)
        }.toSet()
    }

    /**
     * Converts a file path (relative to project dir) into a fully-qualified
     * class name pattern, if the file is a source file under one of the
     * given source directories.
     *
     * @return FQN with trailing `*` wildcard for inner classes, or null if not a source file
     */
    internal fun filePathToClassName(
        projectDir: File,
        relativePath: String,
        sourceDirs: Set<File>,
    ): String? {
        val extension = relativePath.substringAfterLast('.', "")
        if (extension !in SOURCE_EXTENSIONS) return null

        val absoluteFile = projectDir.resolve(relativePath).canonicalFile

        for (sourceDir in sourceDirs) {
            val canonicalSourceDir = sourceDir.canonicalFile
            if (absoluteFile.startsWith(canonicalSourceDir)) {
                val relativeToSource = absoluteFile.relativeTo(canonicalSourceDir).path
                val withoutExtension = relativeToSource.substringBeforeLast('.')
                val className = withoutExtension.replace(File.separatorChar, '.')
                return "$className*"
            }
        }

        // File is not under any known source directory — skip
        return null
    }

    private fun runGitDiff(projectDir: File, sinceRef: String): String {
        val process = ProcessBuilder(
            "git", "diff", "--name-only", "--diff-filter=ACMR", "$sinceRef..HEAD"
        )
            .directory(projectDir)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException(
                "git diff failed (exit code $exitCode) in ${projectDir.absolutePath}: ${stderr.trim()}"
            )
        }

        return stdout
    }
}
