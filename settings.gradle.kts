rootProject.name = "mutaktor"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-download JDKs when not available locally (Qodana, CI, first-time setup)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

include("mutaktor-gradle-plugin")
include("mutaktor-pitest-filter")
include("mutaktor-annotations")
