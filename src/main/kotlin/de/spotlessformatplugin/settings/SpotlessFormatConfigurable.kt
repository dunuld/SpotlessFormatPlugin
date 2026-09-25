package de.spotlessformatplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import de.spotlessformatplugin.services.formatters.EclipseFormatter
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class SpotlessFormatConfigurable(private val project: Project) : Configurable {

    private val settings = SpotlessFormatSettings.getInstance(project)
    
    private var formatterTypeComboBox: ComboBox<SpotlessFormatSettings.FormatterType>? = null
    private var formatterXmlField: TextFieldWithBrowseButton? = null
    private var formatterProfileComboBox: ComboBox<String>? = null
    private var importOrderField: TextFieldWithBrowseButton? = null
    private var prettierConfigField: TextFieldWithBrowseButton? = null
    private var gjfVersionField: JTextField? = null
    private var useSpotlessConfigCheckBox: JCheckBox? = null
    private var spotlessConfigField: TextFieldWithBrowseButton? = null
    private var supportedExtensionsField: JTextField? = null
    private var executeOnSaveCheckBox: JCheckBox? = null
    private var enableNotificationsCheckBox: JCheckBox? = null

    private var isUpdatingFromReset = false

    override fun getDisplayName(): String = "Spotless Formatter"

    override fun createComponent(): JComponent {
        val rootPanel = panel {
            row {
                executeOnSaveCheckBox = checkBox("Execute Spotless on save for changed files").component
            }

            val onSaveSelected = executeOnSaveCheckBox!!.selected

            row {
                useSpotlessConfigCheckBox = checkBox("Use generic Spotless configuration file")
                    .enabledIf(onSaveSelected)
                    .component
            }

            group("Formatter Selection") {
                row("Formatter Type:") {
                    formatterTypeComboBox = comboBox(SpotlessFormatSettings.FormatterType.entries)
                        .enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
            }.enabledIf(onSaveSelected)

            group("Eclipse Formatter") {
                row("Formatter XML:") {
                    formatterXmlField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile().withExtensionFilter("xml").withTitle("Select Formatter XML")
                    ).comment("Eclipse Formatter XML used for Java and XML files").validationOnInput {
                        if (it.text.isNotEmpty() && !File(it.text).exists()) {
                            error("File does not exist")
                        } else null
                    }.enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
                row("Formatter Profile:") {
                    formatterProfileComboBox = comboBox(DefaultComboBoxModel<String>())
                        .comment("Profile from the Eclipse Formatter XML (if available)")
                        .enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
                row("Import Order File:") {
                    importOrderField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile().withTitle("Select Import Order File")
                    ).validationOnInput {
                        if (it.text.isNotEmpty() && !File(it.text).exists()) {
                            error("File does not exist")
                        } else null
                    }.enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.ECLIPSE))
                .enabledIf(onSaveSelected)

            group("Prettier") {
                row("Prettier Config:") {
                    prettierConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile().withTitle("Select Prettier Config")
                    ).comment("Path to .prettierrc or prettier.config.js")
                        .enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.PRETTIER))
                .enabledIf(onSaveSelected)

            group("Google Java Format") {
                row("Version:") {
                    gjfVersionField = textField().comment("e.g. 1.17.0")
                        .columns(COLUMNS_SHORT)
                        .enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected.not()))
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT))
                .enabledIf(onSaveSelected)
            
            group("Spotless Configuration") {
                row("Spotless Config:") {
                    spotlessConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile().withTitle("Select Spotless Configuration File")
                    ).comment("Path to a generic Spotless configuration file (e.g. spotless.gradle). If a relative path is provided, the plugin searches hierarchically upwards from the file being formatted.")
                        .enabledIf(onSaveSelected.and(useSpotlessConfigCheckBox!!.selected))
                        .component
                }
            }.enabledIf(onSaveSelected)

            row("Supported Extensions:") {
                supportedExtensionsField = textField()
                    .comment("Comma-separated list of file extensions (e.g., java,xml,kt)")
                    .enabledIf(onSaveSelected)
                    .component
            }

            row {
                enableNotificationsCheckBox = checkBox("Show notifications")
                    .enabledIf(onSaveSelected)
                    .component
            }
        }

        formatterXmlField?.textField?.document?.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!isUpdatingFromReset) {
                    updateProfileComboBox()
                }
            }
        })

        return rootPanel
    }

    private fun updateProfileComboBox(selectedProfile: String? = null) {
        val xmlPath = formatterXmlField?.text?.trim().orEmpty()
        val profiles = if (xmlPath.isNotEmpty()) {
            val file = File(xmlPath)
            if (file.exists() && file.isFile) {
                EclipseFormatter.getProfileNames(file)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        val comboBox = formatterProfileComboBox ?: return
        val currentSelected = selectedProfile ?: (comboBox.selectedItem as? String)
        comboBox.removeAllItems()
        for (profile in profiles) {
            comboBox.addItem(profile)
        }

        if (profiles.isNotEmpty()) {
            if (currentSelected != null && profiles.contains(currentSelected)) {
                comboBox.selectedItem = currentSelected
            } else {
                comboBox.selectedIndex = 0
            }
        }
    }

    private fun createFormatterTypePredicate(type: SpotlessFormatSettings.FormatterType): com.intellij.ui.layout.ComponentPredicate {
        return object : com.intellij.ui.layout.ComponentPredicate() {
            override fun invoke(): Boolean = formatterTypeComboBox?.selectedItem == type
            override fun addListener(listener: (Boolean) -> Unit) {
                formatterTypeComboBox?.addActionListener { listener(invoke()) }
            }
        }
    }

    override fun isModified(): Boolean {
        val state = settings.state
        val currentProfile = formatterProfileComboBox?.selectedItem as? String ?: ""
        return formatterTypeComboBox?.selectedItem != state.formatterType ||
                formatterXmlField?.text != state.formatterXmlPath ||
                currentProfile != state.formatterProfile ||
                importOrderField?.text != state.importOrderPath ||
                prettierConfigField?.text != state.prettierConfigPath ||
                gjfVersionField?.text != state.gjfVersion ||
                useSpotlessConfigCheckBox?.isSelected != state.useSpotlessConfig ||
                spotlessConfigField?.text != state.spotlessConfigPath ||
                supportedExtensionsField?.text != state.supportedExtensions ||
                executeOnSaveCheckBox?.isSelected != state.executeOnSave ||
                enableNotificationsCheckBox?.isSelected != state.enableNotifications
    }

    override fun apply() {
        val state = settings.state
        state.formatterType = formatterTypeComboBox?.selectedItem as? SpotlessFormatSettings.FormatterType ?: SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = formatterXmlField?.text ?: ""
        state.formatterProfile = formatterProfileComboBox?.selectedItem as? String ?: ""
        state.importOrderPath = importOrderField?.text ?: ""
        state.prettierConfigPath = prettierConfigField?.text ?: ""
        state.gjfVersion = gjfVersionField?.text ?: "1.17.0"
        state.useSpotlessConfig = useSpotlessConfigCheckBox?.isSelected ?: false
        state.spotlessConfigPath = spotlessConfigField?.text ?: ""
        state.supportedExtensions = supportedExtensionsField?.text ?: "java,xml"
        state.executeOnSave = executeOnSaveCheckBox?.isSelected ?: false
        state.enableNotifications = enableNotificationsCheckBox?.isSelected ?: true
    }

    override fun reset() {
        val state = settings.state
        isUpdatingFromReset = true
        try {
            useSpotlessConfigCheckBox?.isSelected = state.useSpotlessConfig
            formatterTypeComboBox?.selectedItem = state.formatterType
            formatterXmlField?.text = state.formatterXmlPath
            updateProfileComboBox(state.formatterProfile)
            importOrderField?.text = state.importOrderPath
            prettierConfigField?.text = state.prettierConfigPath
            gjfVersionField?.text = state.gjfVersion
            spotlessConfigField?.text = state.spotlessConfigPath
            supportedExtensionsField?.text = state.supportedExtensions
            executeOnSaveCheckBox?.isSelected = state.executeOnSave
            enableNotificationsCheckBox?.isSelected = state.enableNotifications
        } finally {
            isUpdatingFromReset = false
        }
    }
}
