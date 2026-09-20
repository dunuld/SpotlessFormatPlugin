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
        formatterFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="TestProfile" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="4"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="4"/>
                </profile>
            </profiles>
            """.trimIndent()
        )
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

        val after = "public class Test {\n    public void test() {\n        int i = 0;\n    }\n}\n"

        assertEquals(after, psiFile.text)
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

    fun testFormatJavaFileWithCustomImportOrder() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="TestProfile" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="2"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="2"/>
                </profile>
            </profiles>
            """.trimIndent()
        )
        val importOrderFile = File(tempDir, "custom.importorder")
        importOrderFile.writeText(
            """
            #Organize Import Order
            0=java
            1=javax
            2=org
            3=com
            """.trimIndent()
        )

        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath
        settings.importOrderPath = importOrderFile.absolutePath

        val before = """
            import com.example.Foo;
            import java.util.List;
            public class Test {
              List<Foo> list;
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", before)
        eclipseFormatter.format(psiFile.virtualFile, settings)

        val expected = "import java.util.List;\n\nimport com.example.Foo;\n\npublic class Test {\n  List<Foo> list;\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatJavaFileWithPropertiesConfig() {
        val formatterFile = File(tempDir, "formatter.properties")
        formatterFile.writeText(
            """
            org.eclipse.jdt.core.formatter.tabulation.char=space
            org.eclipse.jdt.core.formatter.tabulation.size=2
            org.eclipse.jdt.core.formatter.indentation.size=2
            """.trimIndent()
        )
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath
        settings.importOrderPath = importOrderFile.absolutePath

        val before = """
            public class Test {
            void run() {
            int x = 1;
            }
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", before)
        eclipseFormatter.format(psiFile.virtualFile, settings)

        val expected = "public class Test {\n  void run() {\n    int x = 1;\n  }\n}\n"
        assertEquals(expected, psiFile.text)
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

    fun testFormatJavaFileWithMalformedXmlFallsBackGracefully() {
        val formatterFile = File(tempDir, "malformed.xml")
        formatterFile.writeText("<invalid xml>>>")
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath
        settings.importOrderPath = importOrderFile.absolutePath

        val before = "public class Test { void test() {} }"
        val psiFile = myFixture.configureByText("Test.java", before)

        eclipseFormatter.format(psiFile.virtualFile, settings)

        assertTrue(psiFile.text.contains("public class Test"))
    }
}
