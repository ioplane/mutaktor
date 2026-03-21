package io.github.dantte_lp.mutaktor

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Type-safe DSL extension for Mutaktor.
 *
 * ```kotlin
 * mutaktor {
 *     pitVersion = "1.23.0"
 *     targetClasses = setOf("com.example.*")
 *     threads = Runtime.getRuntime().availableProcessors()
 *     since = "main"  // git-diff scoped analysis
 * }
 * ```
 */
public abstract class MutaktorExtension @Inject constructor(objects: ObjectFactory) {

    /** PIT version to use. Default: 1.23.0 */
    public val pitVersion: Property<String> = objects.property(String::class.java)
        .convention(MutaktorPlugin.DEFAULT_PIT_VERSION)

    /** Target classes to mutate (glob patterns). Default: inferred from project group. */
    public val targetClasses: SetProperty<String> = objects.setProperty(String::class.java)

    /** Target tests (glob patterns). Default: same as targetClasses. */
    public val targetTests: SetProperty<String> = objects.setProperty(String::class.java)

    /** Number of threads for mutation analysis. */
    public val threads: Property<Int> = objects.property(Int::class.java)

    /** Git ref to diff against (e.g., "main", "HEAD~5"). Enables git-scoped analysis. */
    public val since: Property<String> = objects.property(String::class.java)
}
