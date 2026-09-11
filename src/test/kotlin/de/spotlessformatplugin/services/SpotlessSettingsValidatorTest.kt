package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessSettingsValidatorTest : BasePlatformTestCase() {

    private lateinit var validator: SpotlessSettingsValidator

    override fun setUp() {
        super.setUp()
        val notifier = SpotlessNotifier(project)
        validator = SpotlessSettingsValidator(notifier)
    }

    fun testValidateSpotlessConfigEmpty() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = true
        state.spotlessConfigPath = ""

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateSpotlessConfigFileNotFound() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = true
        state.spotlessConfigPath = "non-existent.gradle"

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateSpotlessConfigFileExists() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
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
        val tempDir = myFixture.tempDirFixture.tempDirPath
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
        val tempDir = myFixture.tempDirFixture.tempDirPath
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

    fun testValidatePrettierMissingConfigFile() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.PRETTIER
        state.prettierConfigPath = "/non/existent/.prettierrc"

        val psiFile = myFixture.configureByText("test.js", "const x = 1;")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidatePrettierValidConfigOrEmpty() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.PRETTIER
        state.prettierConfigPath = ""

        val psiFile = myFixture.configureByText("test.js", "const x = 1;")
        assertTrue(validator.validate(state, psiFile.virtualFile, null))
    }

    fun testValidateGoogleJavaFormatVersion() {
        val state = SpotlessFormatSettings.State()
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT
        state.gjfVersion = ""

        val psiFile = myFixture.configureByText("Test.java", "class Test {}")
        assertFalse(validator.validate(state, psiFile.virtualFile, null))

        state.gjfVersion = "1.17.0"
        assertTrue(validator.validate(state, psiFile.virtualFile, null))
    }
}
