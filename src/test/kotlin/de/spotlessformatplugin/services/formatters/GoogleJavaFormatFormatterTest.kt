package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class GoogleJavaFormatFormatterTest : BasePlatformTestCase() {

    private lateinit var googleJavaFormatFormatter: GoogleJavaFormatFormatter

    override fun setUp() {
        super.setUp()
        val notifier = SpotlessNotifier(project)
        val documentTextService = DocumentTextService(project)
        googleJavaFormatFormatter = GoogleJavaFormatFormatter(documentTextService, notifier)
    }

    fun testFormatJavaFile() {
        val settings = SpotlessFormatSettings.State()
        settings.formatterType = SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT
        settings.gjfVersion = "1.17.0"

        val before = "public class Test { public void test() {} }"
        val psiFile = myFixture.configureByText("Test.java", before)

        googleJavaFormatFormatter.format(psiFile.virtualFile, settings)

        val expected = "public class Test {\n  public void test() {}\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatNonJavaFileSkipped() {
        val settings = SpotlessFormatSettings.State()
        settings.formatterType = SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT
        settings.gjfVersion = "1.17.0"

        val before = "const a = 1;"
        val psiFile = myFixture.configureByText("test.js", before)

        googleJavaFormatFormatter.format(psiFile.virtualFile, settings)

        assertEquals(before, psiFile.text)
    }
}
