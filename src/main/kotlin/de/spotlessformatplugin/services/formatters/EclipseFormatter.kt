package de.spotlessformatplugin.services.formatters

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class EclipseFormatter(
    private val project: Project,
    private val notifier: SpotlessNotifier
) {

    fun format(virtualFile: VirtualFile, settings: SpotlessFormatSettings.State) {
        notifier.notifyInfo("Applying Eclipse Formatter using: ${settings.formatterXmlPath}")
        // Currently we use the IntelliJ-Formatter as Fallback/Mock
        applyLegacyFormat(virtualFile)
    }

    private fun applyLegacyFormat(virtualFile: VirtualFile) {
        val application = ApplicationManager.getApplication()
        val runnable = Runnable {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@Runnable
            WriteCommandAction.runWriteCommandAction(project, "Spotless Formatting", null, {
                CodeStyleManager.getInstance(project).reformat(psiFile)
                if (virtualFile.extension.equals("java", ignoreCase = true)) {
                    OptimizeImportsProcessor(project, psiFile).run()
                }
            })
        }

        if (application.isDispatchThread) {
            runnable.run()
        } else {
            application.invokeAndWait(runnable)
        }
    }
}
