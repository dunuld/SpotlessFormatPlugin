package de.spotlessformatplugin.services.formatters

import com.diffplug.spotless.Formatter as SpotlessFormatter
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.generic.EndWithNewlineStep
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep
import com.diffplug.spotless.java.ImportOrderStep
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.SpotlessNotifier
import java.io.File

/**
 * Generic Spotless steps: trims trailing whitespace, ensures a final newline and - for Java files - applies an
 * optional import order file. Spotless build configuration (e.g. `spotless.gradle`) is NOT evaluated.
 */
class SpotlessConfigFormatter(private val notifier: SpotlessNotifier) {

    /**
     * @param importOrderPath resolved path of the import order file, or `null`/blank if none is configured.
     * @return the formatted text or `null` if formatting failed.
     */
    fun format(virtualFile: VirtualFile, text: String, importOrderPath: String?): String? {
        return try {
            val steps = mutableListOf<FormatterStep>()
            steps.add(TrimTrailingWhitespaceStep.create())
            steps.add(EndWithNewlineStep.create())

            val importOrderFile = importOrderPath?.takeIf { it.isNotBlank() }?.let(::File)
            if (importOrderFile != null && virtualFile.extension.equals("java", ignoreCase = true) && importOrderFile.isFile) {
                if (isBuildConfiguration(importOrderFile)) {
                    notifier.notifyWarning(SpotlessBundle.message("notification.generic.build.config.ignored", importOrderFile.name))
                } else {
                    steps.add(ImportOrderStep.forJava().createFrom(importOrderFile))
                }
            }

            val formatter = SpotlessFormatter.builder()
                .lineEndingsPolicy(LineEnding.UNIX.createPolicy())
                .encoding(Charsets.UTF_8)
                .steps(steps)
                .build()

            val formatted = formatter.use { it.compute(text, File(virtualFile.path)) }
            notifier.notifySuccess(SpotlessBundle.message("notification.success.generic", virtualFile.name))
            formatted
        } catch (e: Exception) {
            notifier.notifyError(SpotlessBundle.message("notification.formatting.failed", virtualFile.name, e.message ?: e.javaClass.simpleName))
            null
        }
    }

    companion object {
        private val BUILD_CONFIG_EXTENSIONS = setOf("gradle", "kts", "xml", "groovy")

        /** Earlier versions suggested pointing this setting to a Spotless build script; those cannot be evaluated. */
        fun isBuildConfiguration(file: File): Boolean =
            file.extension.lowercase() in BUILD_CONFIG_EXTENSIONS
    }
}
