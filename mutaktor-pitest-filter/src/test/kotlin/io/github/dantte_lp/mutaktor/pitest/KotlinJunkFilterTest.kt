package io.github.dantte_lp.mutaktor.pitest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.pitest.classinfo.ClassName
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.engine.Location
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.mutationtest.engine.MutationIdentifier

class KotlinJunkFilterTest {

    private lateinit var filter: KotlinJunkFilter

    @BeforeEach
    fun setUp() {
        filter = KotlinJunkFilter()
    }

    // -- helpers ----------------------------------------------------------

    private fun mutation(
        className: String,
        methodName: String = "someMethod",
        description: String = "removed conditional",
    ): MutationDetails {
        val location = Location.location(
            ClassName.fromString(className),
            methodName,
            "()V",
        )
        val id = MutationIdentifier(location, 0, "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator")
        return MutationDetails(id, "SomeFile.kt", description, 42, emptyList())
    }

    // -- Pattern 4: DefaultImpls ------------------------------------------

    @Test
    fun `filters DefaultImpls mutations`() {
        val m = mutation("com.example.MyInterface\$DefaultImpls")
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldBeEmpty()
    }

    // -- regular class kept -----------------------------------------------

    @Test
    fun `keeps regular class mutations`() {
        val m = mutation("com.example.RegularClass")
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldContainExactly(m)
    }

    // -- Pattern 1: Intrinsics null-check ---------------------------------

    @Test
    fun `filters Intrinsics null-check mutations`() {
        val m = mutation(
            className = "com.example.Service",
            description = "removed call to kotlin/jvm/internal/Intrinsics::checkNotNullParameter",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldBeEmpty()
    }

    @Test
    fun `filters checkNotNullExpressionValue mutations`() {
        val m = mutation(
            className = "com.example.Service",
            description = "removed call to kotlin/jvm/internal/Intrinsics::checkNotNullExpressionValue",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldBeEmpty()
    }

    // -- Pattern 2: data-class generated methods --------------------------

    @Test
    fun `filters data class component methods`() {
        val mutations = listOf(
            mutation("com.example.Data", methodName = "component1"),
            mutation("com.example.Data", methodName = "component2"),
            mutation("com.example.Data", methodName = "component12"),
            mutation("com.example.Data", methodName = "copy"),
            mutation("com.example.Data", methodName = "copy\$default"),
            mutation("com.example.Data", methodName = "toString"),
            mutation("com.example.Data", methodName = "hashCode"),
            mutation("com.example.Data", methodName = "equals"),
        )
        val result = filter.intercept(mutations, StubMutater)
        result.shouldBeEmpty()
    }

    @Test
    fun `keeps non-data-class methods with similar names in description`() {
        // A method called "process" should not be filtered even if class is a data class
        val m = mutation("com.example.Data", methodName = "process")
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldContainExactly(m)
    }

    // -- Pattern 3: coroutine state machine -------------------------------

    @Test
    fun `filters coroutine invokeSuspend in continuation class`() {
        val m = mutation(
            className = "com.example.MyService\$fetchData\$1",
            methodName = "invokeSuspend",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldBeEmpty()
    }

    @Test
    fun `keeps invokeSuspend in top-level class`() {
        // A top-level class with invokeSuspend should not be filtered (no '$' in name)
        val m = mutation(
            className = "com.example.MyCoroutine",
            methodName = "invokeSuspend",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldContainExactly(m)
    }

    // -- Pattern 5: when hashcode dispatch --------------------------------

    @Test
    fun `filters when-expression hashcode dispatch`() {
        val m = mutation(
            className = "com.example.Router",
            methodName = "route",
            description = "negated conditional — hashCode equals check",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldBeEmpty()
    }

    @Test
    fun `keeps mutation mentioning only hashCode without equals`() {
        val m = mutation(
            className = "com.example.Router",
            methodName = "route",
            description = "removed call to hashCode",
        )
        val result = filter.intercept(listOf(m), StubMutater)
        result.shouldContainExactly(m)
    }

    // -- Factory ----------------------------------------------------------

    @Test
    fun `filter has FILTER type`() {
        filter.type() shouldBe InterceptorType.FILTER
    }

    @Test
    fun `factory provides feature named KOTLIN_JUNK`() {
        val factory = KotlinJunkFilterFactory()
        factory.provides().name() shouldBe "kotlin_junk"
        factory.description() shouldBe "Filters junk mutations in Kotlin compiler-generated bytecode"
    }

    // -- mixed batch ------------------------------------------------------

    @Test
    fun `filters junk and keeps legitimate mutations in mixed batch`() {
        val junk1 = mutation("com.example.Iface\$DefaultImpls")
        val junk2 = mutation("com.example.Svc", description = "removed call to Intrinsics::checkNotNullParameter")
        val junk3 = mutation("com.example.Dto", methodName = "component1")
        val legit1 = mutation("com.example.Service", methodName = "process")
        val legit2 = mutation("com.example.Repo", methodName = "save")

        val result = filter.intercept(listOf(junk1, junk2, junk3, legit1, legit2), StubMutater)
        result shouldHaveSize 2
        result.shouldContainExactly(legit1, legit2)
    }
}

/**
 * Minimal stub [org.pitest.mutationtest.engine.Mutater] — the filter never
 * calls `mutater` so we just need a non-null instance.
 */
private object StubMutater : org.pitest.mutationtest.engine.Mutater {
    override fun findMutations(className: ClassName): List<MutationDetails> = emptyList()
    override fun getMutation(id: MutationIdentifier): org.pitest.mutationtest.engine.Mutant {
        throw UnsupportedOperationException("stub")
    }
}
