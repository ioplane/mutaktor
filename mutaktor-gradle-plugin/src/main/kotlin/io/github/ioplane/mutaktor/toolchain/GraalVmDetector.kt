package io.github.ioplane.mutaktor.toolchain

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.*

/**
 * Detects GraalVM + Quarkus combination and auto-resolves a standard JDK
 * for PIT child process to avoid jrt:// classpath issues.
 */
object GraalVmDetector {

    /**
     * Returns true if the current build JDK is GraalVM.
     */
    fun isGraalVm(): Boolean {
        val vmName = System.getProperty("java.vm.name") ?: ""
        val javaHome = System.getProperty("java.home") ?: ""
        return vmName.contains("GraalVM", ignoreCase = true) ||
               javaHome.contains("graalvm", ignoreCase = true) ||
               javaHome.contains("graal", ignoreCase = true)
    }

    /**
     * Returns true if the project has Quarkus on its classpath.
     */
    fun hasQuarkus(project: Project): Boolean {
        return project.configurations.any { config ->
            config.dependencies.any { dep ->
                dep.group?.contains("quarkus", ignoreCase = true) == true
            }
        }
    }

    /**
     * Try to resolve a non-GraalVM JDK of the same major version.
     * Tries vendors in order: AZUL, ADOPTIUM, AMAZON, then any.
     * Returns null if no alternative JDK can be found.
     */
    fun resolveStandardJdk(
        toolchains: JavaToolchainService,
        javaVersion: Int = Runtime.version().feature(),
    ): Provider<JavaLauncher>? {
        val vendors = listOf(
            JvmVendorSpec.AZUL,
            JvmVendorSpec.ADOPTIUM,
            JvmVendorSpec.AMAZON,
        )
        for (vendor in vendors) {
            try {
                val launcher = toolchains.launcherFor { spec ->
                    spec.languageVersion.set(JavaLanguageVersion.of(javaVersion))
                    spec.vendor.set(vendor)
                }
                // Try to resolve — if foojay is configured, this may trigger download
                if (launcher.isPresent) return launcher
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }
}
