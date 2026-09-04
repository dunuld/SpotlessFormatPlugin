import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "de.spotlessformatplugin"
version = "0.1.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.v4)
    testRuntimeOnly(libs.junit.vintage.engine)

    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }

    implementation("com.diffplug.spotless:spotless-lib:4.10.1")
}

intellijPlatform {
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
    }
}

tasks.test {
    useJUnitPlatform()
}
