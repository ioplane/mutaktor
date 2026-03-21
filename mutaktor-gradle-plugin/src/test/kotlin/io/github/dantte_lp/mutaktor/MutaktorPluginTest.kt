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
    fun `plugin does not register task without java plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        project.tasks.findByName("mutate") shouldBe null
    }

    @Test
    fun `extension has correct default pit version`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.pitVersion.get() shouldBe MutaktorPlugin.DEFAULT_PIT_VERSION
    }

    @Test
    fun `extension has correct default mutators`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.mutators.get() shouldBe setOf("DEFAULTS")
    }

    @Test
    fun `extension has correct default output formats`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.outputFormats.get() shouldBe setOf("HTML", "XML")
    }

    @Test
    fun `extension has correct default thread count`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.threads.get() shouldBe Runtime.getRuntime().availableProcessors()
    }

    @Test
    fun `mutate task is registered in verification group`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val task = project.tasks.findByName("mutate")
        task.shouldNotBeNull()
        task.group shouldBe "verification"
        task.description.shouldNotBeNull()
    }

    @Test
    fun `mutaktor configuration is created`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        project.configurations.findByName("mutaktor").shouldNotBeNull()
    }

    @Test
    fun `mutaktor configuration exists and is resolvable`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val config = project.configurations.getByName("mutaktor")
        config.isCanBeResolved shouldBe true
        config.isCanBeConsumed shouldBe false
    }

    @Test
    fun `targetClasses defaults to project group`() {
        val project = ProjectBuilder.builder().build()
        project.group = "com.example"
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.targetClasses.get() shouldBe setOf("com.example.*")
    }

    @Test
    fun `extreme defaults to false`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.extreme.get() shouldBe false
    }

    @Test
    fun `javaLauncher is not set by default`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)
        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.javaLauncher.isPresent shouldBe false
    }

    @Test
    fun `kotlinFilters defaults to true`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.kotlinFilters.get() shouldBe true
    }

    @Test
    fun `jsonReport defaults to true`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.jsonReport.get() shouldBe true
    }

    @Test
    fun `sarifReport defaults to false`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply(MutaktorPlugin.PLUGIN_ID)

        val ext = project.extensions.getByType(MutaktorExtension::class.java)
        ext.sarifReport.get() shouldBe false
    }
}
