package io.github.ioplane.mutaktor

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.jvm.toolchain.JavaLauncher
import java.math.BigDecimal
import javax.inject.Inject


/**
 * Type-safe DSL extension for Mutaktor — Kotlin-first PIT mutation testing.
 *
 * All properties use the Gradle Provider API for lazy evaluation and
 * configuration-cache compatibility. Sensible conventions are applied
 * so that a minimal configuration is sufficient for most projects.
 *
 * ```kotlin
 * mutaktor {
 *     pitVersion = "1.23.0"
 *     targetClasses = setOf("com.example.*")
 *     threads = Runtime.getRuntime().availableProcessors()
 *     since = "main"              // git-diff scoped analysis
 *     kotlinFilters = true        // filter Kotlin-generated junk mutations
 *     outputFormats = setOf("HTML", "XML")
 * }
 * ```
 */
public abstract class MutaktorExtension @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout,
) {

    // ──────────────────────────────────────────────────────────────
    //  Core
    // ──────────────────────────────────────────────────────────────

    /** PIT version to resolve from Maven Central. */
    public abstract val pitVersion: Property<String>

    /** Glob patterns selecting classes to mutate (e.g. `com.example.*`). */
    public abstract val targetClasses: SetProperty<String>

    /** Glob patterns selecting test classes to run. Defaults to same as [targetClasses]. */
    public abstract val targetTests: SetProperty<String>

    /** Number of parallel threads for mutation analysis. */
    public abstract val threads: Property<Int>

    /**
     * Mutator group or individual mutator names.
     *
     * Common groups: `DEFAULTS`, `STRONGER`, `ALL`.
     * Individual mutators can be mixed in (e.g. `DEFAULTS`, `UOI`).
     */
    public abstract val mutators: SetProperty<String>

    /**
     * Factor to apply to the normal test execution time when calculating the
     * timeout for each mutant. A mutant is killed if the test exceeds
     * `timeoutFactor * normalTime + timeoutConstant`.
     */
    public abstract val timeoutFactor: Property<BigDecimal>

    /**
     * Constant (in milliseconds) added to the calculated timeout for each mutant.
     *
     * @see timeoutFactor
     */
    public abstract val timeoutConstant: Property<Int>

    // ──────────────────────────────────────────────────────────────
    //  Filtering
    // ──────────────────────────────────────────────────────────────

    /** Glob patterns for classes to exclude from mutation. */
    public abstract val excludedClasses: SetProperty<String>

    /**
     * Method-name patterns to exclude from mutation.
     * Supports simple wildcards (e.g. `hashCode`, `toString`, `equals`).
     */
    public abstract val excludedMethods: SetProperty<String>

    /** Glob patterns for test classes to exclude from running. */
    public abstract val excludedTestClasses: SetProperty<String>

    /**
     * Fully-qualified prefixes of packages whose methods should not be
     * instrumented for coverage (e.g. `kotlin.jvm.internal`, `org.slf4j`).
     */
    public abstract val avoidCallsTo: SetProperty<String>

    // ──────────────────────────────────────────────────────────────
    //  Reporting
    // ──────────────────────────────────────────────────────────────

    /** Directory where PIT writes its reports. */
    public abstract val reportDir: DirectoryProperty

    /**
     * Output formats to generate.
     * Supported values: `HTML`, `XML`, `CSV`, `TIMESTAMP`.
     */
    public abstract val outputFormats: SetProperty<String>

    /** When `false`, reports overwrite previous results instead of creating timestamped sub-dirs. */
    public abstract val timestampedReports: Property<Boolean>

    /** When `true`, produces a mutation-testing-elements JSON report (`mutations.json`). */
    public abstract val jsonReport: Property<Boolean>

    /** When `true`, produces a SARIF 2.1.0 report (`mutations.sarif.json`). */
    public abstract val sarifReport: Property<Boolean>

    /** Minimum required mutation score (0-100). Build fails if score is below this threshold. Unset means no gate. */
    public abstract val mutationScoreThreshold: Property<Int>

    // ──────────────────────────────────────────────────────────────
    //  Test configuration
    // ──────────────────────────────────────────────────────────────

    /** Version of the `pitest-junit5-plugin` dependency to resolve. */
    public abstract val junit5PluginVersion: Property<String>

    /** JUnit 5 tag expressions selecting tests to include. */
    public abstract val includedGroups: SetProperty<String>

    /** JUnit 5 tag expressions selecting tests to exclude. */
    public abstract val excludedGroups: SetProperty<String>

    /** When `true`, every mutant is tested against every test (no early exit). */
    public abstract val fullMutationMatrix: Property<Boolean>

    // ──────────────────────────────────────────────────────────────
    //  Advanced / JVM
    // ──────────────────────────────────────────────────────────────

    /**
     * Extra JVM arguments passed to the **forked** test processes
     * (e.g. `--add-opens`, `-Xmx`).
     */
    public abstract val jvmArgs: ListProperty<String>

    /**
     * Extra JVM arguments passed to the **main** PIT analysis process
     * (not the forked workers).
     */
    public abstract val mainProcessJvmArgs: ListProperty<String>

    /**
     * Key-value pairs forwarded to PIT plugins via `--pluginConfiguration`.
     * Keys follow the pattern `pluginName.key` (e.g. `ARCMUTATE_ENGINE.limit=100`).
     */
    public abstract val pluginConfiguration: MapProperty<String, String>

    /**
     * PIT feature flags to enable or disable (prefix with `+` or `-`).
     * Example: `+auto_threads`, `-FLOGIC`.
     */
    public abstract val features: ListProperty<String>

    /** Enable verbose PIT console output. Useful for debugging. */
    public abstract val verbose: Property<Boolean>

    // ── JDK Toolchain for PIT child process ──────────────────────────────
    /**
     * Java launcher for the PIT child process (minion JVM).
     *
     * Use this to run PIT on a different JDK than the build JDK.
     * Required when building with GraalVM (jrt:// module paths break PIT).
     *
     * Example:
     * ```kotlin
     * mutaktor {
     *     javaLauncher.set(javaToolchains.launcherFor {
     *         languageVersion.set(JavaLanguageVersion.of(25))
     *         vendor.set(JvmVendorSpec.AZUL)
     *     })
     * }
     * ```
     */
    public abstract val javaLauncher: Property<JavaLauncher>

    // ──────────────────────────────────────────────────────────────
    //  Git-aware analysis (mutaktor-specific)
    // ──────────────────────────────────────────────────────────────

    /**
     * Git ref to diff against (e.g. `main`, `HEAD~5`, a commit SHA).
     *
     * When set, only classes that changed since this ref are mutated,
     * dramatically reducing analysis time on large code-bases.
     */
    public abstract val since: Property<String>

    // ──────────────────────────────────────────────────────────────
    //  Kotlin filter (mutaktor-specific)
    // ──────────────────────────────────────────────────────────────

    /**
     * Enable the built-in Kotlin junk-mutation filters.
     *
     * These suppress mutations in compiler-generated code such as
     * `DefaultImpls`, data-class `copy`/`componentN`, null-check intrinsics, etc.
     */
    public abstract val kotlinFilters: Property<Boolean>

    // ── Extreme mutation (mutaktor-specific) ──────────────────────────────

    /**
     * Enable extreme mutation testing mode.
     *
     * Replaces fine-grained mutators with method-body removal mutators.
     * Generates far fewer mutants (~1 per method vs ~10 per method),
     * making mutation testing practical for large codebases.
     * Effectively detects pseudo-tested methods.
     */
    public abstract val extreme: Property<Boolean>

    // ──────────────────────────────────────────────────────────────
    //  Incremental analysis
    // ──────────────────────────────────────────────────────────────

    /**
     * File to read previous mutation analysis state from, enabling
     * incremental analysis across builds.
     */
    public abstract val historyInputLocation: RegularFileProperty

    /**
     * File to write mutation analysis state to after a run, so that
     * subsequent builds can use it as [historyInputLocation].
     */
    public abstract val historyOutputLocation: RegularFileProperty

    /**
     * When `true`, PIT writes classpath entries to a temporary file
     * rather than passing them on the command line, avoiding
     * OS-level argument-length limits.
     */
    public abstract val useClasspathFile: Property<Boolean>

    // ── Ratchet (mutation score floor) ────────────────────────────────

    /** Enable per-package mutation score ratchet. Fails build if any package score drops. */
    public abstract val ratchetEnabled: Property<Boolean>

    /** Baseline file for ratchet comparison. Default: .mutaktor-baseline.json */
    public abstract val ratchetBaseline: RegularFileProperty

    /** Auto-update baseline when scores improve. */
    public abstract val ratchetAutoUpdate: Property<Boolean>

    // ──────────────────────────────────────────────────────────────
    //  Conventions
    // ──────────────────────────────────────────────────────────────

    init {
        // Core
        pitVersion.convention(MutaktorPlugin.DEFAULT_PIT_VERSION)
        threads.convention(Runtime.getRuntime().availableProcessors())
        mutators.convention(setOf("DEFAULTS"))
        timeoutFactor.convention(BigDecimal("1.25"))
        timeoutConstant.convention(4000)

        // Reporting
        reportDir.convention(layout.buildDirectory.dir("reports/mutaktor"))
        outputFormats.convention(setOf("HTML", "XML"))
        timestampedReports.convention(false)
        jsonReport.convention(true)
        sarifReport.convention(false)

        // Test
        junit5PluginVersion.convention("1.2.3")
        fullMutationMatrix.convention(false)

        // Advanced
        verbose.convention(false)

        // Kotlin filter
        kotlinFilters.convention(true)

        // Extreme mutation
        extreme.convention(false)

        // Incremental
        useClasspathFile.convention(true)

        // Ratchet
        ratchetEnabled.convention(false)
        ratchetBaseline.convention(layout.projectDirectory.file(".mutaktor-baseline.json"))
        ratchetAutoUpdate.convention(true)
    }
}
