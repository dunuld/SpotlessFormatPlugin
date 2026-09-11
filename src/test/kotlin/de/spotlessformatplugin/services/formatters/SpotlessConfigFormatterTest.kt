package de.spotlessformatplugin.services.formatters

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import java.io.File

class SpotlessConfigFormatterTest : BasePlatformTestCase() {

    private lateinit var spotlessConfigFormatter: SpotlessConfigFormatter

    override fun setUp() {
        super.setUp()
        val notifier = SpotlessNotifier(project)
        val documentTextService = DocumentTextService(project)
        spotlessConfigFormatter = SpotlessConfigFormatter(documentTextService, notifier)
    }

    fun testSpotlessConfigActuallyFormatsTrailingWhitespaceAndIndent() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val configFile = File(tempDir, "spotless.gradle")
        configFile.createNewFile()

        val before = "public class Test {   \n    int a = 1;   \n}"
        val psiFile = myFixture.configureByText("Test.java", before)

        spotlessConfigFormatter.format(psiFile.virtualFile, configFile.absolutePath)

        val expected = "public class Test {\n    int a = 1;\n}\n"
        assertEquals(expected, psiFile.text)
    }

    fun testSpotlessConfigWithImportOrder() {
        val tempDir = myFixture.tempDirFixture.tempDirPath
        val importOrderFile = File(tempDir, "custom.importorder")
        importOrderFile.writeText("0=java\n1=javax\n2=org\n3=com\n")

        val before = """
            import org.junit.Test;
            import java.util.List;

            public class Test {
                List<String> list;
            }
        """.trimIndent()

        val psiFile = myFixture.configureByText("Test.java", before)
        spotlessConfigFormatter.format(psiFile.virtualFile, importOrderFile.absolutePath)

        val expected = """
            import java.util.List;

            import org.junit.Test;

            public class Test {
                List<String> list;
            }
            
        """.trimIndent()

        assertEquals(expected, psiFile.text)
    }
}
