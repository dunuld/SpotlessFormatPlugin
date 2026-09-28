package de.spotlessformatplugin.actionsonsave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider
import com.intellij.ui.components.ActionLink
import de.spotlessformatplugin.settings.SpotlessFormatConfigurable
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessActionOnSaveInfoProvider : ActionOnSaveInfoProvider() {
    public override fun getActionOnSaveInfos(context: ActionOnSaveContext): Collection<ActionOnSaveInfo> {
        return listOf(SpotlessActionOnSaveInfo(context))
    }
}

class SpotlessActionOnSaveInfo(actionOnSaveContext: ActionOnSaveContext) : ActionOnSaveInfo(actionOnSaveContext) {

    private var inMemoryEnabled: Boolean? = null

    private fun getConfigurable(): SpotlessFormatConfigurable? {
        return context.settings.find(SpotlessFormatConfigurable::class.java)
    }

    override fun getActionOnSaveName(): String = "Format changed files with Spotless"

    override fun isActionOnSaveEnabled(): Boolean {
        val configurable = getConfigurable()
        return configurable?.isExecuteOnSave
            ?: inMemoryEnabled
            ?: SpotlessFormatSettings.getInstance(project).state.executeOnSave
    }

    override fun setActionOnSaveEnabled(enabled: Boolean) {
        inMemoryEnabled = enabled
        getConfigurable()?.isExecuteOnSave = enabled
    }

    public override fun isModified(): Boolean {
        val currentEnabled = isActionOnSaveEnabled
        val persistedEnabled = SpotlessFormatSettings.getInstance(project).state.executeOnSave
        return currentEnabled != persistedEnabled
    }

    public override fun apply() {
        val newEnabled = isActionOnSaveEnabled
        SpotlessFormatSettings.getInstance(project).state.executeOnSave = newEnabled
        getConfigurable()?.let {
            it.isExecuteOnSave = newEnabled
        }
        inMemoryEnabled = null
    }

    override fun getActionLinks(): List<ActionLink> {
        return listOfNotNull(createGoToPageInSettingsLink(SpotlessFormatConfigurable.CONFIGURABLE_ID))
    }
}
