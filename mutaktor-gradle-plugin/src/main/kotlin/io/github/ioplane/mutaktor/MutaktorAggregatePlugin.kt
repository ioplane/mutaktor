package io.github.ioplane.mutaktor

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * Aggregator plugin for multi-module mutation reports.
 *
 * Apply to the root project alongside individual `mutaktor` plugins on subprojects:
 * ```kotlin
 * // root build.gradle.kts
 * plugins {
 *     id("io.github.ioplane.mutaktor.aggregate")
 * }
 * ```
 *
 * Registers a `mutateAggregate` task that collects all subproject mutation reports
 * into a single directory.
 */
class MutaktorAggregatePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("mutateAggregate", Copy::class.java) { task ->
            task.group = "verification"
            task.description = "Aggregate mutation reports from all subprojects"
            task.destinationDir = project.layout.buildDirectory.dir("reports/mutaktor-aggregate").get().asFile

            // Collect from all subprojects that have mutaktor plugin
            project.subprojects.forEach { sub ->
                sub.plugins.withId(MutaktorPlugin.PLUGIN_ID) {
                    val subReportDir = sub.layout.buildDirectory.dir("reports/mutaktor")
                    task.from(subReportDir) { spec ->
                        spec.into(sub.name)
                    }
                    task.mustRunAfter(sub.tasks.named(MutaktorPlugin.TASK_NAME))
                }
            }
        }
    }

    companion object {
        const val PLUGIN_ID: String = "io.github.ioplane.mutaktor.aggregate"
        const val TASK_NAME: String = "mutateAggregate"
    }
}
