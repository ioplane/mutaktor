package io.github.ioplane.mutaktor.util

/**
 * Resolves the conventional source-root-relative path for a mutated class.
 *
 * Picks `src/main/kotlin` for `.kt` files and `src/main/java` for everything else.
 */
public object SourcePathResolver {

    /**
     * Builds a relative path like `src/main/kotlin/com/example/Foo.kt` from
     * the fully-qualified [mutatedClass] name and the [sourceFile] leaf name.
     */
    public fun resolveRelativePath(mutatedClass: String, sourceFile: String): String {
        val packagePath = mutatedClass.substringBeforeLast('.').replace('.', '/')
        val srcRoot = if (sourceFile.endsWith(".kt")) "src/main/kotlin" else "src/main/java"
        return "$srcRoot/$packagePath/$sourceFile"
    }
}
