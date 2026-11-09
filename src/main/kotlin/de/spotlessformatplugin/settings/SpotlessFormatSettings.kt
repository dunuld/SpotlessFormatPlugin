package de.spotlessformatplugin.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import de.spotlessformatplugin.SpotlessBundle

@Service(Service.Level.PROJECT)
@State(
    name = "SpotlessFormatSettings",
    storages = [Storage("spotless-format-settings.xml")]
)
class SpotlessFormatSettings : PersistentStateComponent<SpotlessFormatSettings.State> {

    enum class ConfigurationMode {
        FORMATTER,

        /** Generic Spotless steps (trailing whitespace, final newline, optional import order). */
        SPOTLESS;

        override fun toString(): String = when (this) {
            FORMATTER -> SpotlessBundle.message("settings.mode.formatter")
            SPOTLESS -> SpotlessBundle.message("settings.mode.generic")
        }
    }

    enum class FormatterType {
        ECLIPSE,
        PRETTIER,
        GOOGLE_JAVA_FORMAT;

        override fun toString(): String = when (this) {
            ECLIPSE -> SpotlessBundle.message("settings.formatter.eclipse")
            PRETTIER -> SpotlessBundle.message("settings.formatter.prettier")
            GOOGLE_JAVA_FORMAT -> SpotlessBundle.message("settings.formatter.gjf")
        }
    }

    class State {
        var configurationMode: ConfigurationMode = ConfigurationMode.FORMATTER
        var formatterType: FormatterType = FormatterType.ECLIPSE
        var formatterXmlPath: String = ""
        var formatterProfile: String = ""
        var importOrderPath: String = ""
        var prettierConfigPath: String = ""

        /** Remove unused imports and sort imports when using Google Java Format (the google-java-format CLI default). */
        var gjfFixImports: Boolean = true
        var executeOnSave: Boolean = false
        var overrideReformatAction: Boolean = false
        var supportedExtensions: String = DEFAULT_SUPPORTED_EXTENSIONS

        @get:Transient
        var useSpotlessConfig: Boolean
            get() = configurationMode == ConfigurationMode.SPOTLESS
            set(value) {
                configurationMode = if (value) ConfigurationMode.SPOTLESS else ConfigurationMode.FORMATTER
            }

        /**
         * Import order file used by the generic Spotless steps mode. Relative paths are searched upwards from the
         * formatted file (up to the project root). The field name is kept for compatibility with stored settings.
         */
        var spotlessConfigPath: String = ""

        /** Show error and warning notifications. */
        var enableNotifications: Boolean = true

        /** Show a notification after every successful formatting run. */
        var showSuccessNotifications: Boolean = false

        fun supportedExtensionList(): List<String> = parseExtensions(supportedExtensions)

        fun isSupportedExtension(extension: String?): Boolean =
            extension != null && supportedExtensionList().any { it.equals(extension, ignoreCase = true) }

        fun copy(): State = State().also { XmlSerializerUtil.copyBean(this, it) }
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        const val DEFAULT_SUPPORTED_EXTENSIONS = "java,xml,js,ts,json"

        fun getInstance(project: Project): SpotlessFormatSettings = project.getService(SpotlessFormatSettings::class.java)

        fun parseExtensions(value: String): List<String> =
            value.split(',').map { it.trim().removePrefix(".") }.filter { it.isNotEmpty() }
    }
}
