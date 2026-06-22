package de.spotlessformatplugin.services

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

@Service(Service.Level.PROJECT)
class SpotlessRunner(private val project: Project) {

    fun formatFile(virtualFile: VirtualFile) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        if (!validateSettings(settings, virtualFile)) return

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
        
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
            val extension = virtualFile.extension
            if (extension.equals("java", ignoreCase = true)) {
                OptimizeImportsProcessor(project, psiFile).run()
            }
        }
    }

    private fun validateSettings(state: SpotlessFormatSettings.State, virtualFile: VirtualFile): Boolean {
        val extension = virtualFile.extension
        val formatterPath = state.formatterXmlPath
        val importOrderPath = state.importOrderPath

        if (formatterPath.isBlank()) {
            notifyError("Eclipse Formatter XML path is not configured.")
            return false
        }
        val formatterFile = File(formatterPath)
        if (!formatterFile.exists()) {
            notifyError("Eclipse Formatter XML not found at: $formatterPath")
            return false
        }
        if (!formatterFile.canRead()) {
            notifyError("Eclipse Formatter XML is not readable at: $formatterPath")
            return false
        }

        if (extension.equals("java", ignoreCase = true)) {
            if (importOrderPath.isBlank()) {
                notifyError("Import Order file path is not configured.")
                return false
            }
            val importOrderFile = File(importOrderPath)
            if (!importOrderFile.exists()) {
                notifyError("Import Order file not found at: $importOrderPath")
                return false
            }
            if (!importOrderFile.canRead()) {
                notifyError("Import Order file is not readable at: $importOrderPath")
                return false
            }
        }

        return true
    }

    private fun notifyError(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless Configuration Error", content, NotificationType.ERROR)
            .notify(project)
    }
}
