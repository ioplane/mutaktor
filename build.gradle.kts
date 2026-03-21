plugins {
    id("org.sonarqube") version "7.2.3.7755"
}

sonar {
    properties {
        property("sonar.projectKey", "ioplane_mutaktor")
        property("sonar.organization", "ioplane")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                "mutaktor-gradle-plugin/build/reports/jacoco/test/jacocoTestReport.xml",
                "mutaktor-pitest-filter/build/reports/jacoco/test/jacocoTestReport.xml",
                "mutaktor-annotations/build/reports/jacoco/test/jacocoTestReport.xml",
            ).joinToString(",")
        )
        property("sonar.sources",
            "mutaktor-gradle-plugin/src/main," +
            "mutaktor-pitest-filter/src/main," +
            "mutaktor-annotations/src/main"
        )
        property("sonar.tests",
            "mutaktor-gradle-plugin/src/test," +
            "mutaktor-gradle-plugin/src/functionalTest," +
            "mutaktor-pitest-filter/src/test"
        )
        property("sonar.exclusions", "build-logic/**,**/build/**")
    }
}
