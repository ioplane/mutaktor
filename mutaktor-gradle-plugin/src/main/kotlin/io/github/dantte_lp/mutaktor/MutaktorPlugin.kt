package io.github.dantte_lp.mutaktor

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Mutaktor — Kotlin-first Gradle plugin for PIT mutation testing.
 */
public class MutaktorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("mutaktor", MutaktorExtension::class.java)

        project.plugins.withId("java") {
            project.tasks.register("mutate", MutaktorTask::class.java) { task ->
                task.group = "verification"
                task.description = "Run PIT mutation testing"
            }
        }
    }

    public companion object {
        public const val PLUGIN_ID: String = "io.github.dantte-lp.mutaktor"
        public const val DEFAULT_PIT_VERSION: String = "1.23.0"
    }
}
