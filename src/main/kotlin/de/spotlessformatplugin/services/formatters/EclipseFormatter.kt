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
                        val options = parseEclipseSettings(xmlFile)
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

    private fun parseEclipseSettings(file: File): Map<String, String> {
        if (!file.exists() || file.length() == 0L) {
            return emptyMap()
        }
        val options = mutableMapOf<String, String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {}
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(file)
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
}
