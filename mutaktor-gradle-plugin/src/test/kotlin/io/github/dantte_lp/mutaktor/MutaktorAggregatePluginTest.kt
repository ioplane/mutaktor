package io.github.dantte_lp.mutaktor

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class MutaktorAggregatePluginTest {

    @Test
    fun `aggregate plugin applies successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(MutaktorAggregatePlugin.PLUGIN_ID)
        project.tasks.findByName("mutateAggregate").shouldNotBeNull()
    }

    @Test
    fun `aggregate task is in verification group`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(MutaktorAggregatePlugin.PLUGIN_ID)
        val task = project.tasks.getByName("mutateAggregate")
        task.group shouldBe "verification"
    }
}
