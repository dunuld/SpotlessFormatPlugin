package de.spotlessformatplugin.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "SpotlessFormatSettings",
    storages = [Storage("spotless-format-settings.xml")]
)
class SpotlessFormatSettings : PersistentStateComponent<SpotlessFormatSettings.State> {

    class State {
        // Generic path to a formatter configuration file (could be XML or other formats depending on formatterType)
        var formatterConfigPath: String = ""
        // Which formatter to use for the configured formatterConfigPath. Examples: "eclipse", "google-java-format", "custom"
        var formatterType: String = "eclipse"
        // Import order file (kept for compatibility / explicit import ordering)
        var importOrderPath: String = ""
        var executeOnSave: Boolean = false
        var supportedExtensions: String = "java,xml"
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): SpotlessFormatSettings = project.getService(SpotlessFormatSettings::class.java)
    }
}
