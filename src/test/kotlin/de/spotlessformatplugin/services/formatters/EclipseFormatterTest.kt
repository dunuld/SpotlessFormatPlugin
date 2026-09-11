package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class EclipseFormatterTest : BasePlatformTestCase() {

    private lateinit var eclipseFormatter: EclipseFormatter
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        val notifier = SpotlessNotifier(project)
        eclipseFormatter = EclipseFormatter(project, notifier)
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testFormatJavaFile() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath
        settings.importOrderPath = importOrderFile.absolutePath

        val before = """
            public class Test {
            public void test() {
            int i = 0;
            }
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", before)
        eclipseFormatter.format(psiFile.virtualFile, settings)

        val after = """
            public class Test {
                public void test() {
                    int i = 0;
                }
            }
        """.trimIndent()

        myFixture.checkResult(after)
    }

    fun testOptimizeImportsDoesNotCrash() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.createNewFile()
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath
        settings.importOrderPath = importOrderFile.absolutePath

        val withUnused = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.Collections;

            public class Test {
                ArrayList<String> list = new ArrayList<>();
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", withUnused)
        eclipseFormatter.format(psiFile.virtualFile, settings)

        assertTrue(psiFile.text.contains("public class Test"))
    }

    fun testFormatXmlFile() {
        val settings = SpotlessFormatSettings.State()
        val before = "<root><item/></root>"
        val psiFile = myFixture.configureByText("test.xml", before)

        eclipseFormatter.format(psiFile.virtualFile, settings)

        assertTrue(psiFile.text.contains("<root>"))
    }

    fun testFormatKotlinFile() {
        val settings = SpotlessFormatSettings.State()
        val before = "class Test{fun test(){}}"
        val psiFile = myFixture.configureByText("Test.kt", before)

        eclipseFormatter.format(psiFile.virtualFile, settings)

        assertTrue(psiFile.text.contains("fun test()"))
    }
}
