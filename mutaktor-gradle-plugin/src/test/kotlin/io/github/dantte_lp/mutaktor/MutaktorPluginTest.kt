package io.github.dantte_lp.mutaktor

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class MutaktorPluginTest {

    @Test
    fun `plugin applies successfully to a java project`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        project.extensions.findByType(MutaktorExtension::class.java).shouldNotBeNull()
    }

    @Test
    fun `extension has correct default pit version`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.pitVersion.get() shouldBe "1.23.0"
    }

    @Test
    fun `mutate task is registered when java plugin applied`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        project.tasks.findByName("mutate").shouldNotBeNull()
        project.tasks.getByName("mutate").group shouldBe "verification"
    }
}
