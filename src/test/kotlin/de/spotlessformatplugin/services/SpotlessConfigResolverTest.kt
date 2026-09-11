package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessConfigResolverTest : BasePlatformTestCase() {

    private val resolver = SpotlessConfigResolver()

    fun testResolveConfigPathDisabled() {
        val settings = SpotlessFormatSettings.State()
        settings.useSpotlessConfig = false
        settings.spotlessConfigPath = "spotless.gradle"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        val resolved = resolver.resolveConfigPath(settings, psiFile.virtualFile)

        assertNull(resolved)
    }

    fun testResolveConfigPathBlank() {
        val settings = SpotlessFormatSettings.State()
        settings.useSpotlessConfig = true
        settings.spotlessConfigPath = "  "

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        val resolved = resolver.resolveConfigPath(settings, psiFile.virtualFile)

        assertNull(resolved)
    }

    fun testResolveConfigPathAbsolute() {
        val tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        try {
            val configFile = File(tempDir, "abs-spotless.xml")
            configFile.createNewFile()

            val settings = SpotlessFormatSettings.State()
            settings.useSpotlessConfig = true
            settings.spotlessConfigPath = configFile.absolutePath

            val psiFile = myFixture.configureByText("Test.java", "class Test {}")
            val resolved = resolver.resolveConfigPath(settings, psiFile.virtualFile)

            assertEquals(configFile.absolutePath, resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun testResolveConfigPathHierarchical() {
        val tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        try {
            val rootDir = File(tempDir, "projectRoot")
            rootDir.mkdirs()
            val subDir = File(rootDir, "subModule")
            subDir.mkdirs()

            val configFile = File(rootDir, "spotless.gradle")
            configFile.createNewFile()

            val javaFile = File(subDir, "Test.java")
            javaFile.createNewFile()

            val vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(javaFile)
            assertNotNull(vFile)

            val settings = SpotlessFormatSettings.State()
            settings.useSpotlessConfig = true
            settings.spotlessConfigPath = "spotless.gradle"

            val resolved = resolver.resolveConfigPath(settings, vFile!!)

            assertNotNull(resolved)
            assertEquals(configFile.absolutePath, resolved)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun testResolveConfigPathNotFound() {
        val settings = SpotlessFormatSettings.State()
        settings.useSpotlessConfig = true
        settings.spotlessConfigPath = "non-existent-config.gradle"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        val resolved = resolver.resolveConfigPath(settings, psiFile.virtualFile)

        assertNull(resolved)
    }
}
