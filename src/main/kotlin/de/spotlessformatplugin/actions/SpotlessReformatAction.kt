package de.spotlessformatplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.spotlessformatplugin.services.SpotlessRunner
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** Replaces the IDE's Reformat action while retaining its normal behavior when the option is off. */
class SpotlessReformatAction(private val original: AnAction) : AnAction(
    original.templatePresentation.text?.takeIf { it.isNotBlank() } ?: "Reformat Code",
    original.templatePresentation.description,
    original.templatePresentation.icon
) {
    override fun getActionUpdateThread() = original.actionUpdateThread

    override fun update(event: AnActionEvent) {
        original.update(event)
        if (shouldUseSpotless(event)) {
            event.presentation.isEnabled = event.getData(CommonDataKeys.VIRTUAL_FILE) != null
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        if (!shouldUseSpotless(event)) {
            original.actionPerformed(event)
            return
        }
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        project.getService(SpotlessRunner::class.java).formatFile(file)
    }

    private fun shouldUseSpotless(event: AnActionEvent): Boolean {
        val project = event.project ?: return false
        val state = SpotlessFormatSettings.getInstance(project).state
        if (!state.overrideReformatAction) return false
        val extension = event.getData(CommonDataKeys.VIRTUAL_FILE)?.extension ?: return false
        return state.supportedExtensions.split(',').any { it.trim().equals(extension, ignoreCase = true) }
    }
}

class SpotlessReformatActionInstaller : ProjectActivity {
    override suspend fun execute(project: Project) = withContext(Dispatchers.EDT) {
        val manager = ActionManager.getInstance()
        if (installed.compareAndSet(false, true)) {
            val original = manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT)
            if (original != null && original !is SpotlessReformatAction) {
                manager.replaceAction(IdeActions.ACTION_EDITOR_REFORMAT, SpotlessReformatAction(original))
            } else {
                installed.set(false)
            }
        }
    }

    companion object {
        private val installed = AtomicBoolean(false)
    }
}
