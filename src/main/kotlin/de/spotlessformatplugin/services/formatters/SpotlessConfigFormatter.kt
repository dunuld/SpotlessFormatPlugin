package de.spotlessformatplugin.services.formatters

import com.diffplug.spotless.Formatter as SpotlessFormatter
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.generic.EndWithNewlineStep
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep
import com.diffplug.spotless.java.ImportOrderStep
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import java.io.File

class SpotlessConfigFormatter(
    private val documentTextService: DocumentTextService,
    private val notifier: SpotlessNotifier
) {

    fun format(virtualFile: VirtualFile, configPath: String) {
        val text = documentTextService.getFileText(virtualFile) ?: return
        try {
            val steps = mutableListOf<FormatterStep>()
            val configFile = File(configPath)
            val extension = virtualFile.extension ?: ""

            steps.add(TrimTrailingWhitespaceStep.create())
            steps.add(EndWithNewlineStep.create())

            if (extension.equals("java", ignoreCase = true) && configFile.exists()) {
                if (configPath.endsWith(".order") || configPath.endsWith(".importorder")) {
                    steps.add(ImportOrderStep.forJava().createFrom(configFile))
                }
            }

            val formatter = SpotlessFormatter.builder()
                .lineEndingsPolicy(LineEnding.PLATFORM_NATIVE.createPolicy())
                .encoding(Charsets.UTF_8)
                .steps(steps)
                .build()

            val formatted = formatter.compute(text, File(virtualFile.path))
            documentTextService.updateDocumentText(virtualFile, formatted, "Spotless Formatting")
            notifier.notifyInfo("Using Spotless config: $configPath")
        } catch (e: Exception) {
            notifier.notifyError("Spotless formatting failed for ${virtualFile.name}: ${e.message}")
        }
    }
}
