package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessRunnerTest : BasePlatformTestCase() {

    private lateinit var spotlessRunner: SpotlessRunner
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        spotlessRunner = project.getService(SpotlessRunner::class.java)
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.loadState(SpotlessFormatSettings.State())
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testFormatFileWithEclipseIntegration() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="Test" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="4"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="4"/>
                </profile>
            </profiles>
            """.trimIndent()
        )
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = importOrderFile.absolutePath

        val before = """
            public class Test {
            public void test() {
            int i = 0;
            }
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", before)
        spotlessRunner.formatFile(psiFile.virtualFile)

        val expected = "public class Test {\n    public void test() {\n        int i = 0;\n    }\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatFileWithGoogleJavaFormatIntegration() {
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterType = SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT
        settings.state.gjfVersion = "1.17.0"

        val before = "public class Test { public void test() {} }"
        val psiFile = myFixture.configureByText("Test.java", before)

        spotlessRunner.formatFile(psiFile.virtualFile)

        val expected = "public class Test {\n  public void test() {}\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatFileWithSpotlessConfigIntegration() {
        val configFile = File(tempDir, "spotless.gradle")
        configFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.useSpotlessConfig = true
        settings.state.spotlessConfigPath = configFile.absolutePath

        val before = "public class Test {   \n    int a = 1;   \n}"
        val psiFile = myFixture.configureByText("Test.java", before)
        spotlessRunner.formatFile(psiFile.virtualFile)

        val expected = "public class Test {\n    int a = 1;\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatFileValidationFailureAbortsFormatting() {
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = "/non/existent/path.xml"

        val before = "public class Test {}"
        val psiFile = myFixture.configureByText("Test.java", before)
        spotlessRunner.formatFile(psiFile.virtualFile)

        assertEquals(before, psiFile.text)
    }
}
