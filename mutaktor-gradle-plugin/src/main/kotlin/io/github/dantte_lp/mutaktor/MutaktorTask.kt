package io.github.dantte_lp.mutaktor

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Main mutation testing task. Runs PIT via JavaExec in a child process.
 *
 * TODO: Sprint 2 — implement full PIT execution with Provider API wiring.
 */
public abstract class MutaktorTask : DefaultTask() {

    @TaskAction
    public fun mutate() {
        logger.lifecycle("Mutaktor: mutation testing not yet implemented (Sprint 2)")
    }
}
