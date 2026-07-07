import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class FormatterUtilTest {

    @Test
    fun `google java format should change unformatted java source`() {
        val source = """
            public class Test{public static void main(String[]args){int x=1;System.out.println(x);}}
        """.trimIndent()

        val formatted = de.spotlessformatplugin.format.FormatterUtil.formatContent("google-java-format", "", "Test.java", source)

        // formatted content should differ from the compact unformatted input (if the formatter is available)
        assertNotEquals(source, formatted)
    }
}
