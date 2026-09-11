package de.spotlessformatplugin.services.formatters

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.FormatterException
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class GoogleJavaFormatFormatter(
    private val documentTextService: DocumentTextService,
    private val notifier: SpotlessNotifier
) {

    fun format(virtualFile: VirtualFile, settings: SpotlessFormatSettings.State) {
        val extension = virtualFile.extension ?: ""
        if (!extension.equals("java", ignoreCase = true)) {
            notifier.notifyInfo("Google Java Format only applies to Java files. Skipping ${virtualFile.name}.")
            return
        }

        val text = documentTextService.getFileText(virtualFile) ?: return
        try {
            val formatter = Formatter()
            val formatted = formatter.formatSource(text)
            documentTextService.updateDocumentText(virtualFile, formatted, "Google Java Format")
            notifier.notifyInfo("Applying Google Java Format version: ${settings.gjfVersion}")
        } catch (e: FormatterException) {
            notifier.notifyError("Google Java Format error in ${virtualFile.name}: ${e.message}")
        } catch (t: Throwable) {
            notifier.notifyError("Failed to format ${virtualFile.name} with Google Java Format: ${t.message}")
        }
    }
}
