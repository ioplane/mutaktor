plugins {
    id("kotlin-conventions")
}

dependencies {
    compileOnly(libs.pitest.entry)
    compileOnly(project(":mutaktor-annotations"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.pitest.entry)
    testRuntimeOnly(libs.junit.platform.launcher)
}
