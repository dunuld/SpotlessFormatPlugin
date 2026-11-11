package de.spotlessformatplugin.actionsonsave

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.SpotlessRunner
import de.spotlessformatplugin.settings.SpotlessFormatSettings

/**
 * Formats documents when they are saved explicitly ("Actions on Save"). The platform runs this in the background,
 * only for documents of this project, and saves the documents again after they have been updated - so the
 * formatted content is what ends up on disk.
 */
class SpotlessActionOnSave : ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave() {

    override val presentableName: String
        get() = SpotlessBundle.message("actions.on.save.presentable.name")

    override fun isEnabledForProject(project: Project): Boolean =
        SpotlessFormatSettings.getInstance(project).state.executeOnSave

    override suspend fun updateDocument(project: Project, document: Document) {
        val runner = project.service<SpotlessRunner>()
        val file = readAction {
            FileDocumentManager.getInstance().getFile(document)?.takeIf { runner.isEligibleForSave(it) }
        } ?: return
        runner.formatInBackground(file)
    }
}
