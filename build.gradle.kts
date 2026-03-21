plugins {
    id("org.sonarqube") version "7.2.3.7755"
}

sonar {
    properties {
        property("sonar.projectKey", "ioplane_mutaktor")
        property("sonar.organization", "ioplane")
    }
}
