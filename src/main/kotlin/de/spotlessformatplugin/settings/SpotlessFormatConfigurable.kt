package de.spotlessformatplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

class SpotlessFormatConfigurable(private val project: Project) : Configurable {

    private val settings = SpotlessFormatSettings.getInstance(project)
    
    private var formatterXmlField: TextFieldWithBrowseButton? = null
    private var importOrderField: TextFieldWithBrowseButton? = null
    private var supportedExtensionsField: JTextField? = null
    private var executeOnSaveCheckBox: JCheckBox? = null

    override fun getDisplayName(): String = "Spotless Formatter"

    override fun createComponent(): JComponent {
        return panel {
            row("Formatter XML:") {
                formatterXmlField = textFieldWithBrowseButton(
                    project = project,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml").withTitle("Select Formatter XML")
                ).comment("Eclipse Formatter XML used for Java and XML files").validationOnInput {
                    if (it.text.isNotEmpty() && !File(it.text).exists()) {
                        error("File does not exist")
                    } else null
                }.component
            }
            row("Import Order File:") {
                importOrderField = textFieldWithBrowseButton(
                    project = project,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Import Order File")
                ).validationOnInput {
                    if (it.text.isNotEmpty() && !File(it.text).exists()) {
                        error("File does not exist")
                    } else null
                }.component
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

    override fun isModified(): Boolean {
        val state = settings.state
        return formatterXmlField?.text != state.formatterXmlPath ||
                importOrderField?.text != state.importOrderPath ||
                supportedExtensionsField?.text != state.supportedExtensions ||
                executeOnSaveCheckBox?.isSelected != state.executeOnSave
    }

    override fun apply() {
        val state = settings.state
        state.formatterXmlPath = formatterXmlField?.text ?: ""
        state.importOrderPath = importOrderField?.text ?: ""
        state.supportedExtensions = supportedExtensionsField?.text ?: "java,xml"
        state.executeOnSave = executeOnSaveCheckBox?.isSelected ?: false
    }

    override fun reset() {
        val state = settings.state
        formatterXmlField?.text = state.formatterXmlPath
        importOrderField?.text = state.importOrderPath
        supportedExtensionsField?.text = state.supportedExtensions
        executeOnSaveCheckBox?.isSelected = state.executeOnSave
    }
}
