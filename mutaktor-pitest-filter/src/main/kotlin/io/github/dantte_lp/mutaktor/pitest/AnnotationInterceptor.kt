package io.github.dantte_lp.mutaktor.pitest

import org.pitest.bytecode.analysis.ClassTree
import org.pitest.mutationtest.build.InterceptorParameters
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.build.MutationInterceptor
import org.pitest.mutationtest.build.MutationInterceptorFactory
import org.pitest.mutationtest.engine.Mutater
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.plugin.Feature

/**
 * PIT [MutationInterceptorFactory] that registers the [AnnotationInterceptor].
 *
 * Discovered via `META-INF/services/org.pitest.mutationtest.build.MutationInterceptorFactory`.
 */
class AnnotationInterceptorFactory : MutationInterceptorFactory {

    override fun createInterceptor(params: InterceptorParameters): MutationInterceptor =
        AnnotationInterceptor()

    override fun provides(): Feature =
        Feature.named("MUTAKTOR_ANNOTATIONS")
            .withOnByDefault(true)
            .withDescription(description())

    override fun description(): String =
        "Filters mutations based on @SuppressMutations and @MutationCritical annotations"
}

/**
 * A [MutationInterceptor] that reads Mutaktor annotations from bytecode:
 *
 * - `@SuppressMutations` on a class or method: all mutations in that scope are removed.
 * - `@MutationCritical` on a class or method: mutations are kept (they will be
 *   enforced by the ratchet / quality gate to require 100% kill rate).
 *
 * Annotation detection is done by inspecting the [ClassTree] AST for runtime-visible
 * annotation descriptors matching the Mutaktor annotation FQCNs.
 */
class AnnotationInterceptor : MutationInterceptor {

    private companion object {
        /** Bytecode descriptor for `@SuppressMutations`. */
        const val SUPPRESS_DESCRIPTOR =
            "Lio/github/dantte_lp/mutaktor/annotations/SuppressMutations;"

        /** Bytecode descriptor for `@MutationCritical`. */
        const val CRITICAL_DESCRIPTOR =
            "Lio/github/dantte_lp/mutaktor/annotations/MutationCritical;"
    }

    /** Whether the current class is annotated with `@SuppressMutations`. */
    private var suppressClass: Boolean = false

    /** Method names in the current class that have `@SuppressMutations`. */
    private val suppressedMethods = mutableSetOf<String>()

    override fun type(): InterceptorType = InterceptorType.FILTER

    override fun begin(clazz: ClassTree) {
        suppressClass = false
        suppressedMethods.clear()

        // Check class-level annotations
        val classNode = clazz.rawNode()
        val classAnnotations = classNode.visibleAnnotations.orEmpty()
        suppressClass = classAnnotations.any { it.desc == SUPPRESS_DESCRIPTOR }

        // Check method-level annotations
        for (method in classNode.methods.orEmpty()) {
            val methodAnnotations = method.visibleAnnotations.orEmpty()
            if (methodAnnotations.any { it.desc == SUPPRESS_DESCRIPTOR }) {
                suppressedMethods.add(method.name)
            }
        }
    }

    override fun intercept(
        mutations: Collection<MutationDetails>,
        mutater: Mutater,
    ): Collection<MutationDetails> {
        if (suppressClass) return emptyList()

        return mutations.filterNot { mutation ->
            mutation.method in suppressedMethods
        }
    }

    override fun end() {
        suppressClass = false
        suppressedMethods.clear()
    }
}
