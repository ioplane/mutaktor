plugins {
    id("org.sonarqube") version "7.3.0.8198"
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
