package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.SpotlessNotifier
import java.io.File

class SpotlessConfigFormatterTest : BasePlatformTestCase() {

    private lateinit var formatter: SpotlessConfigFormatter
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        formatter = SpotlessConfigFormatter(SpotlessNotifier(project))
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testTrimsTrailingWhitespaceAndAddsNewlineWithoutImportOrder() {
        val before = "public class Test {   \n    int a = 1;   \n}"
        val file = myFixture.configureByText("Test.java", before).virtualFile

        assertEquals("public class Test {\n    int a = 1;\n}\n", formatter.format(file, before, null))
    }

    fun testAppliesImportOrderFile() {
        val importOrderFile = File(tempDir, "custom.importorder")
        importOrderFile.writeText("0=java\n1=javax\n2=org\n3=com\n")

        val before = "import org.junit.Test;\nimport java.util.List;\n\npublic class Test {\n    List<String> list;\n}"
        val file = myFixture.configureByText("Test.java", before).virtualFile

        val expected = "import java.util.List;\n\nimport org.junit.Test;\n\npublic class Test {\n    List<String> list;\n}\n"
        assertEquals(expected, formatter.format(file, before, importOrderFile.absolutePath))
    }

    fun testBuildScriptIsNotUsedAsImportOrder() {
        val buildScript = File(tempDir, "spotless.gradle")
        buildScript.writeText("spotless { java { importOrder('com', 'java') } }")

        val before = "import org.junit.Test;\nimport java.util.List;\n\nclass Test {}   "
        val file = myFixture.configureByText("Test.java", before).virtualFile

        // Only whitespace steps are applied, imports stay as they are.
        assertEquals("import org.junit.Test;\nimport java.util.List;\n\nclass Test {}\n", formatter.format(file, before, buildScript.absolutePath))
    }
}
