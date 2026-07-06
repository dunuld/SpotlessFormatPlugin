package de.spotlessformatplugin.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.SpotlessRunner
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.util.concurrent.ConcurrentHashMap

class SpotlessSaveListener(private val project: Project) : FileDocumentManagerListener {

    // collect files that are being saved in a short window so that Save All results in one batch formatting
    private val pendingFiles = ConcurrentHashMap.newKeySet<VirtualFile>()
    @Volatile
    private var scheduled = false

    override fun beforeDocumentSaving(document: Document) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        if (!settings.executeOnSave) return

        val fileDocumentManager = FileDocumentManager.getInstance()
        val virtualFile: VirtualFile? = fileDocumentManager.getFile(document)

        if (virtualFile != null && isSupportedFile(virtualFile)) {
            pendingFiles.add(virtualFile)
            if (!scheduled) {
                scheduled = true
                ApplicationManager.getApplication().invokeLater {
                    flushPending()
                }
            }
        }
    }

    private fun flushPending() {
        val toFormat = ArrayList<VirtualFile>()
        toFormat.addAll(pendingFiles)
        pendingFiles.clear()
        scheduled = false

        if (toFormat.isNotEmpty()) {
            project.getService(SpotlessRunner::class.java).formatFiles(toFormat)
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
