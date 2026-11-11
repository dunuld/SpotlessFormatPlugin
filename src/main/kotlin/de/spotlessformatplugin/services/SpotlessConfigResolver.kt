package de.spotlessformatplugin.services

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

/** Resolves the file configured for the generic Spotless steps mode. */
class SpotlessConfigResolver {

    /**
     * Absolute paths and paths containing `$PROJECT_DIR$` are used as they are. Relative paths are searched upwards
     * from the formatted file's directory, but never above [searchBoundary] (normally the project root). If nothing is
     * found, the path is resolved against the project root.
     */
    fun resolveConfigPath(
        settings: SpotlessFormatSettings.State,
        virtualFile: VirtualFile,
        searchBoundary: VirtualFile?,
        projectBasePath: String?
    ): String? {
        val configuredPath = settings.spotlessConfigPath.trim()
        if (!settings.useSpotlessConfig || configuredPath.isEmpty()) return null

        if (configuredPath.contains(SpotlessPaths.PROJECT_DIR_MACRO) || File(configuredPath).isAbsolute) {
            return SpotlessPaths.resolve(projectBasePath, configuredPath)?.absolutePath
        }

        var currentDir = virtualFile.parent
        while (currentDir != null) {
            if (currentDir.isInLocalFileSystem) {
                val candidate = File(currentDir.path, configuredPath)
                if (candidate.isFile) return candidate.absolutePath
            }
            if (searchBoundary == null || currentDir == searchBoundary ||
                !VfsUtilCore.isAncestor(searchBoundary, currentDir, true)
            ) {
                break
            }
            currentDir = currentDir.parent
        }

        return SpotlessPaths.resolve(projectBasePath, configuredPath)?.takeIf { it.isFile }?.absolutePath
    }
}
