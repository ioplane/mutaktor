package io.github.dantte_lp.mutaktor.annotations

/**
 * Suppresses mutation testing for annotated code.
 * Annotated classes/methods are excluded from mutation analysis.
 *
 * Use sparingly — document the reason.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
public annotation class SuppressMutations(val reason: String)
