package de.spotlessformatplugin.format

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.FormatterException

/**
 * Lightweight formatter utility used by SpotlessRunner.
 *
 * Current behavior:
 * - "google-java-format": format Java source using google-java-format library
 * - "kotlin" : attempt to format Kotlin using ktlint if available
 * - "eclipse" / "custom": TODO - planned Spotless/Eclipse integration (falls back to original content)
 *
 * The goal is to perform in-process formatting of file contents so that only the provided files
 * are modified. This util intentionally operates on plain strings so it can be unit-tested
 * without the full IntelliJ test harness.
 */
object FormatterUtil {
    fun formatContent(formatterType: String, configPath: String, fileName: String, content: String): String {
        return when (formatterType.lowercase()) {
            "google-java-format" -> formatWithGoogleJavaFormat(content)
            "kotlin", "ktlint" -> formatKotlinWithKtlint(content)
            // TODO: Implement Eclipse formatter (XML) and custom Spotless steps using spotless-lib
            "eclipse", "custom" -> content
            else -> content
        }
    }

    private fun formatWithGoogleJavaFormat(content: String): String {
        return try {
            Formatter().formatSource(content)
        } catch (e: FormatterException) {
            // If formatting fails, return original content to avoid data loss
            content
        } catch (e: NoClassDefFoundError) {
            // Dependency not present at runtime — return original content
            content
        }
    }

    private fun formatKotlinWithKtlint(content: String): String {
        return try {
            // Try to invoke ktlint's simple formatting entry point via reflection to avoid hard compile dependency on API shape
            val ktlintClass = try { Class.forName("com.pinterest.ktlint.core.KtLint") } catch (ex: ClassNotFoundException) { null }
            if (ktlintClass != null) {
                // Look for a static 'format' method that accepts a String
                val method = ktlintClass.methods.firstOrNull { m -> m.name == "format" && m.parameterCount == 1 && m.parameterTypes[0] == String::class.java }
                if (method != null) {
                    val res = method.invoke(null, content) as? String
                    return res ?: content
                }
                // Newer ktlint API has different signatures; try to call format with params via method name match
                val method2 = ktlintClass.methods.firstOrNull { m -> m.name == "format" && m.parameterCount >= 1 }
                if (method2 != null) {
                    // As we don't know exact params, attempt a best-effort invocation with just the content (may fail)
                    return try { (method2.invoke(null, content) as? String) ?: content } catch (e: Throwable) { content }
                }
            }
            content
        } catch (e: Throwable) {
            content
        }
    }
}
