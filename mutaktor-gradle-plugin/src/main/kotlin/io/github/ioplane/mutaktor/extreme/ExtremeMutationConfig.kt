package io.github.ioplane.mutaktor.extreme

/**
 * Configuration for extreme mutation testing mode.
 *
 * Extreme mutation replaces entire method bodies with trivial returns,
 * generating far fewer mutants than traditional fine-grained mutation.
 * It effectively detects pseudo-tested methods (methods with tests that
 * don't actually verify the method's behavior).
 */
object ExtremeMutationConfig {

    /** Mutators for extreme mode — method body removal/replacement. */
    val EXTREME_MUTATORS: Set<String> = setOf(
        "VOID_METHOD_CALLS",      // removes void method calls
        "EMPTY_RETURNS",          // replaces object returns with empty/default
        "FALSE_RETURNS",          // replaces boolean returns with false
        "TRUE_RETURNS",           // replaces boolean returns with true
        "NULL_RETURNS",           // replaces object returns with null
        "PRIMITIVE_RETURNS",      // replaces primitive returns with 0
    )

    /** Apply extreme mutators to the given existing set, replacing them. */
    fun applyTo(currentMutators: Set<String>): Set<String> = EXTREME_MUTATORS

    /** Description for logging. */
    const val DESCRIPTION: String = "extreme mode: method body removal (6 mutators)"
}
