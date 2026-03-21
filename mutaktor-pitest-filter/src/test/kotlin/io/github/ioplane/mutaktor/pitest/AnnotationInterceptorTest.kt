package io.github.ioplane.mutaktor.pitest

import io.kotest.matchers.shouldBe
import org.pitest.mutationtest.build.InterceptorType
import org.junit.jupiter.api.Test

class AnnotationInterceptorTest {

    @Test
    fun `factory provides feature named MUTAKTOR_ANNOTATIONS`() {
        val factory = AnnotationInterceptorFactory()
        val feature = factory.provides()

        feature.name() shouldBe "mutaktor_annotations"
    }

    @Test
    fun `filter has FILTER type`() {
        val interceptor = AnnotationInterceptor()

        interceptor.type() shouldBe InterceptorType.FILTER
    }
}
