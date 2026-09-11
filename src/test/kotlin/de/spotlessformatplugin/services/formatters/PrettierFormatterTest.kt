package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class PrettierFormatterTest : BasePlatformTestCase() {

    private lateinit var prettierFormatter: PrettierFormatter
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        val notifier = SpotlessNotifier(project)
        val documentTextService = DocumentTextService(project)
        prettierFormatter = PrettierFormatter(project, documentTextService, notifier)
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testFormatWithPrettierDoesNotCrash() {
        val configFile = File(tempDir, ".prettierrc")
        configFile.createNewFile()

        val settings = SpotlessFormatSettings.State()
        settings.formatterType = SpotlessFormatSettings.FormatterType.PRETTIER
        settings.prettierConfigPath = configFile.absolutePath

        val before = "function test() { return 1 }"
        val psiFile = myFixture.configureByText("test.js", before)

        prettierFormatter.format(psiFile.virtualFile, settings)

        assertTrue(psiFile.text.contains("function test"))
    }
}
