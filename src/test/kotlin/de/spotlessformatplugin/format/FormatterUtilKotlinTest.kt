import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class FormatterUtilKotlinTest {

    @Test
    fun `ktlint should change unformatted kotlin source`() {
        val source = """
            fun main(){val x=1;println(x)}
        """.trimIndent()

        val formatted = de.spotlessformatplugin.format.FormatterUtil.formatContent("kotlin", "", "Test.kt", source)

        // If ktlint is available, formatted content should differ
        assertNotEquals(source, formatted)
    }
}
