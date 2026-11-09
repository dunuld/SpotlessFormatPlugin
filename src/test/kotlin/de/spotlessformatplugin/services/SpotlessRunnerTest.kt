package de.spotlessformatplugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
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

        val before = "public class Test { public void test() {} }"
        val psiFile = myFixture.configureByText("Test.java", before)

        spotlessRunner.formatFile(psiFile.virtualFile)

        val expected = "public class Test {\n  public void test() {}\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatFileWithSpotlessConfigIntegration() {
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.useSpotlessConfig = true

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

    private fun configureEclipse() {
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
        val importOrderFile = File(tempDir, "import.order").apply { createNewFile() }
        val state = SpotlessFormatSettings.getInstance(project).state
        state.formatterXmlPath = formatterFile.absolutePath
        state.importOrderPath = importOrderFile.absolutePath
    }

    fun testResultIsDiscardedWhenDocumentChangedDuringFormatting() {
        configureEclipse()
        val psiFile = myFixture.configureByText("Test.java", "public class Test {\nint a;\n}")
        val document = myFixture.editor.document

        val snapshot = spotlessRunner.createSnapshot(psiFile.virtualFile)!!
        val formatted = spotlessRunner.computeFormattedText(snapshot)!!

        // The user types while the formatter is running.
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, "// typed while formatting")
        }

        val applied = WriteCommandAction.writeCommandAction(project)
            .compute<Boolean, RuntimeException> { spotlessRunner.applyIfUnchanged(snapshot, formatted) }

        assertFalse(applied)
        assertEquals("public class Test {\nint a;\n}// typed while formatting", document.text)
    }

    fun testFormatInBackgroundAppliesResult() {
        configureEclipse()
        val psiFile = myFixture.configureByText("Test.java", "public class Test {\nint a;\n}")

        val future = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
            runBlocking { spotlessRunner.formatInBackground(psiFile.virtualFile) }
        }
        assertTrue(PlatformTestUtil.waitForFuture(future, 60_000))

        assertEquals("public class Test {\n    int a;\n}\n", myFixture.editor.document.text)
    }

    fun testOnlySupportedProjectFilesAreEligibleForSave() {
        val javaFile = myFixture.configureByText("Test.java", "class Test {}").virtualFile
        val textFile = myFixture.configureByText("notes.txt", "text").virtualFile
        val externalFile = LightVirtualFile("External.java", "class External {}")

        ApplicationManager.getApplication().runReadAction {
            assertTrue(spotlessRunner.isEligibleForSave(javaFile))
            assertFalse(spotlessRunner.isEligibleForSave(textFile))
            assertFalse(spotlessRunner.isEligibleForSave(externalFile))
        }
    }
}
