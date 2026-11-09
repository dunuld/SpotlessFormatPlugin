import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// group and version come from gradle.properties (single source of truth).

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }

    implementation(libs.spotless.lib)
    implementation(libs.google.java.format)
    implementation(libs.eclipseJdtCore)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.v4)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}

changelog {
    repositoryUrl = "https://github.com/dunuld/SpotlessFormatPlugin"
    groups.empty()
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }

        changeNotes = provider {
            val version = project.version.toString()
            with(changelog) {
                renderItem(
                    (getOrNull(version) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

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
    // No --add-exports/--add-opens for jdk.compiler here on purpose: the plugin grants google-java-format the
    // required access at runtime (JdkCompilerAccess), and the tests verify that it works without extra JVM flags.
}
