package de.spotlessformatplugin.services

import com.intellij.openapi.project.Project
import java.io.File

/**
 * Resolves user-configured paths. `$PROJECT_DIR$` is expanded, `~` points to the user home and relative paths are
 * resolved against the project root (not the IDE's working directory), so settings can be shared through VCS.
 */
object SpotlessPaths {
    const val PROJECT_DIR_MACRO = $$"$PROJECT_DIR$"

    fun resolve(project: Project, path: String?): File? = resolve(project.basePath, path)

    fun resolve(projectBasePath: String?, path: String?): File? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        var expanded = trimmed
        if (expanded.contains(PROJECT_DIR_MACRO)) {
            if (projectBasePath == null) return File(expanded.replace(PROJECT_DIR_MACRO, "."))
            expanded = expanded.replace(PROJECT_DIR_MACRO, projectBasePath)
        }
        if (expanded == "~" || expanded.startsWith("~/") || expanded.startsWith("~\\")) {
            expanded = System.getProperty("user.home") + expanded.substring(1)
        }

        val file = File(expanded)
        if (file.isAbsolute || projectBasePath == null) return file
        return File(projectBasePath, expanded)
    }
}
