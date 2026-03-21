package io.github.dantte_lp.mutaktor

import io.github.dantte_lp.mutaktor.ratchet.MutationRatchet
import io.github.dantte_lp.mutaktor.ratchet.RatchetBaseline
import io.github.dantte_lp.mutaktor.report.GithubChecksReporter
import io.github.dantte_lp.mutaktor.report.MutationElementsConverter
import io.github.dantte_lp.mutaktor.report.QualityGate
import io.github.dantte_lp.mutaktor.report.SarifConverter
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.File

/**
 * Main mutation testing task. Launches PIT as a child JVM process via [JavaExec].
 *
 * All properties use the Provider API and are wired from [MutaktorExtension] by
 * [MutaktorPlugin]. The task builds PIT CLI arguments from present properties and
 * delegates execution to `super.exec()`.
 *
 * Example Gradle configuration (done automatically by the plugin):
 * ```kotlin
 * tasks.named<MutaktorTask>("mutate") {
 *     targetClasses.add("com.example.*")
 *     threads.set(4)
 * }
 * ```
 */
@CacheableTask
public abstract class MutaktorTask : JavaExec() {

    // ── Internal (not part of cache key) ──────────────────────────────────────

    /** PIT version resolved by the plugin. Used for logging, not cache-relevant. */
    @get:Internal
    public abstract val pitVersion: Property<String>

    // ── Inputs ────────────────────────────────────────────────────────────────

    /** Target classes to mutate (glob patterns, e.g. `com.example.*`). Required. */
    @get:Input
    public abstract val targetClasses: SetProperty<String>

    /** Target test classes (glob patterns). Defaults to PIT auto-detection when absent. */
    @get:Input
    @get:Optional
    public abstract val targetTests: SetProperty<String>

    /** Number of parallel threads for mutation analysis. */
    @get:Input
    @get:Optional
    public abstract val threads: Property<Int>

    /** Mutation operators to apply (e.g. `STRONGER`, `ALL`). */
    @get:Input
    @get:Optional
    public abstract val mutators: SetProperty<String>

    /** Output formats (e.g. `HTML`, `XML`, `CSV`). */
    @get:Input
    @get:Optional
    public abstract val outputFormats: SetProperty<String>

    /** Whether PIT should create timestamped report sub-directories. */
    @get:Input
    @get:Optional
    public abstract val timestampedReports: Property<Boolean>

    /** Class glob patterns to exclude from mutation. */
    @get:Input
    @get:Optional
    public abstract val excludedClasses: SetProperty<String>

    /** Method name patterns to exclude from mutation. */
    @get:Input
    @get:Optional
    public abstract val excludedMethods: SetProperty<String>

    /** Packages whose calls should be replaced with NO-OPs (e.g. logging). */
    @get:Input
    @get:Optional
    public abstract val avoidCallsTo: SetProperty<String>

    /** Extra JVM arguments passed to the child PIT process. */
    @get:Input
    @get:Optional
    public abstract val pitJvmArgs: ListProperty<String>

    /** Whether to log verbose PIT output. */
    @get:Input
    @get:Optional
    public abstract val verbose: Property<Boolean>

    /** PIT feature flags (e.g. `+auto_threads`, `-fhistory`). */
    @get:Input
    @get:Optional
    public abstract val features: ListProperty<String>

    /** Arbitrary key-value configuration passed to PIT plugins. */
    @get:Input
    @get:Optional
    public abstract val pluginConfiguration: MapProperty<String, String>

    /** When true, writes the classpath to a file instead of passing it inline. */
    @get:Input
    @get:Optional
    public abstract val useClasspathFile: Property<Boolean>

    // ── Post-processing inputs ────────────────────────────────────────────────

    /** When true, produces a mutation-testing-elements JSON report. */
    @get:Input
    @get:Optional
    public abstract val jsonReport: Property<Boolean>

    /** When true, produces a SARIF 2.1.0 report. */
    @get:Input
    @get:Optional
    public abstract val sarifReport: Property<Boolean>

    /** Minimum required mutation score (0-100). Build fails if score is below this threshold. */
    @get:Input
    @get:Optional
    public abstract val mutationScoreThreshold: Property<Int>

    /** Enable per-package mutation score ratchet. */
    @get:Input
    @get:Optional
    public abstract val ratchetEnabled: Property<Boolean>

    /** Baseline file for ratchet comparison. */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    @get:Optional
    public abstract val ratchetBaseline: RegularFileProperty

    /** Auto-update baseline when scores improve. */
    @get:Input
    @get:Optional
    public abstract val ratchetAutoUpdate: Property<Boolean>

    // ── File inputs ───────────────────────────────────────────────────────────

    /** Project source directories (for source-level reporting). */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    public abstract val sourceDirs: ConfigurableFileCollection

    /** Classes + test classes + dependency JARs for the child PIT process. */
    @get:Classpath
    public abstract val launchClasspath: ConfigurableFileCollection

    /** Additional classpath entries (runtime deps, annotation processors, etc.). */
    @get:Classpath
    public abstract val additionalClasspath: ConfigurableFileCollection

    /** Directories containing compiled mutable code (e.g. `build/classes/kotlin/main`). */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    public abstract val mutableCodePaths: ConfigurableFileCollection

    /** History file to read previous results from (incremental analysis). */
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    @get:Optional
    public abstract val historyInputLocation: RegularFileProperty

    // ── Outputs ───────────────────────────────────────────────────────────────

    /** Directory where PIT writes its reports. */
    @get:OutputDirectory
    public abstract val reportDir: DirectoryProperty

    /** File that receives the flattened classpath when [useClasspathFile] is true. */
    @get:OutputFile
    @get:Optional
    public abstract val classpathFile: RegularFileProperty

    /** History file where PIT writes results for future incremental runs. */
    @get:OutputFile
    @get:Optional
    public abstract val historyOutputLocation: RegularFileProperty

    // ── Execution ─────────────────────────────────────────────────────────────

    override fun exec() {
        if (!targetClasses.isPresent || targetClasses.get().isEmpty()) {
            throw GradleException("Mutaktor: targetClasses is empty. Set mutaktor.targetClasses or project.group.")
        }

        mainClass.set(PIT_MAIN_CLASS)
        classpath = launchClasspath

        if (useClasspathFile.getOrElse(false)) {
            writeClasspathFile()
        }

        args(buildPitArguments())

        logger.lifecycle(
            "Mutaktor: launching PIT {} with {} argument(s)",
            pitVersion.getOrElse("(unknown)"),
            args?.size ?: 0,
        )

        super.exec()

        postProcess()
    }

    // ── Post-processing ────────────────────────────────────────────────────────

    /**
     * Runs post-processing steps after PIT completes:
     * JSON report, SARIF report, quality gate, ratchet, GitHub Checks.
     *
     * Gracefully handles missing `mutations.xml` (PIT might fail or produce no mutations).
     */
    private fun postProcess() {
        val reportDirectory = reportDir.get().asFile
        val mutationsXml = reportDirectory.resolve("mutations.xml")

        if (!mutationsXml.exists()) {
            logger.warn("Mutaktor: mutations.xml not found in {} — skipping post-processing", reportDirectory)
            return
        }

        // 1. JSON report (mutation-testing-elements)
        if (jsonReport.getOrElse(false)) {
            val sourceRoot = sourceDirs.files.firstOrNull() ?: reportDirectory
            val json = MutationElementsConverter.convert(mutationsXml, sourceRoot)
            val jsonFile = reportDirectory.resolve("mutations.json")
            jsonFile.writeText(json)
            logger.lifecycle("Mutaktor: wrote mutation-testing-elements JSON to {}", jsonFile)
        }

        // 2. SARIF report
        if (sarifReport.getOrElse(false)) {
            val sarif = SarifConverter.convert(mutationsXml, pitVersion.getOrElse("unknown"))
            val sarifFile = reportDirectory.resolve("mutations.sarif.json")
            sarifFile.writeText(sarif)
            logger.lifecycle("Mutaktor: wrote SARIF report to {}", sarifFile)
        }

        // 3. Quality gate
        if (mutationScoreThreshold.isPresent) {
            val threshold = mutationScoreThreshold.get()
            val result = QualityGate.evaluate(mutationsXml, threshold)
            logger.lifecycle(
                "Mutaktor: mutation score {} % ({}/{} killed) — threshold {} %",
                result.mutationScore, result.killedMutations, result.totalMutations, threshold,
            )
            if (!result.passed) {
                throw GradleException(
                    "Mutaktor: quality gate FAILED — mutation score ${result.mutationScore}% is below threshold ${threshold}%"
                )
            }
        }

        // 4. Ratchet
        if (ratchetEnabled.getOrElse(false)) {
            val baselineFile = ratchetBaseline.asFile.orNull
            val baseline = if (baselineFile != null) RatchetBaseline.load(baselineFile) else emptyMap()
            val currentScores = MutationRatchet.computeScores(mutationsXml)
            val ratchetResult = MutationRatchet.evaluate(currentScores, baseline)

            if (ratchetResult.improvements.isNotEmpty() || ratchetResult.newPackages.isNotEmpty()) {
                logger.lifecycle("Mutaktor: ratchet — {} improvement(s), {} new package(s)",
                    ratchetResult.improvements.size, ratchetResult.newPackages.size)
            }

            if (ratchetAutoUpdate.getOrElse(true) && baselineFile != null) {
                RatchetBaseline.save(baselineFile, currentScores)
                logger.lifecycle("Mutaktor: updated ratchet baseline {}", baselineFile)
            }

            if (!ratchetResult.passed) {
                val regressionDetails = ratchetResult.regressions.joinToString("\n") {
                    "  ${it.packageName}: ${it.previousScore}% → ${it.currentScore}%"
                }
                throw GradleException(
                    "Mutaktor: ratchet FAILED — mutation score regression detected:\n$regressionDetails"
                )
            }
        }

        // 5. GitHub Checks
        val githubToken = System.getenv("GITHUB_TOKEN")
        val githubRepository = System.getenv("GITHUB_REPOSITORY")
        val githubSha = System.getenv("GITHUB_SHA")

        if (!githubToken.isNullOrBlank() && !githubRepository.isNullOrBlank() && !githubSha.isNullOrBlank()) {
            val threshold = mutationScoreThreshold.getOrElse(0)
            val gateResult = QualityGate.evaluate(mutationsXml, threshold)
            val survived = QualityGate.survivedMutants(mutationsXml)

            GithubChecksReporter.report(
                token = githubToken,
                repository = githubRepository,
                sha = githubSha,
                mutants = survived,
                mutationScore = gateResult.mutationScore,
                threshold = threshold,
            )
            logger.lifecycle("Mutaktor: posted GitHub Check Run with {} annotation(s)", survived.size)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the PIT CLI argument list from all configured properties.
     *
     * Format: `--argName=value` for scalars, `--argName=a,b,c` for collections.
     * Only present (non-empty) properties produce arguments.
     */
    internal fun buildPitArguments(): List<String> = buildList {

        // Required
        addSetArg("targetClasses", targetClasses)

        // Optional collections
        addSetArg("targetTests", targetTests)
        addSetArg("mutators", mutators)
        addSetArg("outputFormats", outputFormats)
        addSetArg("excludedClasses", excludedClasses)
        addSetArg("excludedMethods", excludedMethods)
        addSetArg("avoidCallsTo", avoidCallsTo)

        // Optional scalars
        addPropertyArg("threads", threads)
        addPropertyArg("timestampedReports", timestampedReports)
        addPropertyArg("verbose", verbose)

        // Report directory (always present after plugin wiring)
        if (reportDir.isPresent) {
            add("--reportDir=${reportDir.get().asFile.absolutePath}")
        }

        // Source directories
        if (!sourceDirs.isEmpty) {
            add("--sourceDirs=${sourceDirs.files.joinToString(",") { it.absolutePath }}")
        }

        // Classpath handling
        if (useClasspathFile.getOrElse(false) && classpathFile.isPresent) {
            add("--classPathFile=${classpathFile.get().asFile.absolutePath}")
        } else {
            val cpFiles = (additionalClasspath.files + mutableCodePaths.files)
            if (cpFiles.isNotEmpty()) {
                add("--classPath=${cpFiles.joinToString(",") { it.absolutePath }}")
            }
        }

        // Mutable code paths
        if (!mutableCodePaths.isEmpty) {
            add("--mutableCodePaths=${mutableCodePaths.files.joinToString(",") { it.absolutePath }}")
        }

        // Child JVM args
        if (pitJvmArgs.isPresent && pitJvmArgs.get().isNotEmpty()) {
            add("--jvmArgs=${pitJvmArgs.get().joinToString(",")}")
        }

        // Features
        if (features.isPresent && features.get().isNotEmpty()) {
            add("--features=${features.get().joinToString(",")}")
        }

        // Plugin configuration (key=value pairs, comma-separated)
        if (pluginConfiguration.isPresent && pluginConfiguration.get().isNotEmpty()) {
            add(
                "--pluginConfiguration=${
                    pluginConfiguration.get().entries.joinToString(",") { "${it.key}=${it.value}" }
                }",
            )
        }

        // History
        if (historyInputLocation.isPresent) {
            add("--historyInputLocation=${historyInputLocation.get().asFile.absolutePath}")
        }
        if (historyOutputLocation.isPresent) {
            add("--historyOutputLocation=${historyOutputLocation.get().asFile.absolutePath}")
        }
    }

    /**
     * Writes [additionalClasspath] and [mutableCodePaths] entries to [classpathFile],
     * one path per line, so PIT can read them via `--classPathFile`.
     */
    private fun writeClasspathFile() {
        val target: File = classpathFile.get().asFile
        target.parentFile.mkdirs()
        val entries = (additionalClasspath.files + mutableCodePaths.files)
            .map { it.absolutePath }
        target.writeText(entries.joinToString(System.lineSeparator()))
        logger.info("Mutaktor: wrote {} classpath entries to {}", entries.size, target)
    }

    private companion object {
        /** PIT CLI entry point class. */
        const val PIT_MAIN_CLASS: String =
            "org.pitest.mutationtest.commandline.MutationCoverageReport"
    }
}

/** Adds `--[name]=[joined values]` if the set property is present and non-empty. */
private fun MutableList<String>.addSetArg(name: String, prop: SetProperty<String>) {
    if (prop.isPresent && prop.get().isNotEmpty()) {
        add("--$name=${prop.get().joinToString(",")}")
    }
}

/** Adds `--[name]=[value]` if the property is present. Works for any type. */
private fun MutableList<String>.addPropertyArg(name: String, prop: Property<*>) {
    if (prop.isPresent) {
        add("--$name=${prop.get()}")
    }
}
