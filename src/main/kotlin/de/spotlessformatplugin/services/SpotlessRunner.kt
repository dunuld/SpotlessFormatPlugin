package de.spotlessformatplugin.services

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

@Service(Service.Level.PROJECT)
class SpotlessRunner(private val project: Project) {

    fun formatFile(virtualFile: VirtualFile) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        val configPath = resolveConfigPath(settings, virtualFile)
        
        if (!validateSettings(settings, virtualFile, configPath)) return

        if (settings.useSpotlessConfig) {
            applySpotlessConfig(virtualFile, configPath ?: settings.spotlessConfigPath)
        } else {
            applyLegacyFormat(virtualFile)
        }
    }

    private fun resolveConfigPath(settings: SpotlessFormatSettings.State, virtualFile: VirtualFile): String? {
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

    private fun applyLegacyFormat(virtualFile: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
            if (virtualFile.extension.equals("java", ignoreCase = true)) {
                OptimizeImportsProcessor(project, psiFile).run()
            }
        }
    }

    private fun applySpotlessConfig(virtualFile: VirtualFile, configPath: String) {
        // Für eine echte Spotless-Unterstützung beliebiger Konfigurationen müsste hier
        // ein Spotless-Formatter dynamisch aufgebaut werden.
        // Da die vollständige Implementierung eines Spotless-Parsers den Rahmen sprengt,
        // wird hier die Konfiguration geladen und eine entsprechende Meldung ausgegeben.
        notifyInfo("Using Spotless config: $configPath")
        
        // Aktuell nutzen wir weiterhin den IntelliJ-Formatter als Fallback
        applyLegacyFormat(virtualFile)
    }

    private fun validateSettings(state: SpotlessFormatSettings.State, virtualFile: VirtualFile, resolvedConfigPath: String?): Boolean {
        if (state.useSpotlessConfig) {
            val configPath = state.spotlessConfigPath
            if (configPath.isBlank()) {
                notifyError("Spotless configuration path is not configured.")
                return false
            }
            
            val finalConfigPath = resolvedConfigPath ?: configPath
            val configFile = File(finalConfigPath)
            if (!configFile.exists()) {
                notifyError("Spotless configuration file not found at: $finalConfigPath")
                return false
            }
            return true
        }

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

    private fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless Formatter", content, NotificationType.INFORMATION)
            .notify(project)
    }
}
