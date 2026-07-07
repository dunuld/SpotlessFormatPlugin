package de.spotlessformatplugin.format

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.FormatterException

/**
 * Lightweight formatter utility used by SpotlessRunner.
 *
 * Current behavior:
 * - "google-java-format": format Java source using google-java-format library
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
}
