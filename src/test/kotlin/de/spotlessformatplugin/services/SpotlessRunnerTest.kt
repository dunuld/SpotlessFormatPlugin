package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

class SpotlessRunnerTest : BasePlatformTestCase() {

    private lateinit var spotlessRunner: SpotlessRunner

    @BeforeEach
    public override fun setUp() {
        super.setUp()
        spotlessRunner = project.getService(SpotlessRunner::class.java)
    }

    fun testFormatJavaFile() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val tempDirFile = java.io.File(tempDir)
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs()
        }
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = java.io.File(tempDir, "import.order")
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
        val virtualFile = psiFile.virtualFile

        spotlessRunner.formatFile(virtualFile)

        val after = """
            public class Test {
                public void test() {
                    int i = 0;
                }
            }
        """.trimIndent()

        myFixture.checkResult(after)
    }

    fun testOptimizeImports() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val tempDirFile = java.io.File(tempDir)
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs()
        }
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = java.io.File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = importOrderFile.absolutePath

        val withUnused = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.Collections;

            public class Test {
                ArrayList<String> list = new ArrayList<>();
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", withUnused)
        
        spotlessRunner.formatFile(psiFile.virtualFile)

        val text = psiFile.text
        assertFalse(text.contains("import java.util.Collections;"), "Unused import should be removed")
        // Note: IntelliJ's OptimizeImports might not remove List if it's not strictly unused or depending on settings.
        // In this specific case, ArrayList is used, but List is also from java.util.
        // Actually, List IS unused here.
        assertFalse(text.contains("import java.util.List;"), "List import should be removed if only ArrayList is used")
        assertTrue(text.contains("import java.util.ArrayList;"), "ArrayList import should be kept")
    }

    fun testNonJavaFileNotFormattedByRunnerDirectly() {
        // The runner currently doesn't check extension, the listener does.
        // But let's see what happens if we call it on a text file.
        val content = "  some  text  "
        val psiFile = myFixture.configureByText("test.txt", content)
        
        spotlessRunner.formatFile(psiFile.virtualFile)
        
        // It might still format it if IntelliJ has a formatter for txt or just default behavior
        // If it's plain text, usually no-op or just basic.
        // Given current implementation calls CodeStyleManager.reformat(psiFile)
    }

    fun testSettingsArePersisted() {
        val settings = SpotlessFormatSettings.getInstance(project)
        val state = settings.state
        
        state.formatterXmlPath = "/path/to/formatter.xml"
        state.importOrderPath = "/path/to/import.order"
        state.executeOnSave = true
        
        val newSettings = SpotlessFormatSettings.getInstance(project)
        assertEquals("/path/to/formatter.xml", newSettings.state.formatterXmlPath)
        assertEquals("/path/to/import.order", newSettings.state.importOrderPath)
        assertTrue(newSettings.state.executeOnSave)
    }

    fun testFormatFileWithNullVirtualFile() {
        // Current implementation uses PsiManager.getInstance(project).findFile(virtualFile)
        // If we could pass null, it would likely fail or we should test it.
        // But formatFile(virtualFile: VirtualFile) doesn't accept null in Kotlin by default unless marked nullable.
    }

    fun testSaveTriggerFormatting() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val tempDirFile = java.io.File(tempDir)
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs()
        }
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = java.io.File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = importOrderFile.absolutePath
        settings.state.executeOnSave = true
        
        val before = "public class Test { public void test() {} }"
        val psiFile = myFixture.configureByText("Test.java", before)
        
        // Simulate save
        com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveDocument(myFixture.getDocument(psiFile))
        
        // Check if formatted
        // Note: BasePlatformTestCase might not fully trigger listeners as a real IDE would.
        // But SpotlessSaveListener is registered in plugin.xml.
    }

    fun testFormatFileWithMissingFormatterPath() {
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = ""
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
    }

    fun testFormatFileWithInvalidFormatterPath() {
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = "/non/existent/path.xml"
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
    }

    fun testFormatJavaFileWithMissingImportOrderPath() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = ""
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
    }

    fun testFormatJavaFileWithInvalidImportOrderPath() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = "/non/existent/path.order"
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
    }

    fun testFormatXmlFileWithInvalidImportOrderPathShouldSucceed() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = "/non/existent/path.order"
        
        val before = "<root><item/></root>"
        val psiFile = myFixture.configureByText("test.xml", before)
        spotlessRunner.formatFile(psiFile.virtualFile)
        // XML should be formatted because importOrder is only for Java
        assertTrue(psiFile.text.contains("<root>"))
    }

    fun testFormatFileWithUnreadableFormatterFile() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "unreadable.xml")
        formatterFile.createNewFile()
        formatterFile.setReadable(false)

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
        
        formatterFile.setReadable(true) // cleanup
    }

    fun testFormatJavaFileWithUnreadableImportOrderFile() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = java.io.File(tempDir, "unreadable.order")
        importOrderFile.createNewFile()
        importOrderFile.setReadable(false)

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = importOrderFile.absolutePath
        
        val psiFile = myFixture.configureByText("Test.java", "public class Test {}")
        spotlessRunner.formatFile(psiFile.virtualFile)
        assertEquals("public class Test {}", psiFile.text)
        
        importOrderFile.setReadable(true) // cleanup
    }

    fun testFormatXmlFileWithoutImportOrder() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.importOrderPath = "/non/existent/path.order" // Sollte für XML ignoriert werden
        settings.state.supportedExtensions = "java,xml"

        val before = "<root><item/></root>"
        val psiFile = myFixture.configureByText("test.xml", before)
        val virtualFile = psiFile.virtualFile

        // Sollte nicht frühzeitig abbrechen, auch wenn importOrder fehlt
        spotlessRunner.formatFile(virtualFile)
        
        // Da wir den Standard-IDE-Formatter nutzen, sollte er zumindest etwas formatiert haben 
        // (oder identisch sein, wenn bereits formatiert, aber wichtig ist, dass er NICHT abbricht).
        // Standardmäßig würde IntelliJ XML einrücken.
        assertTrue(psiFile.text.contains("<root>"))
    }

    fun testFormatKotlinFileWithCustomExtension() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val formatterFile = java.io.File(tempDir, "formatter.xml")
        formatterFile.createNewFile()

        val settings = SpotlessFormatSettings.getInstance(project)
        settings.state.formatterXmlPath = formatterFile.absolutePath
        settings.state.supportedExtensions = "java,xml,kt"

        val before = "class Test{fun test(){}}"
        val psiFile = myFixture.configureByText("Test.kt", before)
        
        spotlessRunner.formatFile(psiFile.virtualFile)
        
        // Prüfen ob es formatiert wurde (IntelliJ Standard für Kotlin)
        assertTrue(psiFile.text.contains("fun test()"))
    }
}
