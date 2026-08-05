package de.spotlessformatplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

class SpotlessFormatConfigurable(private val project: Project) : Configurable {

    private val settings = SpotlessFormatSettings.getInstance(project)
    
    private var formatterTypeComboBox: ComboBox<SpotlessFormatSettings.FormatterType>? = null
    private var formatterXmlField: TextFieldWithBrowseButton? = null
    private var importOrderField: TextFieldWithBrowseButton? = null
    private var prettierConfigField: TextFieldWithBrowseButton? = null
    private var gjfVersionField: JTextField? = null
    private var useSpotlessConfigCheckBox: JCheckBox? = null
    private var spotlessConfigField: TextFieldWithBrowseButton? = null
    private var supportedExtensionsField: JTextField? = null
    private var executeOnSaveCheckBox: JCheckBox? = null

    override fun getDisplayName(): String = "Spotless Formatter"

    override fun createComponent(): JComponent {
        return panel {
            row {
                useSpotlessConfigCheckBox = checkBox("Use generic Spotless configuration file").component
            }

            group("Formatter Selection") {
                row("Formatter Type:") {
                    formatterTypeComboBox = comboBox(SpotlessFormatSettings.FormatterType.values().toList())
                        .enabledIf(useSpotlessConfigCheckBox!!.selected.not())
                        .component
                }
            }

            group("Eclipse Formatter") {
                row("Formatter XML:") {
                    formatterXmlField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml").withTitle("Select Formatter XML")
                    ).comment("Eclipse Formatter XML used for Java and XML files").validationOnInput {
                        if (it.text.isNotEmpty() && !File(it.text).exists()) {
                            error("File does not exist")
                        } else null
                    }.enabledIf(useSpotlessConfigCheckBox!!.selected.not())
                        .component
                }
                row("Import Order File:") {
                    importOrderField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Import Order File")
                    ).validationOnInput {
                        if (it.text.isNotEmpty() && !File(it.text).exists()) {
                            error("File does not exist")
                        } else null
                    }.enabledIf(useSpotlessConfigCheckBox!!.selected.not())
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.ECLIPSE))

            group("Prettier") {
                row("Prettier Config:") {
                    prettierConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Prettier Config")
                    ).comment("Path to .prettierrc or prettier.config.js")
                        .enabledIf(useSpotlessConfigCheckBox!!.selected.not())
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.PRETTIER))

            group("Google Java Format") {
                row("Version:") {
                    gjfVersionField = textField().comment("e.g. 1.17.0")
                        .columns(COLUMNS_SHORT)
                        .enabledIf(useSpotlessConfigCheckBox!!.selected.not())
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT))
            
            group("Spotless Configuration") {
                row("Spotless Config:") {
                    spotlessConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Spotless Configuration File")
                    ).comment("Path to a generic Spotless configuration file (e.g. spotless.gradle). If a relative path is provided, the plugin searches hierarchically upwards from the file being formatted.").enabledIf(useSpotlessConfigCheckBox!!.selected).component
                }
            }

            row("Supported Extensions:") {
                supportedExtensionsField = textField()
                    .comment("Comma-separated list of file extensions (e.g., java,xml,kt)")
                    .component
            }
            row {
                executeOnSaveCheckBox = checkBox("Execute Spotless on save for changed files").component
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
        return formatterTypeComboBox?.selectedItem != state.formatterType ||
                formatterXmlField?.text != state.formatterXmlPath ||
                importOrderField?.text != state.importOrderPath ||
                prettierConfigField?.text != state.prettierConfigPath ||
                gjfVersionField?.text != state.gjfVersion ||
                useSpotlessConfigCheckBox?.isSelected != state.useSpotlessConfig ||
                spotlessConfigField?.text != state.spotlessConfigPath ||
                supportedExtensionsField?.text != state.supportedExtensions ||
                executeOnSaveCheckBox?.isSelected != state.executeOnSave
    }

    override fun apply() {
        val state = settings.state
        state.formatterType = formatterTypeComboBox?.selectedItem as? SpotlessFormatSettings.FormatterType ?: SpotlessFormatSettings.FormatterType.ECLIPSE
        state.formatterXmlPath = formatterXmlField?.text ?: ""
        state.importOrderPath = importOrderField?.text ?: ""
        state.prettierConfigPath = prettierConfigField?.text ?: ""
        state.gjfVersion = gjfVersionField?.text ?: "1.17.0"
        state.useSpotlessConfig = useSpotlessConfigCheckBox?.isSelected ?: false
        state.spotlessConfigPath = spotlessConfigField?.text ?: ""
        state.supportedExtensions = supportedExtensionsField?.text ?: "java,xml"
        state.executeOnSave = executeOnSaveCheckBox?.isSelected ?: false
    }

    override fun reset() {
        val state = settings.state
        useSpotlessConfigCheckBox?.isSelected = state.useSpotlessConfig
        formatterTypeComboBox?.selectedItem = state.formatterType
        formatterXmlField?.text = state.formatterXmlPath
        importOrderField?.text = state.importOrderPath
        prettierConfigField?.text = state.prettierConfigPath
        gjfVersionField?.text = state.gjfVersion
        spotlessConfigField?.text = state.spotlessConfigPath
        supportedExtensionsField?.text = state.supportedExtensions
        executeOnSaveCheckBox?.isSelected = state.executeOnSave
    }
}
