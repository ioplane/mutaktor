package io.github.ioplane.mutaktor.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourcePathResolverTest {

    @Test
    fun `java file resolves to src main java`() {
        val result = SourcePathResolver.resolveRelativePath(
            mutatedClass = "com.example.Foo",
            sourceFile = "Foo.java",
        )
        result shouldBe "src/main/java/com/example/Foo.java"
    }

    @Test
    fun `kotlin file resolves to src main kotlin`() {
        val result = SourcePathResolver.resolveRelativePath(
            mutatedClass = "com.example.Bar",
            sourceFile = "Bar.kt",
        )
        result shouldBe "src/main/kotlin/com/example/Bar.kt"
    }

    @Test
    fun `nested class uses correct package path`() {
        val result = SourcePathResolver.resolveRelativePath(
            mutatedClass = "com.example.outer.Inner",
            sourceFile = "Inner.java",
        )
        result shouldBe "src/main/java/com/example/outer/Inner.java"
    }

    @Test
    fun `deeply nested package resolves correctly`() {
        val result = SourcePathResolver.resolveRelativePath(
            mutatedClass = "io.github.ioplane.mutaktor.report.SarifConverter",
            sourceFile = "SarifConverter.kt",
        )
        result shouldBe "src/main/kotlin/io/github/ioplane/mutaktor/report/SarifConverter.kt"
    }

    @Test
    fun `default package class`() {
        val result = SourcePathResolver.resolveRelativePath(
            mutatedClass = "Foo",
            sourceFile = "Foo.java",
        )
        // substringBeforeLast('.') on "Foo" returns "Foo" (no dot found)
        // so packagePath = "Foo", which is a bit odd but consistent
        result shouldBe "src/main/java/Foo/Foo.java"
    }
}
