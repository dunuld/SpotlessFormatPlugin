package de.spotlessformatplugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.formatters.EclipseFormatter
import de.spotlessformatplugin.services.formatters.GoogleJavaFormatFormatter
import de.spotlessformatplugin.services.formatters.SpotlessConfigFormatter
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/**
 * Formats files in three phases:
 * 1. take a snapshot of the document (text + modification stamp) in a read action,
 * 2. compute the formatted text in the background (may take a while for large files),
 * 3. apply the result as minimal edits in a write command - but only if the document has not been changed in the
 *    meantime, so nothing the user typed while formatting is lost.
 */
@Service(Service.Level.PROJECT)
class SpotlessRunner(private val project: Project, private val coroutineScope: CoroutineScope) {

    private val notifier = SpotlessNotifier(project)
    private val configResolver = SpotlessConfigResolver()
    private val validator = SpotlessSettingsValidator(project, notifier)
    private val eclipseFormatter = EclipseFormatter(project, notifier)
    private val googleJavaFormatFormatter = GoogleJavaFormatFormatter(notifier)
    private val spotlessConfigFormatter = SpotlessConfigFormatter(notifier)

    /** Immutable view of a document plus everything needed to format it off the EDT. */
    class Snapshot(
        val file: VirtualFile,
        val document: Document,
        val text: String,
        val modificationStamp: Long,
        val settings: SpotlessFormatSettings.State,
        val configPath: String?,
        val javaRelease: Int?
    )

    fun isSupportedFile(file: VirtualFile): Boolean =
        !file.isDirectory && SpotlessFormatSettings.getInstance(project).state.isSupportedExtension(file.extension)

    /** Files formatted on save must belong to this project's content. Requires a read action. */
    fun isEligibleForSave(file: VirtualFile): Boolean =
        isSupportedFile(file) && file.isValid && ProjectFileIndex.getInstance(project).isInContent(file)

    /** Formats the files in the background (used by the reformat action). */
    fun formatFiles(files: List<VirtualFile>) {
        if (files.isEmpty()) return
        if (ApplicationManager.getApplication().isUnitTestMode) {
            files.forEach(::formatSynchronously)
            return
        }
        coroutineScope.launch {
            withBackgroundProgress(project, SpotlessBundle.message("progress.title")) {
                for (file in files) {
                    formatInBackground(file)
                }
            }
        }
    }

    fun formatFile(file: VirtualFile) = formatFiles(listOf(file))

    /** Suspending variant used by actions on save. Must not be called on the EDT. */
    suspend fun formatInBackground(file: VirtualFile): Boolean {
        val snapshot = readAction { createSnapshot(file) } ?: return false
        val formatted = runInterruptible(Dispatchers.IO) { computeFormattedText(snapshot) } ?: return false
        return withContext(Dispatchers.EDT) { applyInWriteCommand(snapshot, formatted) }
    }

    /** Blocking variant for tests and callers that are already allowed to block. */
    fun formatSynchronously(file: VirtualFile): Boolean {
        val snapshot = ApplicationManager.getApplication().runReadAction<Snapshot?> { createSnapshot(file) } ?: return false
        val formatted = computeFormattedText(snapshot) ?: return false
        return applyInWriteCommand(snapshot, formatted)
    }

    private fun applyInWriteCommand(snapshot: Snapshot, formatted: String): Boolean =
        WriteCommandAction.writeCommandAction(project)
            .withName(SpotlessBundle.message("command.name"))
            .compute<Boolean, RuntimeException> { applyIfUnchanged(snapshot, formatted) }

    internal fun createSnapshot(file: VirtualFile): Snapshot? {
        if (!file.isValid || file.isDirectory) return null
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val settings = SpotlessFormatSettings.getInstance(project).state.copy()
        val configPath = configResolver.resolveConfigPath(settings, file, searchBoundary(file), project.basePath)
        return Snapshot(
            file = file,
            document = document,
            text = document.text,
            modificationStamp = document.modificationStamp,
            settings = settings,
            configPath = configPath,
            javaRelease = javaRelease(file)
        )
    }

    internal fun computeFormattedText(snapshot: Snapshot): String? {
        val settings = snapshot.settings
        val file = snapshot.file
        if (!validator.validate(settings, file, snapshot.configPath)) return null

        return if (settings.useSpotlessConfig) {
            spotlessConfigFormatter.format(file, snapshot.text, snapshot.configPath)
        } else {
            when (settings.formatterType) {
                SpotlessFormatSettings.FormatterType.ECLIPSE ->
                    eclipseFormatter.format(file, snapshot.text, settings, snapshot.javaRelease)
                SpotlessFormatSettings.FormatterType.GOOGLE_JAVA_FORMAT ->
                    googleJavaFormatFormatter.format(file, snapshot.text, settings)
            }
        }
    }

    /** Must be called inside a write command. */
    internal fun applyIfUnchanged(snapshot: Snapshot, formatted: String): Boolean {
        val document = snapshot.document
        if (document.modificationStamp != snapshot.modificationStamp) {
            notifier.notifyWarning(SpotlessBundle.message("notification.document.changed", snapshot.file.name))
            return false
        }
        if (!document.isWritable) {
            notifier.notifyWarning(SpotlessBundle.message("notification.document.read.only", snapshot.file.name))
            return false
        }
        DocumentUpdater.applyMinimalChanges(document, formatted)
        PsiDocumentManager.getInstance(project).commitDocument(document)
        return true
    }

    /** Search boundary for relative config paths: the project root, or the file's content root for external files. */
    private fun searchBoundary(file: VirtualFile): VirtualFile? {
        val projectDir = project.guessProjectDir()
        if (projectDir != null && VfsUtilCore.isAncestor(projectDir, file, false)) return projectDir
        return ProjectFileIndex.getInstance(project).getContentRootForFile(file)
    }

    private fun javaRelease(file: VirtualFile): Int? {
        if (!file.extension.equals("java", ignoreCase = true)) return null
        val level = (PsiManager.getInstance(project).findFile(file) as? PsiJavaFile)?.languageLevel
            ?: LanguageLevelProjectExtension.getInstance(project).languageLevel
        return level.toJavaVersion().feature
    }
}
