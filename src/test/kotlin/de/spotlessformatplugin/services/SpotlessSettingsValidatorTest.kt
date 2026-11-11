package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessSettingsValidatorTest : BasePlatformTestCase() {

    private lateinit var validator: SpotlessSettingsValidator
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        val notifier = SpotlessNotifier(project)
        validator = SpotlessSettingsValidator(project, notifier)
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testValidateGenericModeWithoutImportOrderIsValid() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = true
        state.spotlessConfigPath = ""

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertTrue(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateSpotlessConfigFileNotFound() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = true
        state.spotlessConfigPath = "non-existent.gradle"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateSpotlessConfigFileExists() {
        val configFile = File(tempDir, "spotless.gradle")
        configFile.createNewFile()

        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = true
        state.spotlessConfigPath = "spotless.gradle"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertTrue(validator.validate(state, psiFile.virtualFile, configFile.absolutePath))
    }

    fun testValidateEclipseFormatterMissingPath() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = ""

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateEclipseFormatterNonExistentPath() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = "/non/existent/path.xml"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateEclipseFormatterJavaMissingImportOrder() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = formatterFile.absolutePath
        state.importOrderPath = ""

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateEclipseFormatterXmlIgnoresImportOrder() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = formatterFile.absolutePath
        state.importOrderPath = "/non/existent/path.order"

        val psiFile = myFixture.configureByText("test.xml", "<root/>")
        assertTrue(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testGoogleJavaFormatNeedsNoConfiguration() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertTrue(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testRelativeEclipsePathsAreResolvedAgainstProjectRoot() {
        val baseDir = File(project.basePath!!)
        val configDir = File(baseDir, "validator-config").apply { mkdirs() }
        try {
            File(configDir, "formatter.xml").writeText("<profiles/>")
            File(configDir, "java.importorder").writeText("0=java")

            val state = SpotlessFormatSettings.State()
            state.formatterXmlPath = "validator-config/formatter.xml"
            state.importOrderPath = "${SpotlessPaths.PROJECT_DIR_MACRO}/validator-config/java.importorder"

            val psiFile = myFixture.configureByText("Test.java", "class Test {}")
            assertTrue(validator.validate(state, psiFile.virtualFile, null))
        } finally {
            configDir.deleteRecursively()
        }
    }
}
