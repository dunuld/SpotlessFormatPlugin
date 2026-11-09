package de.spotlessformatplugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selected
import de.spotlessformatplugin.SpotlessBundle.message
import de.spotlessformatplugin.services.SpotlessPaths
import de.spotlessformatplugin.services.formatters.EclipseFormatter
import de.spotlessformatplugin.services.formatters.GoogleJavaFormatFormatter
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class SpotlessFormatConfigurable(private val project: Project) : SearchableConfigurable {

    companion object {
        const val CONFIGURABLE_ID = "de.spotlessformatplugin.settings.SpotlessFormatConfigurable"
    }

    private val settings = SpotlessFormatSettings.getInstance(project)

    private var configurationModeComboBox: ComboBox<SpotlessFormatSettings.ConfigurationMode>? = null
    private var formatterTypeComboBox: ComboBox<SpotlessFormatSettings.FormatterType>? = null
    private var formatterXmlField: TextFieldWithBrowseButton? = null
    private var formatterProfileComboBox: ComboBox<String>? = null
    private var importOrderField: TextFieldWithBrowseButton? = null
    private var prettierConfigField: TextFieldWithBrowseButton? = null
    private var gjfFixImportsCheckBox: JCheckBox? = null
    private var spotlessConfigField: TextFieldWithBrowseButton? = null
    private var supportedExtensionsField: JTextField? = null
    private var executeOnSaveCheckBox: JCheckBox? = null
    private var overrideReformatCheckBox: JCheckBox? = null
    private var enableNotificationsCheckBox: JCheckBox? = null
    private var showSuccessNotificationsCheckBox: JCheckBox? = null

    private var isUpdatingFromReset = false
    private var pendingExecuteOnSave: Boolean? = null

    var isExecuteOnSave: Boolean
        get() = executeOnSaveCheckBox?.isSelected ?: pendingExecuteOnSave ?: settings.state.executeOnSave
        set(value) {
            val checkBox = executeOnSaveCheckBox
            if (checkBox != null) {
                checkBox.isSelected = value
            } else {
                pendingExecuteOnSave = value
            }
        }

    override fun getId(): String = CONFIGURABLE_ID

    override fun getDisplayName(): String = message("settings.displayName")

    override fun createComponent(): JComponent {
        val rootPanel = panel {
            row {
                executeOnSaveCheckBox = checkBox(message("settings.execute.on.save"))
                    .comment(message("settings.execute.on.save.comment"))
                    .component
            }

            row {
                overrideReformatCheckBox = checkBox(message("settings.override.reformat")).component
            }

            // The formatter settings are needed as soon as Spotless is used in any way.
            val spotlessUsed: ComponentPredicate =
                executeOnSaveCheckBox!!.selected.or(overrideReformatCheckBox!!.selected)

            row(message("settings.mode.label")) {
                configurationModeComboBox = comboBox(SpotlessFormatSettings.ConfigurationMode.entries)
                    .enabledIf(spotlessUsed)
                    .component
            }

            group(message("settings.group.formatter.selection")) {
                row(message("settings.formatter.type.label")) {
                    formatterTypeComboBox = comboBox(SpotlessFormatSettings.FormatterType.entries)
                        .enabledIf(spotlessUsed)
                        .component
                }
            }.visibleIf(createConfigurationModePredicate(SpotlessFormatSettings.ConfigurationMode.FORMATTER))
                .enabledIf(spotlessUsed)

            group(message("settings.group.eclipse")) {
                row(message("settings.eclipse.xml.label")) {
                    formatterXmlField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile()
                            .withExtensionFilter("xml")
                            .withTitle(message("settings.eclipse.xml.chooser.title"))
                    ).comment(message("settings.eclipse.xml.comment"))
                        .validatePath()
                        .enabledIf(spotlessUsed)
                        .component
                }
                row(message("settings.eclipse.profile.label")) {
                    formatterProfileComboBox = comboBox(DefaultComboBoxModel<String>())
                        .comment(message("settings.eclipse.profile.comment"))
                        .enabledIf(spotlessUsed)
                        .component
                }
                row(message("settings.import.order.label")) {
                    importOrderField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile()
                            .withTitle(message("settings.import.order.chooser.title"))
                    ).comment(message("settings.path.comment"))
                        .validatePath()
                        .enabledIf(spotlessUsed)
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.ECLIPSE))
                .enabledIf(spotlessUsed)

            group(message("settings.group.prettier")) {
                row(message("settings.prettier.config.label")) {
                    prettierConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile()
                            .withTitle(message("settings.prettier.config.chooser.title"))
                    ).comment(message("settings.prettier.config.comment"))
                        .validatePath()
                        .enabledIf(spotlessUsed)
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.PRETTIER))
                .enabledIf(spotlessUsed)

            group(message("settings.group.gjf")) {
                row {
                    gjfFixImportsCheckBox = checkBox(message("settings.gjf.fix.imports"))
                        .comment(message("settings.gjf.version.comment", GoogleJavaFormatFormatter.BUNDLED_VERSION))
                        .enabledIf(spotlessUsed)
                        .component
                }
            }.visibleIf(createFormatterTypePredicate(SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT))
                .enabledIf(spotlessUsed)

            group(message("settings.group.generic")) {
                row(message("settings.generic.import.order.label")) {
                    spotlessConfigField = textFieldWithBrowseButton(
                        project = project,
                        fileChooserDescriptor = FileChooserDescriptorFactory.singleFile()
                            .withTitle(message("settings.import.order.chooser.title"))
                    ).comment(message("settings.generic.comment"))
                        .enabledIf(spotlessUsed)
                        .component
                }
            }.visibleIf(createConfigurationModePredicate(SpotlessFormatSettings.ConfigurationMode.SPOTLESS))
                .enabledIf(spotlessUsed)

            row(message("settings.extensions.label")) {
                supportedExtensionsField = textField()
                    .comment(message("settings.extensions.comment"))
                    .enabledIf(spotlessUsed)
                    .component
            }

            row {
                enableNotificationsCheckBox = checkBox(message("settings.notifications.errors"))
                    .enabledIf(spotlessUsed)
                    .component
            }
            row {
                showSuccessNotificationsCheckBox = checkBox(message("settings.notifications.success"))
                    .enabledIf(spotlessUsed)
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

    private fun Cell<TextFieldWithBrowseButton>.validatePath(): Cell<TextFieldWithBrowseButton> =
        validationOnInput {
            val file = SpotlessPaths.resolve(project, it.text)
            if (file != null && !file.isFile) ValidationInfo(message("settings.validation.file.missing"), it) else null
        }

    private fun updateProfileComboBox(selectedProfile: String? = null) {
        val file = SpotlessPaths.resolve(project, formatterXmlField?.text)
        val profiles = if (file != null && file.isFile) EclipseFormatter.getProfileNames(file) else emptyList()

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

    private fun createConfigurationModePredicate(mode: SpotlessFormatSettings.ConfigurationMode): ComponentPredicate {
        return object : ComponentPredicate() {
            override fun invoke(): Boolean = configurationModeComboBox?.selectedItem == mode
            override fun addListener(listener: (Boolean) -> Unit) {
                configurationModeComboBox?.addActionListener { listener(invoke()) }
            }
        }
    }

    private fun createFormatterTypePredicate(type: SpotlessFormatSettings.FormatterType): ComponentPredicate {
        return object : ComponentPredicate() {
            override fun invoke(): Boolean =
                configurationModeComboBox?.selectedItem == SpotlessFormatSettings.ConfigurationMode.FORMATTER &&
                        formatterTypeComboBox?.selectedItem == type

            override fun addListener(listener: (Boolean) -> Unit) {
                configurationModeComboBox?.addActionListener { listener(invoke()) }
                formatterTypeComboBox?.addActionListener { listener(invoke()) }
            }
        }
    }

    override fun isModified(): Boolean {
        val state = settings.state
        val currentProfile = formatterProfileComboBox?.selectedItem as? String ?: ""
        val currentExecuteOnSave = executeOnSaveCheckBox?.isSelected ?: pendingExecuteOnSave ?: state.executeOnSave
        return configurationModeComboBox?.selectedItem != state.configurationMode ||
                formatterTypeComboBox?.selectedItem != state.formatterType ||
                formatterXmlField?.text != state.formatterXmlPath ||
                currentProfile != state.formatterProfile ||
                importOrderField?.text != state.importOrderPath ||
                prettierConfigField?.text != state.prettierConfigPath ||
                gjfFixImportsCheckBox?.isSelected != state.gjfFixImports ||
                spotlessConfigField?.text != state.spotlessConfigPath ||
                supportedExtensionsField?.text != state.supportedExtensions ||
                currentExecuteOnSave != state.executeOnSave ||
                overrideReformatCheckBox?.isSelected != state.overrideReformatAction ||
                enableNotificationsCheckBox?.isSelected != state.enableNotifications ||
                showSuccessNotificationsCheckBox?.isSelected != state.showSuccessNotifications
    }

    override fun apply() {
        val state = settings.state
        val defaults = SpotlessFormatSettings.State()
        state.configurationMode = configurationModeComboBox?.selectedItem as? SpotlessFormatSettings.ConfigurationMode ?: defaults.configurationMode
        state.formatterType = formatterTypeComboBox?.selectedItem as? SpotlessFormatSettings.FormatterType ?: defaults.formatterType
        state.formatterXmlPath = formatterXmlField?.text ?: defaults.formatterXmlPath
        state.formatterProfile = formatterProfileComboBox?.selectedItem as? String ?: defaults.formatterProfile
        state.importOrderPath = importOrderField?.text ?: defaults.importOrderPath
        state.prettierConfigPath = prettierConfigField?.text ?: defaults.prettierConfigPath
        state.gjfFixImports = gjfFixImportsCheckBox?.isSelected ?: defaults.gjfFixImports
        state.spotlessConfigPath = spotlessConfigField?.text ?: defaults.spotlessConfigPath
        state.supportedExtensions = supportedExtensionsField?.text ?: defaults.supportedExtensions
        state.executeOnSave = executeOnSaveCheckBox?.isSelected ?: pendingExecuteOnSave ?: defaults.executeOnSave
        state.overrideReformatAction = overrideReformatCheckBox?.isSelected ?: defaults.overrideReformatAction
        state.enableNotifications = enableNotificationsCheckBox?.isSelected ?: defaults.enableNotifications
        state.showSuccessNotifications = showSuccessNotificationsCheckBox?.isSelected ?: defaults.showSuccessNotifications
        pendingExecuteOnSave = null
    }

    override fun reset() {
        val state = settings.state
        isUpdatingFromReset = true
        try {
            configurationModeComboBox?.selectedItem = state.configurationMode
            formatterTypeComboBox?.selectedItem = state.formatterType
            formatterXmlField?.text = state.formatterXmlPath
            updateProfileComboBox(state.formatterProfile)
            importOrderField?.text = state.importOrderPath
            prettierConfigField?.text = state.prettierConfigPath
            gjfFixImportsCheckBox?.isSelected = state.gjfFixImports
            spotlessConfigField?.text = state.spotlessConfigPath
            supportedExtensionsField?.text = state.supportedExtensions
            executeOnSaveCheckBox?.isSelected = pendingExecuteOnSave ?: state.executeOnSave
            pendingExecuteOnSave = null
            overrideReformatCheckBox?.isSelected = state.overrideReformatAction
            enableNotificationsCheckBox?.isSelected = state.enableNotifications
            showSuccessNotificationsCheckBox?.isSelected = state.showSuccessNotifications
        } finally {
            isUpdatingFromReset = false
        }
    }

    override fun disposeUIResources() {
        pendingExecuteOnSave = null
        executeOnSaveCheckBox = null
        configurationModeComboBox = null
        formatterTypeComboBox = null
        formatterXmlField = null
        formatterProfileComboBox = null
        importOrderField = null
        prettierConfigField = null
        gjfFixImportsCheckBox = null
        spotlessConfigField = null
        supportedExtensionsField = null
        overrideReformatCheckBox = null
        enableNotificationsCheckBox = null
        showSuccessNotificationsCheckBox = null
    }
}
