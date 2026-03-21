package io.github.ioplane.mutaktor.annotations

/**
 * Marks code that MUST have 100% mutation score.
 * Build fails if any mutant survives in annotated code.
 *
 * Can be applied to classes, methods, or constructors.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
public annotation class MutationCritical(val reason: String = "")
