plugins {
    id("org.sonarqube") version "7.2.3.7755"
}

sonar {
    properties {
        property("sonar.projectKey", "ioplane_mutaktor")
        property("sonar.organization", "ioplane")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "*/build/reports/jacoco/test/jacocoTestReport.xml"
        )
        property("sonar.exclusions", "build-logic/**")
    }
}
