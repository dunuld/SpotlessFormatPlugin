package de.spotlessformatplugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class DocumentTextService(private val project: Project) {

    fun getFileText(virtualFile: VirtualFile): String? {
        val application = ApplicationManager.getApplication()
        var text: String? = null
        val runnable = Runnable {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            val document = psiFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) }
            text = document?.text ?: psiFile?.text ?: String(virtualFile.contentsToByteArray(), virtualFile.charset)
        }
        if (application.isDispatchThread) {
            runnable.run()
        } else {
            application.invokeAndWait(runnable)
        }
        return text
    }

    fun updateDocumentText(virtualFile: VirtualFile, newText: String, commandName: String) {
        val application = ApplicationManager.getApplication()
        val runnable = Runnable {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@Runnable
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@Runnable
            if (document.text != newText) {
                WriteCommandAction.runWriteCommandAction(project, commandName, null, {
                    document.setText(newText)
                    PsiDocumentManager.getInstance(project).commitDocument(document)
                })
            }
        }

        if (application.isDispatchThread) {
            runnable.run()
        } else {
            application.invokeAndWait(runnable)
        }
    }
}
