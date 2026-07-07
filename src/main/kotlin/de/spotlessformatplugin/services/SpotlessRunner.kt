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
import de.spotlessformatplugin.format.FormatterUtil
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class SpotlessRunner(private val project: Project) {

    fun formatFile(virtualFile: VirtualFile) {
        formatFiles(listOf(virtualFile))
    }

    fun formatFiles(virtualFiles: Collection<VirtualFile>) {
        val settings = SpotlessFormatSettings.getInstance(project).state
        if (!validateSettings(settings)) return

        val formatterType = settings.formatterType
        val formatterConfig = settings.formatterConfigPath

        // Detect build tool (not strictly used for in-process formatting yet)
        val buildTool = detectBuildTool()

        for (virtualFile in virtualFiles) {
            try {
                val extension = virtualFile.extension ?: ""
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

                // Read current content
                val inputStream = virtualFile.inputStream
                val original = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                inputStream.close()

                // Attempt in-process formatting using FormatterUtil
                val formatted = FormatterUtil.formatContent(formatterType, formatterConfig, virtualFile.name, original)

                if (formatted != original) {
                    // Write back only if changed
                    WriteCommandAction.runWriteCommandAction(project) {
                        try {
                            virtualFile.setBinaryContent(formatted.toByteArray(StandardCharsets.UTF_8))
                        } catch (e: Exception) {
                            // fallback: try IntelliJ reformat if writing failed
                            psiFile?.let { CodeStyleManager.getInstance(project).reformat(it) }
                        }

                        if (extension.equals("java", ignoreCase = true)) {
                            psiFile?.let { OptimizeImportsProcessor(project, it).run() }
                        }
                    }
                } else {
                    // If formatter did not change content, fallback to IntelliJ reformat to preserve previous behavior
                    psiFile?.let {
                        WriteCommandAction.runWriteCommandAction(project) {
                            CodeStyleManager.getInstance(project).reformat(it)
                            if (extension.equals("java", ignoreCase = true)) {
                                OptimizeImportsProcessor(project, it).run()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                notifyError("Formatting failed for ${virtualFile.path}: ${e.message}")
            }
        }

        // Optionally, notify user about build tool detection (debug/info)
        if (buildTool != null) {
            notifyInfo("Detected build tool: $buildTool")
        }
    }

    private fun detectBuildTool(): String? {
        val basePath = project.basePath ?: return null
        val pom = File(basePath, "pom.xml")
        if (pom.exists()) return "maven"
        val gradle = File(basePath, "build.gradle")
        val gradleKts = File(basePath, "build.gradle.kts")
        if (gradle.exists() || gradleKts.exists()) return "gradle"
        return null
    }

    private fun validateSettings(state: SpotlessFormatSettings.State): Boolean {
        val formatterPath = state.formatterConfigPath
        val formatterType = state.formatterType

        if (formatterType.isBlank()) {
            notifyError("Formatter type is not configured. Please select a formatter type in settings.")
            return false
        }

        if (formatterPath.isBlank()) {
            // Some formatter types may not require a config file; warn only for types that do (we keep it simple here)
            // We'll allow blank for e.g. google-java-format which has no config file in many setups.
            if (formatterType.equals("eclipse", ignoreCase = true) || formatterType.equals("custom", ignoreCase = true)) {
                notifyError("Formatter configuration path is not configured but is required for the selected formatter: $formatterType")
                return false
            }
        } else {
            val formatterFile = File(formatterPath)
            if (!formatterFile.exists()) {
                notifyError("Formatter config file not found at: $formatterPath")
                return false
            }
            if (!formatterFile.canRead()) {
                notifyError("Formatter config file is not readable at: $formatterPath")
                return false
            }
        }

        // Import order file is optional; if set, validate readability
        if (state.importOrderPath.isNotBlank()) {
            val importFile = File(state.importOrderPath)
            if (!importFile.exists()) {
                notifyError("Import order file not found at: ${state.importOrderPath}")
                return false
            }
            if (!importFile.canRead()) {
                notifyError("Import order file is not readable at: ${state.importOrderPath}")
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
