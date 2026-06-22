package de.spotlessformatplugin.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.SpotlessRunner
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessSaveListener(private val project: Project) : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        if (!settings.executeOnSave) return

        val fileDocumentManager = FileDocumentManager.getInstance()
        val virtualFile: VirtualFile? = fileDocumentManager.getFile(document)

        if (virtualFile != null && isSupportedFile(virtualFile)) {
            project.getService(SpotlessRunner::class.java).formatFile(virtualFile)
        }
    }

    private fun isSupportedFile(file: VirtualFile): Boolean {
        val extension = file.extension ?: return false
        val settings = SpotlessFormatSettings.getInstance(project).state
        return settings.supportedExtensions
            .split(",")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { it.equals(extension, ignoreCase = true) }
    }
}
