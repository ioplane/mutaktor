package io.github.ioplane.mutaktor

import io.github.ioplane.mutaktor.extreme.ExtremeMutationConfig
import io.github.ioplane.mutaktor.git.GitDiffAnalyzer
import io.github.ioplane.mutaktor.toolchain.GraalVmDetector
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaToolchainService

/**
 * Mutaktor — Kotlin-first Gradle plugin for PIT mutation testing.
 *
 * Applies only when the `java` plugin is present. Creates a `mutaktor` extension
 * for configuration, a `mutaktor` dependency configuration for PIT classpath,
 * and registers the `mutate` task with fully lazy wiring.
 */
public class MutaktorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("mutaktor", MutaktorExtension::class.java)

        project.plugins.withType(JavaPlugin::class.java) {
            val mutaktorConfiguration = createMutaktorConfiguration(project, extension)
            configureTask(project, extension, mutaktorConfiguration)
        }
    }

    private fun createMutaktorConfiguration(
        project: Project,
        extension: MutaktorExtension,
    ) = project.configurations.create(MUTAKTOR_CONFIGURATION_NAME) { config ->
        config.isCanBeConsumed = false
        config.isCanBeResolved = true
        config.description = "Classpath for PIT mutation testing (managed by Mutaktor)"

        config.defaultDependencies { deps ->
            val pitVersion = extension.pitVersion.get()

            deps.add(
                project.dependencies.create(
                    "org.pitest:pitest-command-line:$pitVersion"
                )
            )

            deps.add(
                project.dependencies.create(
                    "org.pitest:pitest-junit5-plugin:${extension.junit5PluginVersion.get()}"
                )
            )

            // Kotlin filters JAR — use project dependency if available (multi-module build),
            // otherwise skip (filters will be published as a separate artifact later)
            if (extension.kotlinFilters.get()) {
                try {
                    deps.add(project.dependencies.create(project.project(":mutaktor-pitest-filter")))
                } catch (_: Exception) {
                    // Not in multi-module build (e.g. funcTest, standalone usage) — skip
                }
            }
        }
    }

    private fun configureTask(
        project: Project,
        extension: MutaktorExtension,
        mutaktorConfiguration: org.gradle.api.artifacts.Configuration,
    ) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.named("main")
        val testSourceSet = sourceSets.named("test")

        // Default targetClasses from project.group if not explicitly set
        extension.targetClasses.convention(
            project.provider {
                val group = project.group.toString()
                if (group.isNotBlank()) setOf("$group.*") else emptySet()
            }
        )

        project.tasks.register(TASK_NAME, MutaktorTask::class.java) { task ->
            task.group = "verification"
            task.description = "Run PIT mutation testing"

            // Core
            task.pitVersion.set(extension.pitVersion)
            // Git-diff scoped analysis: override targetClasses if since is set
            task.targetClasses.set(
                extension.since.flatMap { sinceRef ->
                    project.provider {
                        val srcDirs = task.sourceDirs.files
                        val changed = GitDiffAnalyzer.changedClasses(project.projectDir, sinceRef, srcDirs)
                        if (changed.isEmpty()) {
                            project.logger.lifecycle("Mutaktor: no changed classes since '{}' — using extension targetClasses", sinceRef)
                            extension.targetClasses.get()
                        } else {
                            project.logger.lifecycle("Mutaktor: scoping to {} changed classes since '{}'", changed.size, sinceRef)
                            changed
                        }
                    }
                }.orElse(extension.targetClasses)
            )
            task.targetTests.set(extension.targetTests)
            task.threads.set(extension.threads)
            // Extreme mode: override mutators if enabled
            task.mutators.set(
                extension.extreme.flatMap { isExtreme ->
                    if (isExtreme) {
                        project.provider { ExtremeMutationConfig.EXTREME_MUTATORS }
                    } else {
                        extension.mutators
                    }
                }.orElse(extension.mutators)
            )

            // Filtering
            task.excludedClasses.set(extension.excludedClasses)
            task.excludedMethods.set(extension.excludedMethods)
            task.avoidCallsTo.set(extension.avoidCallsTo)

            // Reporting
            task.reportDir.set(extension.reportDir)
            task.outputFormats.set(extension.outputFormats)
            task.timestampedReports.set(extension.timestampedReports)
            task.jsonReport.set(extension.jsonReport)
            task.sarifReport.set(extension.sarifReport)
            task.mutationScoreThreshold.set(extension.mutationScoreThreshold)

            // Advanced
            task.pitJvmArgs.set(extension.jvmArgs)
            task.verbose.set(extension.verbose)
            task.features.set(extension.features)
            task.pluginConfiguration.set(extension.pluginConfiguration)
            task.useClasspathFile.set(extension.useClasspathFile)

            // Ratchet
            task.ratchetEnabled.set(extension.ratchetEnabled)
            task.ratchetBaseline.set(extension.ratchetBaseline)
            task.ratchetAutoUpdate.set(extension.ratchetAutoUpdate)

            // Incremental
            task.historyInputLocation.set(extension.historyInputLocation)
            task.historyOutputLocation.set(extension.historyOutputLocation)

            // Classpath file location
            task.classpathFile.set(
                project.layout.buildDirectory.file("mutaktor/pitClasspath")
            )

            // Source dirs (Java + Kotlin)
            task.sourceDirs.from(
                mainSourceSet.map { ss ->
                    ss.java.srcDirs + (ss.extensions.findByName("kotlin")
                        ?.let { (it as org.gradle.api.file.SourceDirectorySet).srcDirs }
                        ?: emptySet())
                }
            )

            // Mutable code paths
            task.mutableCodePaths.from(
                mainSourceSet.map { ss -> ss.output.classesDirs }
            )

            // Test classpath
            task.additionalClasspath.from(
                testSourceSet.map { ss -> ss.runtimeClasspath }
            )

            // PIT launch classpath from the mutaktor configuration
            task.launchClasspath.from(mutaktorConfiguration)

            // JDK toolchain for PIT child process
            if (extension.javaLauncher.isPresent) {
                task.javaLauncher.set(extension.javaLauncher)
            }

            // Auto-detect GraalVM + Quarkus → switch PIT to standard JDK
            if (!extension.javaLauncher.isPresent) {
                if (GraalVmDetector.isGraalVm() && GraalVmDetector.hasQuarkus(project)) {
                    val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
                    val standardJdk = GraalVmDetector.resolveStandardJdk(toolchains)
                    if (standardJdk != null) {
                        task.javaLauncher.set(standardJdk)
                        project.logger.lifecycle(
                            "Mutaktor: GraalVM + Quarkus detected. Auto-selected standard JDK for PIT child process."
                        )
                    } else {
                        project.logger.warn(
                            "Mutaktor: GraalVM + Quarkus detected but no standard JDK found.\n" +
                            "PIT may fail with jrt:// classpath errors.\n" +
                            "Fix: Add to settings.gradle.kts:\n" +
                            "  plugins { id(\"org.gradle.toolchains.foojay-resolver-convention\") version \"0.9.0\" }"
                        )
                    }
                }
            }

            // Run after test task
            task.mustRunAfter(project.tasks.named("test"))
        }
    }

    public companion object {
        public const val PLUGIN_ID: String = "io.github.ioplane.mutaktor"
        public const val DEFAULT_PIT_VERSION: String = "1.23.0"
        public const val MUTAKTOR_CONFIGURATION_NAME: String = "mutaktor"
        public const val TASK_NAME: String = "mutate"
    }
}
