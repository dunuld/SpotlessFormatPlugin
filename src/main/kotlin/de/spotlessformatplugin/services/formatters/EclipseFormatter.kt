package de.spotlessformatplugin.services.formatters

import com.diffplug.spotless.Formatter as SpotlessFormatter
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.generic.EndWithNewlineStep
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep
import com.diffplug.spotless.java.ImportOrderStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import org.eclipse.jface.text.Document
import java.io.File
import java.util.Properties
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class EclipseFormatter(
    private val documentTextService: DocumentTextService,
    private val notifier: SpotlessNotifier
) {
    constructor(project: Project, notifier: SpotlessNotifier) : this(DocumentTextService(project), notifier)

    fun format(virtualFile: VirtualFile, settings: SpotlessFormatSettings.State) {
        val text = documentTextService.getFileText(virtualFile) ?: return
        val extension = virtualFile.extension ?: ""

        try {
            val steps = mutableListOf<FormatterStep>()

            if (extension.equals("java", ignoreCase = true)) {
                val xmlPath = settings.formatterXmlPath
                if (xmlPath.isNotBlank()) {
                    val xmlFile = File(xmlPath)
                    if (xmlFile.exists()) {
                        val options = parseEclipseSettings(xmlFile, settings.formatterProfile)
                        steps.add(EclipseJdtStep(options))
                    }
                }

                val importOrderPath = settings.importOrderPath
                if (importOrderPath.isNotBlank()) {
                    val importOrderFile = File(importOrderPath)
                    if (importOrderFile.exists()) {
                        steps.add(ImportOrderStep.forJava().createFrom(importOrderFile))
                    }
                }
            }

            steps.add(TrimTrailingWhitespaceStep.create())
            steps.add(EndWithNewlineStep.create())

            val formatter = SpotlessFormatter.builder()
                .lineEndingsPolicy(LineEnding.PLATFORM_NATIVE.createPolicy())
                .encoding(Charsets.UTF_8)
                .steps(steps)
                .build()

            val formatted = formatter.compute(text, File(virtualFile.path))
            documentTextService.updateDocumentText(virtualFile, formatted, "Spotless Formatting")
            notifier.notifyInfo("Applying Eclipse Formatter using: ${settings.formatterXmlPath}")
        } catch (e: Exception) {
            notifier.notifyError("Spotless formatting failed for ${virtualFile.name}: ${e.message}")
        }
    }

    private class EclipseJdtStep(private val options: Map<String, String>) : FormatterStep {
        override fun getName(): String = "eclipse jdt formatter"

        override fun format(rawUnix: String, file: File): String {
            return formatWithEclipseJdt(rawUnix, options)
        }

        override fun close() {}

        private fun formatWithEclipseJdt(rawText: String, options: Map<String, String>): String {
            val formatterOptions = HashMap<String, String>()
            val defaultSettings = DefaultCodeFormatterConstants.getEclipseDefaultSettings()
            if (defaultSettings != null) {
                for ((key, value) in defaultSettings) {
                    if (key != null && value != null) {
                        formatterOptions[key] = value
                    }
                }
            }
            formatterOptions.putAll(options)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_17)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_17)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_17)

            val codeFormatter = ToolFactory.createCodeFormatter(formatterOptions)
            val textEdit = codeFormatter.format(
                CodeFormatter.K_COMPILATION_UNIT or CodeFormatter.F_INCLUDE_COMMENTS,
                rawText,
                0,
                rawText.length,
                0,
                null
            ) ?: return rawText

            val document = Document(rawText)
            textEdit.apply(document)
            return document.get()
        }
    }

    companion object {
        fun getProfileNames(file: File): List<String> {
            if (!file.exists() || file.length() == 0L) {
                return emptyList()
            }
            val profileNames = mutableListOf<String>()
            try {
                val doc = parseXmlDocument(file) ?: return emptyList()
                val profiles = doc.getElementsByTagName("profile")
                for (i in 0 until profiles.length) {
                    val node = profiles.item(i)
                    val name = node.attributes?.getNamedItem("name")?.nodeValue
                    if (!name.isNullOrBlank()) {
                        profileNames.add(name)
                    }
                }
            } catch (_: Exception) {}
            return profileNames
        }

        fun parseEclipseSettings(file: File, profileName: String? = null): Map<String, String> {
            if (!file.exists() || file.length() == 0L) {
                return emptyMap()
            }
            val options = mutableMapOf<String, String>()
            try {
                val doc = parseXmlDocument(file)
                if (doc != null) {
                    val profiles = doc.getElementsByTagName("profile")
                    if (profiles.length > 0) {
                        var selectedProfileNode: org.w3c.dom.Node? = null
                        if (!profileName.isNullOrBlank()) {
                            for (i in 0 until profiles.length) {
                                val node = profiles.item(i)
                                val name = node.attributes?.getNamedItem("name")?.nodeValue
                                if (name == profileName) {
                                selectedProfileNode = node
                                break
                            }
                        }
                    }
                    if (selectedProfileNode == null) {
                        selectedProfileNode = profiles.item(0)
                    }

                    if (selectedProfileNode is Element) {
                        val settings = selectedProfileNode.getElementsByTagName("setting")
                        for (i in 0 until settings.length) {
                            val node = settings.item(i)
                            val attributes = node.attributes
                            val id = attributes?.getNamedItem("id")?.nodeValue
                            val value = attributes?.getNamedItem("value")?.nodeValue
                            if (id != null && value != null) {
                                options[id] = value
                            }
                        }
                    }
                    return options
                } else {
                    val settings = doc.getElementsByTagName("setting")
                    for (i in 0 until settings.length) {
                        val node = settings.item(i)
                        val attributes = node.attributes
                        val id = attributes?.getNamedItem("id")?.nodeValue
                        val value = attributes?.getNamedItem("value")?.nodeValue
                        if (id != null && value != null) {
                            options[id] = value
                        }
                    }
                    return options
                }
            }
        } catch (_: Exception) {
            try {
                val properties = Properties()
                file.inputStream().use { properties.load(it) }
                for ((key, value) in properties) {
                    if (key != null && value != null) {
                        options[key.toString()] = value.toString()
                    }
                }
            } catch (_: Exception) {}
        }
        return options
    }

    private fun parseXmlDocument(file: File): org.w3c.dom.Document? {
        val factory = DocumentBuilderFactory.newInstance()
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (_: Exception) {}
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }
}
}
