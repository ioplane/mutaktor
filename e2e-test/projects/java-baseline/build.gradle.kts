plugins {
    java
    id("io.github.ioplane.mutaktor") version "0.1.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }

mutaktor {
    targetClasses.set(setOf("com.example.*"))
    threads.set(2)
}
