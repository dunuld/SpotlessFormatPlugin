package de.spotlessformatplugin.actions

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.SpotlessRunner
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps the IDE's Reformat Code action. If the option is enabled and every selected item is a file with a supported
 * extension, the files are formatted with Spotless; otherwise (option off, directories, unsupported files) the
 * original action runs unchanged.
 */
class SpotlessReformatAction(val original: AnAction) : AnAction(
    original.templatePresentation.text?.takeIf { it.isNotBlank() } ?: SpotlessBundle.message("action.reformat.fallback.text"),
    original.templatePresentation.description,
    original.templatePresentation.icon
) {
    override fun getActionUpdateThread() = original.actionUpdateThread

    override fun update(event: AnActionEvent) {
        original.update(event)
        if (spotlessTargets(event) != null) {
            event.presentation.isEnabled = true
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val targets = spotlessTargets(event)
        if (project == null || targets == null) {
            original.actionPerformed(event)
            return
        }
        project.getService(SpotlessRunner::class.java).formatFiles(targets)
    }

    companion object {
        /** Files to format with Spotless, or `null` if the original action should handle the event. */
        fun spotlessTargets(event: AnActionEvent): List<VirtualFile>? {
            val project = event.project ?: return null
            val state = SpotlessFormatSettings.getInstance(project).state
            if (!state.overrideReformatAction) return null

            val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(event.getData(CommonDataKeys.VIRTUAL_FILE))
            if (files.isEmpty()) return null
            if (files.any { it.isDirectory || !state.isSupportedExtension(it.extension) }) return null
            return files
        }
    }
}

/**
 * Installs the wrapper once per application and restores the original action when the plugin is unloaded
 * (application services are disposed before a dynamic plugin unload).
 */
@Service(Service.Level.APP)
class SpotlessReformatActionInstaller : Disposable {
    private var replacedOriginal: AnAction? = null

    fun install() {
        if (replacedOriginal != null) return
        val manager = ActionManager.getInstance()
        val current = manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT) ?: return
        if (current is SpotlessReformatAction) return
        manager.replaceAction(IdeActions.ACTION_EDITOR_REFORMAT, SpotlessReformatAction(current))
        replacedOriginal = current
    }

    override fun dispose() {
        val original = replacedOriginal ?: return
        replacedOriginal = null
        val manager = ActionManager.getInstance()
        if (manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT) is SpotlessReformatAction) {
            manager.replaceAction(IdeActions.ACTION_EDITOR_REFORMAT, original)
        }
    }
}

class SpotlessReformatActionStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) = withContext(Dispatchers.EDT) {
        service<SpotlessReformatActionInstaller>().install()
    }
}
