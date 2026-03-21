rootProject.name = "mutaktor"

pluginManagement {
    includeBuild("build-logic")
}

include("mutaktor-gradle-plugin")
include("mutaktor-pitest-filter")
include("mutaktor-annotations")
