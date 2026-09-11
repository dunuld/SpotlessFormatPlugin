package de.spotlessformatplugin.services

import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessSettingsValidator(private val notifier: SpotlessNotifier) {

    fun validate(state: SpotlessFormatSettings.State, virtualFile: VirtualFile, resolvedConfigPath: String?): Boolean {
        if (state.useSpotlessConfig) {
            val configPath = state.spotlessConfigPath
            if (configPath.isBlank()) {
                notifier.notifyError("Spotless configuration path is not configured.")
                return false
            }

            val finalConfigPath = resolvedConfigPath ?: configPath
            val configFile = File(finalConfigPath)
            if (!configFile.exists()) {
                notifier.notifyError("Spotless configuration file not found at: $finalConfigPath")
                return false
            }
            return true
        }

        return when (state.formatterType) {
            SpotlessFormatSettings.FormatterType.ECLIPSE -> validateEclipseSettings(state, virtualFile)
            SpotlessFormatSettings.FormatterType.PRETTIER -> validatePrettierSettings(state)
            SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT -> validateGoogleJavaFormatSettings(state)
        }
    }

    private fun validateEclipseSettings(state: SpotlessFormatSettings.State, virtualFile: VirtualFile): Boolean {
        val extension = virtualFile.extension
        val formatterPath = state.formatterXmlPath
        val importOrderPath = state.importOrderPath

        if (formatterPath.isBlank()) {
            notifier.notifyError("Eclipse Formatter XML path is not configured.")
            return false
        }
        val formatterFile = File(formatterPath)
        if (!formatterFile.exists()) {
            notifier.notifyError("Eclipse Formatter XML not found at: $formatterPath")
            return false
        }
        if (!formatterFile.canRead()) {
            notifier.notifyError("Eclipse Formatter XML is not readable at: $formatterPath")
            return false
        }

        if (extension.equals("java", ignoreCase = true)) {
            if (importOrderPath.isBlank()) {
                notifier.notifyError("Import Order file path is not configured.")
                return false
            }
            val importOrderFile = File(importOrderPath)
            if (!importOrderFile.exists()) {
                notifier.notifyError("Import Order file not found at: $importOrderPath")
                return false
            }
            if (!importOrderFile.canRead()) {
                notifier.notifyError("Import Order file is not readable at: $importOrderPath")
                return false
            }
        }
        return true
    }

    private fun validatePrettierSettings(state: SpotlessFormatSettings.State): Boolean {
        if (state.prettierConfigPath.isNotBlank()) {
            val configFile = File(state.prettierConfigPath)
            if (!configFile.exists()) {
                notifier.notifyError("Prettier configuration file not found at: ${state.prettierConfigPath}")
                return false
            }
        }
        return true
    }

    private fun validateGoogleJavaFormatSettings(state: SpotlessFormatSettings.State): Boolean {
        if (state.gjfVersion.isBlank()) {
            notifier.notifyError("Google Java Format version is not configured.")
            return false
        }
        return true
    }
}
