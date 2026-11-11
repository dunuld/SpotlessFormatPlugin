package de.spotlessformatplugin.services.formatters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.DocumentUpdater
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

    /** Formats the file's text and writes the result back so assertions can use psiFile.text. */
    private fun format(psiFile: PsiFile, settings: SpotlessFormatSettings.State, javaRelease: Int? = null) {
        val result = eclipseFormatter.format(psiFile.virtualFile, psiFile.text, settings, javaRelease) ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
            DocumentUpdater.applyMinimalChanges(document, result)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
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
        format(psiFile, settings)

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
        format(psiFile, settings)

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
        format(psiFile, settings)

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
        format(psiFile, settings)

        val expected = "public class Test {\n  void run() {\n    int x = 1;\n  }\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testFormatXmlFile() {
        val settings = SpotlessFormatSettings.State()
        val before = "<root><item/></root>"
        val psiFile = myFixture.configureByText("test.xml", before)

        format(psiFile, settings)

        assertTrue(psiFile.text.contains("<root>"))
    }

    fun testFormatKotlinFile() {
        val settings = SpotlessFormatSettings.State()
        val before = "class Test{fun test(){}}"
        val psiFile = myFixture.configureByText("Test.kt", before)

        format(psiFile, settings)

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

        format(psiFile, settings)

        assertTrue(psiFile.text.contains("public class Test"))
    }

    fun testGetProfileNames() {
        val xmlFile = File(tempDir, "multi_profiles.xml")
        xmlFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="ProfileTwoSpaces" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="2"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="2"/>
                </profile>
                <profile kind="CodeFormatterProfile" name="ProfileFourSpaces" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="4"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="4"/>
                </profile>
            </profiles>
            """.trimIndent()
        )

        val profileNames = EclipseFormatter.getProfileNames(xmlFile)
        assertEquals(listOf("ProfileTwoSpaces", "ProfileFourSpaces"), profileNames)

        val nonExistent = EclipseFormatter.getProfileNames(File(tempDir, "non_existent.xml"))
        assertTrue(nonExistent.isEmpty())

        val invalidXml = File(tempDir, "invalid.xml")
        invalidXml.writeText("invalid")
        assertTrue(EclipseFormatter.getProfileNames(invalidXml).isEmpty())
    }

    fun testFormatJavaFileWithSelectedProfile() {
        val formatterFile = File(tempDir, "multi_profiles.xml")
        formatterFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="TwoSpaces" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="2"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="2"/>
                </profile>
                <profile kind="CodeFormatterProfile" name="FourSpaces" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="4"/>
                    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="4"/>
                </profile>
            </profiles>
            """.trimIndent()
        )
        val importOrderFile = File(tempDir, "import.order")
        importOrderFile.createNewFile()

        val before = """
            public class Test {
            public void test() {
            int i = 0;
            }
            }
        """.trimIndent()

        // Format with TwoSpaces profile
        val settingsTwo = SpotlessFormatSettings.State()
        settingsTwo.formatterXmlPath = formatterFile.absolutePath
        settingsTwo.formatterProfile = "TwoSpaces"
        settingsTwo.importOrderPath = importOrderFile.absolutePath

        val psiFileTwo = myFixture.configureByText("TestTwo.java", before)
        format(psiFileTwo, settingsTwo)
        val expectedTwo = "public class Test {\n  public void test() {\n    int i = 0;\n  }\n}\n"
        assertEquals(expectedTwo, psiFileTwo.text)

        // Format with FourSpaces profile
        val settingsFour = SpotlessFormatSettings.State()
        settingsFour.formatterXmlPath = formatterFile.absolutePath
        settingsFour.formatterProfile = "FourSpaces"
        settingsFour.importOrderPath = importOrderFile.absolutePath

        val psiFileFour = myFixture.configureByText("TestFour.java", before)
        format(psiFileFour, settingsFour)
        val expectedFour = "public class Test {\n    public void test() {\n        int i = 0;\n    }\n}\n"
        assertEquals(expectedFour, psiFileFour.text)

        // Fallback when unknown profile name is supplied
        val settingsFallback = SpotlessFormatSettings.State()
        settingsFallback.formatterXmlPath = formatterFile.absolutePath
        settingsFallback.formatterProfile = "UnknownProfile"
        settingsFallback.importOrderPath = importOrderFile.absolutePath

        val psiFileFallback = myFixture.configureByText("TestFallback.java", before)
        format(psiFileFallback, settingsFallback)
        assertEquals(expectedTwo, psiFileFallback.text)
    }

    fun testRelativePathsAreResolvedAgainstProjectRoot() {
        val baseDir = File(project.basePath!!)
        val configDir = File(baseDir, "spotless-test-config")
        configDir.mkdirs()
        try {
            File(configDir, "formatter.xml").writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <profiles version="13">
                    <profile kind="CodeFormatterProfile" name="TwoSpaces" version="13">
                        <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                        <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="2"/>
                        <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="2"/>
                    </profile>
                </profiles>
                """.trimIndent()
            )
            val settings = SpotlessFormatSettings.State()
            settings.formatterXmlPath = "spotless-test-config/formatter.xml"

            val psiFile = myFixture.configureByText("Relative.java", "class Relative {\nvoid run() {\n}\n}")
            format(psiFile, settings)

            assertEquals("class Relative {\n  void run() {\n  }\n}\n", psiFile.text)
        } finally {
            configDir.deleteRecursively()
        }
    }

    fun testUsesJavaReleaseOfTheFileForParsing() {
        val formatterFile = File(tempDir, "formatter.xml")
        formatterFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <profiles version="13">
                <profile kind="CodeFormatterProfile" name="Default" version="13">
                    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
                </profile>
            </profiles>
            """.trimIndent()
        )
        val settings = SpotlessFormatSettings.State()
        settings.formatterXmlPath = formatterFile.absolutePath

        // Record patterns in switch need Java 21; the source level is now taken from the file's module.
        val before = "class Test {\nString f(Object o) {\nreturn switch (o) {\ncase Point(int x, int y) -> \"p\";\ndefault -> \"o\";\n};\n}\nrecord Point(int x, int y) {}\n}"
        val formatted = eclipseFormatter.format(myFixture.configureByText("Test.java", before).virtualFile, before, settings, 21)

        assertNotNull(formatted)
        assertTrue(formatted!!.contains("case Point(int x, int y) -> \"p\";"))
        assertTrue(formatted != before)
    }

    fun testJavaSourceLevelMapping() {
        assertEquals("1.8", EclipseFormatter.javaSourceLevel(8))
        assertEquals("17", EclipseFormatter.javaSourceLevel(null))
        assertEquals("21", EclipseFormatter.javaSourceLevel(21))
        // Never exceeds what the bundled JDT supports
        val capped = EclipseFormatter.javaSourceLevel(999)
        assertTrue(capped.toInt() < 999)
    }
}
