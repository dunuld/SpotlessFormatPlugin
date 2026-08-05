package de.spotlessformatplugin.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "SpotlessFormatSettings",
    storages = [Storage("spotless-format-settings.xml")]
)
class SpotlessFormatSettings : PersistentStateComponent<SpotlessFormatSettings.State> {

    enum class FormatterType {
        ECLIPSE,
        PRETTIER,
        GOOGLE_JAVA_FORMAT
    }

    class State {
        var formatterType: FormatterType = FormatterType.ECLIPSE
        var formatterXmlPath: String = ""
        var importOrderPath: String = ""
        var prettierConfigPath: String = ""
        var gjfVersion: String = "1.17.0"
        var executeOnSave: Boolean = false
        var supportedExtensions: String = "java,xml,js,ts,json"
        var useSpotlessConfig: Boolean = false
        var spotlessConfigPath: String = ""
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
