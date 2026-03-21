package io.github.ioplane.mutaktor

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MutaktorTaskArgumentsTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var task: MutaktorTask

    @BeforeEach
    fun setUp() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        project.plugins.apply("java")
        task = project.tasks.register("testMutate", MutaktorTask::class.java).get()
    }

    // ── 1. targetClasses ────────────────────────────────────────

    @Test
    fun `targetClasses produces correct argument`() {
        task.targetClasses.add("com.example.*")

        val args = task.buildPitArguments()

        args shouldContain "--targetClasses=com.example.*"
    }

    // ── 2. multiple targetClasses ───────────────────────────────

    @Test
    fun `multiple targetClasses joined by comma`() {
        task.targetClasses.addAll("com.example.*", "com.other.*")

        val args = task.buildPitArguments()

        // Order may vary for sets, so check both patterns exist
        val targetArg = args.first { it.startsWith("--targetClasses=") }
        val values = targetArg.removePrefix("--targetClasses=").split(",").toSet()
        values shouldBe setOf("com.example.*", "com.other.*")
    }

    // ── 3. threads ──────────────────────────────────────────────

    @Test
    fun `threads produces correct argument`() {
        task.threads.set(4)

        val args = task.buildPitArguments()

        args shouldContain "--threads=4"
    }

    // ── 4. outputFormats ────────────────────────────────────────

    @Test
    fun `outputFormats produces correct argument`() {
        task.outputFormats.addAll("HTML", "XML")

        val args = task.buildPitArguments()

        val formatArg = args.first { it.startsWith("--outputFormats=") }
        val values = formatArg.removePrefix("--outputFormats=").split(",").toSet()
        values shouldBe setOf("HTML", "XML")
    }

    // ── 5. reportDir ────────────────────────────────────────────

    @Test
    fun `reportDir produces correct argument`() {
        val reportDir = tempDir.resolve("reports").toFile()
        reportDir.mkdirs()
        task.reportDir.set(reportDir)

        val args = task.buildPitArguments()

        args shouldContain "--reportDir=${reportDir.absolutePath}"
    }

    // ── 6. verbose ──────────────────────────────────────────────

    @Test
    fun `verbose true produces correct argument`() {
        task.verbose.set(true)

        val args = task.buildPitArguments()

        args shouldContain "--verbose=true"
    }

    // ── 7. empty optional properties produce no arguments ───────

    @Test
    fun `empty optional properties produce no arguments`() {
        // Only targetClasses set, everything else empty
        task.targetClasses.add("com.example.*")

        val args = task.buildPitArguments()

        args.none { it.startsWith("--threads=") } shouldBe true
        args.none { it.startsWith("--verbose=") } shouldBe true
        args.none { it.startsWith("--features=") } shouldBe true
        args.none { it.startsWith("--pluginConfiguration=") } shouldBe true
        args.none { it.startsWith("--targetTests=") } shouldBe true
        args.none { it.startsWith("--mutators=") } shouldBe true
        args.none { it.startsWith("--excludedClasses=") } shouldBe true
        args.none { it.startsWith("--excludedMethods=") } shouldBe true
        args.none { it.startsWith("--avoidCallsTo=") } shouldBe true
        args.none { it.startsWith("--jvmArgs=") } shouldBe true
    }

    // ── 8. features ─────────────────────────────────────────────

    @Test
    fun `features list produces correct argument`() {
        task.features.addAll("+auto_threads", "-FLOGIC")

        val args = task.buildPitArguments()

        val featuresArg = args.first { it.startsWith("--features=") }
        val values = featuresArg.removePrefix("--features=").split(",").toSet()
        values shouldBe setOf("+auto_threads", "-FLOGIC")
    }

    // ── 9. pluginConfiguration ──────────────────────────────────

    @Test
    fun `pluginConfiguration map produces correct argument`() {
        task.pluginConfiguration.put("key1", "val1")
        task.pluginConfiguration.put("key2", "val2")

        val args = task.buildPitArguments()

        val configArg = args.first { it.startsWith("--pluginConfiguration=") }
        val pairs = configArg.removePrefix("--pluginConfiguration=").split(",").toSet()
        pairs shouldContainAll setOf("key1=val1", "key2=val2")
    }

    // ── 10. classpath file mode ─────────────────────────────────

    @Test
    fun `classpath file mode produces classPathFile argument`() {
        val cpFile = tempDir.resolve("cp.txt").toFile()
        task.useClasspathFile.set(true)
        task.classpathFile.set(cpFile)

        val args = task.buildPitArguments()

        args shouldContain "--classPathFile=${cpFile.absolutePath}"
    }

    // ── Additional edge cases ───────────────────────────────────

    @Test
    fun `timestampedReports produces correct argument`() {
        task.timestampedReports.set(false)

        val args = task.buildPitArguments()

        args shouldContain "--timestampedReports=false"
    }

    @Test
    fun `pitJvmArgs produces jvmArgs argument`() {
        task.pitJvmArgs.addAll("-Xmx512m", "-XX:+UseG1GC")

        val args = task.buildPitArguments()

        val jvmArg = args.first { it.startsWith("--jvmArgs=") }
        jvmArg shouldBe "--jvmArgs=-Xmx512m,-XX:+UseG1GC"
    }
}
