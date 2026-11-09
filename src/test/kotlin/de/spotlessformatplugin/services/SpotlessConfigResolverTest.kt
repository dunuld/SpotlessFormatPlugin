package de.spotlessformatplugin.services

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessConfigResolverTest : BasePlatformTestCase() {

    private val resolver = SpotlessConfigResolver()
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    private fun settings(path: String, enabled: Boolean = true) = SpotlessFormatSettings.State().apply {
        useSpotlessConfig = enabled
        spotlessConfigPath = path
    }

    private fun vFile(file: File) = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!

    fun testResolveConfigPathDisabled() {
        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertNull(resolver.resolveConfigPath(settings("java.importorder", enabled = false), psiFile.virtualFile, null, null))
    }

    fun testResolveConfigPathBlank() {
        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertNull(resolver.resolveConfigPath(settings("  "), psiFile.virtualFile, null, null))
    }

    fun testResolveConfigPathAbsolute() {
        val configFile = File(tempDir, "abs.importorder").apply { createNewFile() }
        val psiFile = myFixture.configureByText("Test.java", "class Test {}")

        assertEquals(configFile.absolutePath, resolver.resolveConfigPath(settings(configFile.absolutePath), psiFile.virtualFile, null, null))
    }

    fun testResolveConfigPathHierarchicalWithinBoundary() {
        val rootDir = File(tempDir, "projectRoot")
        val subDir = File(rootDir, "subModule/src").apply { mkdirs() }
        val configFile = File(rootDir, "java.importorder").apply { createNewFile() }
        val javaFile = File(subDir, "Test.java").apply { createNewFile() }

        val resolved = resolver.resolveConfigPath(settings("java.importorder"), vFile(javaFile), vFile(rootDir), rootDir.absolutePath)

        assertEquals(configFile.absolutePath, resolved)
    }

    fun testSearchStopsAtBoundary() {
        // Config file above the project root must not be picked up.
        File(tempDir, "java.importorder").createNewFile()
        val rootDir = File(tempDir, "projectRoot")
        val subDir = File(rootDir, "src").apply { mkdirs() }
        val javaFile = File(subDir, "Test.java").apply { createNewFile() }

        val resolved = resolver.resolveConfigPath(settings("java.importorder"), vFile(javaFile), vFile(rootDir), rootDir.absolutePath)

        assertNull(resolved)
    }

    fun testFallsBackToProjectRelativePath() {
        val rootDir = File(tempDir, "projectRoot")
        val configFile = File(rootDir, "config/java.importorder").apply { parentFile.mkdirs(); createNewFile() }
        val javaFile = File(rootDir, "src/Test.java").apply { parentFile.mkdirs(); createNewFile() }

        val resolved = resolver.resolveConfigPath(settings("config/java.importorder"), vFile(javaFile), vFile(rootDir), rootDir.absolutePath)

        assertEquals(configFile.absolutePath, resolved)
    }

    fun testResolveConfigPathNotFound() {
        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertNull(resolver.resolveConfigPath(settings("non-existent.importorder"), psiFile.virtualFile, null, tempDir.absolutePath))
    }
}
