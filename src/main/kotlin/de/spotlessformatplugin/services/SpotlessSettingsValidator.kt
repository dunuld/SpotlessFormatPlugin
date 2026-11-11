package de.spotlessformatplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessSettingsValidator(private val project: Project, private val notifier: SpotlessNotifier) {

    fun validate(state: SpotlessFormatSettings.State, virtualFile: VirtualFile, resolvedConfigPath: String?): Boolean {
        if (state.useSpotlessConfig) {
            val configPath = state.spotlessConfigPath.trim()
            // The import order file is optional in the generic mode.
            if (configPath.isEmpty()) return true
            val finalConfigPath = resolvedConfigPath ?: configPath
            return checkReadableFile(
                File(finalConfigPath),
                "validation.generic.config.not.found",
                "validation.generic.config.not.readable"
            )
        }

        return when (state.formatterType) {
            SpotlessFormatSettings.FormatterType.ECLIPSE -> validateEclipseSettings(state, virtualFile)
            SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT -> true
        }
    }

    private fun validateEclipseSettings(state: SpotlessFormatSettings.State, virtualFile: VirtualFile): Boolean {
        val formatterFile = SpotlessPaths.resolve(project, state.formatterXmlPath)
        if (formatterFile == null) {
            notifier.notifyConfigurationError(SpotlessBundle.message("validation.eclipse.xml.missing"))
            return false
        }
        if (!checkReadableFile(formatterFile, "validation.eclipse.xml.not.found", "validation.eclipse.xml.not.readable")) {
            return false
        }

        if (virtualFile.extension.equals("java", ignoreCase = true)) {
            val importOrderFile = SpotlessPaths.resolve(project, state.importOrderPath)
            if (importOrderFile == null) {
                notifier.notifyConfigurationError(SpotlessBundle.message("validation.import.order.missing"))
                return false
            }
            if (!checkReadableFile(importOrderFile, "validation.import.order.not.found", "validation.import.order.not.readable")) {
                return false
            }
        }
        return true
    }

    private fun checkReadableFile(file: File, notFoundKey: String, notReadableKey: String): Boolean {
        if (!file.isFile) {
            notifier.notifyConfigurationError(SpotlessBundle.message(notFoundKey, file.path))
            return false
        }
        if (!file.canRead()) {
            notifier.notifyConfigurationError(SpotlessBundle.message(notReadableKey, file.path))
            return false
        }
        return true
    }
}
