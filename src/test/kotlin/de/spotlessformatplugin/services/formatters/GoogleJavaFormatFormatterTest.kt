package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings

/**
 * The test JVM gets no --add-exports/--add-opens for jdk.compiler, so these tests also verify that the plugin grants
 * google-java-format the required access at runtime (as it must in the IDE).
 */
class GoogleJavaFormatFormatterTest : BasePlatformTestCase() {

    private lateinit var formatter: GoogleJavaFormatFormatter

    override fun setUp() {
        super.setUp()
        formatter = GoogleJavaFormatFormatter(SpotlessNotifier(project))
    }

    fun testFormatJavaFile() {
        val settings = SpotlessFormatSettings.State()
        val before = "public class Test { public void test() {} }"
        val file = myFixture.configureByText("Test.java", before).virtualFile

        assertEquals("public class Test {\n  public void test() {}\n}\n", formatter.format(file, before, settings))
    }

    fun testFixImportsRemovesUnusedAndSortsImports() {
        val before = "import java.util.List;\nimport java.util.ArrayList;\nimport java.util.Map;\n\nclass Test { ArrayList<String> a; List<String> b; }\n"
        val file = myFixture.configureByText("Imports.java", before).virtualFile

        val settings = SpotlessFormatSettings.State()
        val fixed = formatter.format(file, before, settings)!!
        assertTrue(fixed.startsWith("import java.util.ArrayList;\nimport java.util.List;\n\n"))
        assertFalse(fixed.contains("java.util.Map"))

        settings.gjfFixImports = false
        val untouched = formatter.format(file, before, settings)!!
        assertTrue(untouched.contains("import java.util.Map;"))
    }

    fun testFormatNonJavaFileSkipped() {
        val before = "const a = 1;"
        val file = myFixture.configureByText("test.js", before).virtualFile
        assertNull(formatter.format(file, before, SpotlessFormatSettings.State()))
    }

    fun testSyntaxErrorReturnsNull() {
        val before = "class Test { void run( { }"
        val file = myFixture.configureByText("Broken.java", before).virtualFile
        assertNull(formatter.format(file, before, SpotlessFormatSettings.State()))
    }

    fun testBundledVersionIsKnown() {
        assertFalse(GoogleJavaFormatFormatter.BUNDLED_VERSION == "unknown")
    }
}
