package de.spotlessformatplugin.services.formatters

import com.diffplug.spotless.Formatter as SpotlessFormatter
import com.diffplug.spotless.FormatterStep
import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.generic.EndWithNewlineStep
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep
import com.diffplug.spotless.java.ImportOrderStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.services.SpotlessPaths
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import org.eclipse.jface.text.Document
import org.w3c.dom.Element
import java.io.File
import java.util.Properties
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Formats Java files with the Eclipse JDT formatter and an optional import order. Other file types only get the
 * generic steps (trailing whitespace, final newline) - the Eclipse XML is not applied to them.
 */
class EclipseFormatter(
    private val project: Project,
    private val notifier: SpotlessNotifier
) {

    /**
     * @param javaRelease Java feature release of the file's module (e.g. 21); used as source level for parsing when
     * the formatter configuration does not define one.
     * @return the formatted text or `null` if formatting failed (the user has been notified).
     */
    fun format(
        virtualFile: VirtualFile,
        text: String,
        settings: SpotlessFormatSettings.State,
        javaRelease: Int? = null
    ): String? {
        val isJava = virtualFile.extension.equals("java", ignoreCase = true)

        return try {
            val steps = mutableListOf<FormatterStep>()

            if (isJava) {
                val xmlFile = SpotlessPaths.resolve(project, settings.formatterXmlPath)
                if (xmlFile != null && xmlFile.isFile) {
                    val options = parseEclipseSettings(xmlFile, settings.formatterProfile)
                    steps.add(EclipseJdtStep(options, javaRelease))
                }

                val importOrderFile = SpotlessPaths.resolve(project, settings.importOrderPath)
                if (importOrderFile != null && importOrderFile.isFile) {
                    steps.add(ImportOrderStep.forJava().createFrom(importOrderFile))
                }
            }

            steps.add(TrimTrailingWhitespaceStep.create())
            steps.add(EndWithNewlineStep.create())

            val formatter = SpotlessFormatter.builder()
                .lineEndingsPolicy(LineEnding.UNIX.createPolicy())
                .encoding(Charsets.UTF_8)
                .steps(steps)
                .build()

            val formatted = formatter.use { it.compute(text, File(virtualFile.path)) }
            notifier.notifySuccess(SpotlessBundle.message("notification.success.eclipse", virtualFile.name))
            formatted
        } catch (e: EclipseFormatException) {
            notifier.notifyError(SpotlessBundle.message("notification.eclipse.parse.failed", virtualFile.name, e.sourceLevel))
            null
        } catch (e: Exception) {
            notifier.notifyError(SpotlessBundle.message("notification.formatting.failed", virtualFile.name, e.message ?: e.javaClass.simpleName))
            null
        }
    }

    private class EclipseFormatException(val sourceLevel: String) : RuntimeException()

    private class EclipseJdtStep(
        private val options: Map<String, String>,
        private val javaRelease: Int?
    ) : FormatterStep {
        override fun getName(): String = "eclipse jdt formatter"

        override fun format(rawUnix: String, file: File): String {
            val formatterOptions = HashMap<String, String>()
            DefaultCodeFormatterConstants.getEclipseDefaultSettings()?.forEach { (key, value) ->
                if (key != null && value != null) formatterOptions[key] = value
            }
            formatterOptions.putAll(options)

            val sourceLevel = javaSourceLevel(javaRelease)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_SOURCE, sourceLevel)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_COMPLIANCE, sourceLevel)
            formatterOptions.putIfAbsent(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, sourceLevel)

            val codeFormatter = ToolFactory.createCodeFormatter(formatterOptions)
            val textEdit = codeFormatter.format(
                CodeFormatter.K_COMPILATION_UNIT or CodeFormatter.F_INCLUDE_COMMENTS,
                rawUnix,
                0,
                rawUnix.length,
                0,
                "\n"
            ) ?: throw EclipseFormatException(formatterOptions[JavaCore.COMPILER_SOURCE] ?: sourceLevel)

            val document = Document(rawUnix)
            textEdit.apply(document)
            return document.get()
        }

        override fun close() {}
    }

    companion object {
        private const val FALLBACK_JAVA_RELEASE = 17

        /** Maps a Java feature release to an Eclipse source level, capped at the newest level JDT supports. */
        fun javaSourceLevel(javaRelease: Int?): String {
            val latest = JavaCore.latestSupportedJavaVersion()
            val latestRelease = latest.removePrefix("1.").toIntOrNull() ?: FALLBACK_JAVA_RELEASE
            val release = (javaRelease ?: FALLBACK_JAVA_RELEASE).coerceAtMost(latestRelease)
            return if (release <= 8) "1.$release" else release.toString()
        }

        fun getProfileNames(file: File): List<String> {
            if (!file.isFile || file.length() == 0L) return emptyList()
            return try {
                val doc = parseXmlDocument(file)
                val profiles = doc.getElementsByTagName("profile")
                (0 until profiles.length).mapNotNull { i ->
                    profiles.item(i).attributes?.getNamedItem("name")?.nodeValue?.takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun parseEclipseSettings(file: File, profileName: String? = null): Map<String, String> {
            if (!file.isFile || file.length() == 0L) return emptyMap()
            return try {
                parseXmlSettings(file, profileName)
            } catch (_: Exception) {
                parsePropertiesSettings(file)
            }
        }

        private fun parseXmlSettings(file: File, profileName: String?): Map<String, String> {
            val doc = parseXmlDocument(file)
            val profiles = doc.getElementsByTagName("profile")
            val scope: Element = if (profiles.length > 0) {
                val named = if (!profileName.isNullOrBlank()) {
                    (0 until profiles.length).map { profiles.item(it) }
                        .firstOrNull { it.attributes?.getNamedItem("name")?.nodeValue == profileName }
                } else {
                    null
                }
                (named ?: profiles.item(0)) as? Element ?: return emptyMap()
            } else {
                doc.documentElement
            }

            val options = mutableMapOf<String, String>()
            val settings = scope.getElementsByTagName("setting")
            for (i in 0 until settings.length) {
                val attributes = settings.item(i).attributes
                val id = attributes?.getNamedItem("id")?.nodeValue
                val value = attributes?.getNamedItem("value")?.nodeValue
                if (id != null && value != null) options[id] = value
            }
            return options
        }

        private fun parsePropertiesSettings(file: File): Map<String, String> = try {
            val properties = Properties()
            file.inputStream().use { properties.load(it) }
            properties.entries.associate { (key, value) -> key.toString() to value.toString() }
        } catch (_: Exception) {
            emptyMap()
        }

        private fun parseXmlDocument(file: File): org.w3c.dom.Document {
            val factory = DocumentBuilderFactory.newInstance()
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {
            }
            val builder = factory.newDocumentBuilder()
            builder.setErrorHandler(null)
            return builder.parse(file)
        }
    }
}
