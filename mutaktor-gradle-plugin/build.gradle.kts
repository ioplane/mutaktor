plugins {
    id("kotlin-conventions")
    `java-gradle-plugin`
}

dependencies {
    implementation(libs.pitest.command.line)

    testImplementation(gradleApi()) // Gradle 9.4 moves gradleApi() to compileOnlyApi
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    plugins {
        create("mutaktor") {
            id = "io.github.dantte-lp.mutaktor"
            implementationClass = "io.github.dantte_lp.mutaktor.MutaktorPlugin"
            displayName = "Mutaktor — Kotlin-first PIT Mutation Testing"
            description = "Gradle plugin for PIT mutation testing with git-aware analysis, Kotlin junk filtering, and CI/CD integration"
            tags = listOf("testing", "mutation-testing", "pitest", "kotlin", "pit")
        }
        create("mutaktorAggregate") {
            id = "io.github.dantte-lp.mutaktor.aggregate"
            implementationClass = "io.github.dantte_lp.mutaktor.MutaktorAggregatePlugin"
            displayName = "Mutaktor Aggregate — Multi-module mutation reports"
            description = "Aggregates mutation testing reports from subprojects"
            tags = listOf("testing", "mutation-testing", "pitest", "aggregate")
        }
    }
}

// Functional tests source set
val functionalTest by sourceSets.creating
configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs functional tests with Gradle TestKit."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.check { dependsOn(functionalTestTask) }

gradlePlugin.testSourceSets(functionalTest)
