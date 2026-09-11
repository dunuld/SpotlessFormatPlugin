package de.spotlessformatplugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.formatters.EclipseFormatter
import de.spotlessformatplugin.services.formatters.GoogleJavaFormatFormatter
import de.spotlessformatplugin.services.formatters.PrettierFormatter
import de.spotlessformatplugin.services.formatters.SpotlessConfigFormatter
import de.spotlessformatplugin.settings.SpotlessFormatSettings

@Service(Service.Level.PROJECT)
class SpotlessRunner(private val project: Project) {

    private val notifier = SpotlessNotifier(project)
    private val documentTextService = DocumentTextService(project)
    private val configResolver = SpotlessConfigResolver()
    private val validator = SpotlessSettingsValidator(notifier)
    private val eclipseFormatter = EclipseFormatter(project, notifier)
    private val prettierFormatter = PrettierFormatter(project, documentTextService, notifier)
    private val googleJavaFormatFormatter = GoogleJavaFormatFormatter(documentTextService, notifier)
    private val spotlessConfigFormatter = SpotlessConfigFormatter(documentTextService, notifier)

    fun formatFile(virtualFile: VirtualFile) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        val configPath = configResolver.resolveConfigPath(settings, virtualFile)

        if (!validator.validate(settings, virtualFile, configPath)) return

        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
            performFormat(virtualFile, settings, configPath, null)
            return
        }

        val progressWindow = ProgressWindow(true, false, project)
        progressWindow.title = "Formatting with Spotless"
        progressWindow.setDelayInMillis(500)

        application.executeOnPooledThread {
            ProgressManager.getInstance().runProcess({
                performFormat(virtualFile, settings, configPath, ProgressManager.getInstance().progressIndicator)
            }, progressWindow)
        }
    }

    private fun performFormat(
        virtualFile: VirtualFile,
        settings: SpotlessFormatSettings.State,
        configPath: String?,
        indicator: ProgressIndicator?
    ) {
        indicator?.isIndeterminate = true
        indicator?.text = "Formatting ${virtualFile.name}..."

        if (settings.useSpotlessConfig) {
            spotlessConfigFormatter.format(virtualFile, configPath ?: settings.spotlessConfigPath)
        } else {
            when (settings.formatterType) {
                SpotlessFormatSettings.FormatterType.ECLIPSE -> eclipseFormatter.format(virtualFile, settings)
                SpotlessFormatSettings.FormatterType.PRETTIER -> prettierFormatter.format(virtualFile, settings)
                SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT -> googleJavaFormatFormatter.format(virtualFile, settings)
            }
        }
    }
}
