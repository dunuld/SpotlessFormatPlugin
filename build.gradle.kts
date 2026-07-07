import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "de.spotlessformatplugin"
version = "0.0.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.v4)
    testRuntimeOnly(libs.junit.vintage.engine)

    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }

    // Spotless core library to allow in-process formatting steps in future
    implementation("com.diffplug.spotless:spotless-lib:2.23.0")
    // Google Java Format for in-process Java formatting
    implementation("com.google.googlejavaformat:google-java-format:1.15.0")
}

tasks.test {
    useJUnitPlatform()
}
