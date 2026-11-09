package de.spotlessformatplugin.services.formatters

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.FormatterException
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.JdkCompilerAccess
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings

/** Formats Java files with the google-java-format version bundled with the plugin. */
class GoogleJavaFormatFormatter(private val notifier: SpotlessNotifier) {

    /** @return the formatted text or `null` if the file was skipped or formatting failed. */
    fun format(virtualFile: VirtualFile, text: String, settings: SpotlessFormatSettings.State): String? {
        if (!virtualFile.extension.equals("java", ignoreCase = true)) {
            notifier.notifyWarning(SpotlessBundle.message("notification.gjf.java.only", virtualFile.name))
            return null
        }

        try {
            JdkCompilerAccess.ensureAccess(Formatter::class.java)
        } catch (e: IllegalStateException) {
            notifier.notifyError(
                SpotlessBundle.message("notification.gjf.access.failed", e.message ?: "", JdkCompilerAccess.MANUAL_VM_OPTIONS)
            )
            return null
        }

        return try {
            val formatter = Formatter()
            val formatted = if (settings.gjfFixImports) formatter.formatSourceAndFixImports(text) else formatter.formatSource(text)
            notifier.notifySuccess(SpotlessBundle.message("notification.success.gjf", virtualFile.name, BUNDLED_VERSION))
            formatted
        } catch (e: FormatterException) {
            notifier.notifyError(SpotlessBundle.message("notification.gjf.syntax.error", virtualFile.name, e.message ?: ""))
            null
        } catch (e: LinkageError) {
            notifier.notifyError(SpotlessBundle.message("notification.formatting.failed", virtualFile.name, e.toString()))
            null
        } catch (e: RuntimeException) {
            notifier.notifyError(SpotlessBundle.message("notification.formatting.failed", virtualFile.name, e.message ?: e.javaClass.simpleName))
            null
        }
    }

    companion object {
        /** Version of the bundled google-java-format library. */
        val BUNDLED_VERSION: String by lazy {
            Formatter::class.java.`package`?.implementationVersion ?: versionFromGjf() ?: "unknown"
        }

        /** The plugin class loader does not expose JAR manifest data, so ask google-java-format itself. */
        private fun versionFromGjf(): String? = try {
            val versionClass = Class.forName("com.google.googlejavaformat.java.GoogleJavaFormatVersion", true, Formatter::class.java.classLoader)
            val method = versionClass.getDeclaredMethod("version")
            method.isAccessible = true
            (method.invoke(null) as? String)?.takeIf { it.isNotBlank() }
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
