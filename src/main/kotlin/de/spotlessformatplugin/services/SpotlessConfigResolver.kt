package de.spotlessformatplugin.services

import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class SpotlessConfigResolver {

    fun resolveConfigPath(settings: SpotlessFormatSettings.State, virtualFile: VirtualFile): String? {
        if (!settings.useSpotlessConfig || settings.spotlessConfigPath.isBlank()) return null

        val configFile = File(settings.spotlessConfigPath)
        if (configFile.isAbsolute) return settings.spotlessConfigPath

        // Hierarchische Suche
        var currentDir = virtualFile.parent
        while (currentDir != null) {
            val fileInDir = File(currentDir.path, settings.spotlessConfigPath)
            if (fileInDir.exists()) {
                return fileInDir.absolutePath
            }
            currentDir = currentDir.parent
        }

        return null
    }
}
