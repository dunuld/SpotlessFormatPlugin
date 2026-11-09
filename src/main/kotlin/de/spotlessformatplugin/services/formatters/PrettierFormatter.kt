package de.spotlessformatplugin.services.formatters

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.services.SpotlessPaths
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Formats files with an external Prettier installation (project `node_modules`, global installation or `npx`).
 *
 * Prettier executes code from the project (local binaries, `prettier.config.js`, plugins), so it is never run in
 * projects that are not trusted. `npx` is invoked with `--no-install` so nothing is downloaded implicitly.
 */
class PrettierFormatter internal constructor(
    private val project: Project,
    private val notifier: SpotlessNotifier,
    private val isProjectTrusted: () -> Boolean,
    private val additionalSearchDirs: List<File>,
    private val timeoutSeconds: Long
) {
    constructor(project: Project, notifier: SpotlessNotifier) : this(
        project,
        notifier,
        { TrustedProjects.isProjectTrusted(project) },
        emptyList(),
        DEFAULT_TIMEOUT_SECONDS
    )

    internal data class PrettierExecutable(
        val command: List<String>,
        val nodeBinDir: String? = null
    )

    /** @return the formatted text (with `\n` line separators) or `null` if formatting failed. */
    fun format(virtualFile: VirtualFile, text: String, settings: SpotlessFormatSettings.State): String? {
        if (!isProjectTrusted()) {
            notifier.notifyWarning(SpotlessBundle.message("notification.prettier.untrusted"))
            return null
        }
        val configFile = SpotlessPaths.resolve(project, settings.prettierConfigPath)
        val formatted = runPrettier(virtualFile, text, configFile) ?: return null
        notifier.notifySuccess(SpotlessBundle.message("notification.success.prettier", virtualFile.name))
        return StringUtil.convertLineSeparators(formatted)
    }

    internal fun findPrettierExecutable(): PrettierExecutable? {
        val isWindows = SystemInfo.isWindows
        val projectBase = project.basePath

        // 1. Project-local installation
        if (projectBase != null) {
            val localPrettier = File(projectBase, if (isWindows) "node_modules/.bin/prettier.cmd" else "node_modules/.bin/prettier")
            if (isExecutable(localPrettier, isWindows)) {
                return PrettierExecutable(listOf(localPrettier.absolutePath))
            }
        }

        val candidateDirs = candidateNodeDirs()

        // 2. Global 'prettier' binary
        val prettierName = if (isWindows) "prettier.cmd" else "prettier"
        candidateDirs.firstNotNullOfOrNull { dir ->
            File(dir, prettierName).takeIf { isExecutable(it, isWindows) }
                ?.let { PrettierExecutable(listOf(it.absolutePath), dir.absolutePath) }
        }?.let { return it }

        // 3. 'npx' - never install packages implicitly
        val npxName = if (isWindows) "npx.cmd" else "npx"
        return candidateDirs.firstNotNullOfOrNull { dir ->
            File(dir, npxName).takeIf { isExecutable(it, isWindows) }
                ?.let { PrettierExecutable(listOf(it.absolutePath, "--no-install", "prettier"), dir.absolutePath) }
        }
    }

    private fun candidateNodeDirs(): List<File> {
        val dirs = mutableListOf<File>()
        dirs.addAll(additionalSearchDirs)
        (System.getenv("PATH") ?: "").split(File.pathSeparator).filter { it.isNotBlank() }.forEach { dirs.add(File(it)) }

        val userHome = System.getProperty("user.home") ?: ""
        if (SystemInfo.isWindows) {
            System.getenv("APPDATA")?.let { dirs.add(File(it, "npm")) }
            System.getenv("ProgramFiles")?.let { dirs.add(File(it, "nodejs")) }
            dirs.add(File(userHome, ".volta/bin"))
        } else {
            listOf(
                "/opt/homebrew/bin",
                "/usr/local/bin",
                "/usr/bin",
                "/bin",
                "$userHome/.volta/bin",
                "$userHome/.asdf/shims",
                "$userHome/.nodenv/shims"
            ).forEach { dirs.add(File(it)) }

            val nvmNodeDir = File(userHome, ".nvm/versions/node")
            nvmNodeDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }?.forEach { versionDir ->
                File(versionDir, "bin").takeIf { it.isDirectory }?.let { dirs.add(it) }
            }
        }
        return dirs.distinct()
    }

    private fun isExecutable(file: File, isWindows: Boolean): Boolean =
        file.isFile && (isWindows || file.canExecute())

    private fun runPrettier(virtualFile: VirtualFile, content: String, configFile: File?): String? {
        val prettierExec = findPrettierExecutable()
        if (prettierExec == null) {
            notifier.notifyError(SpotlessBundle.message("notification.prettier.not.found"))
            return null
        }

        val command = prettierExec.command.toMutableList()
        command.add("--stdin-filepath")
        command.add(stdinFilePath(virtualFile))
        if (configFile != null && configFile.isFile) {
            command.add("--config")
            command.add(configFile.absolutePath)
        }

        val processBuilder = ProcessBuilder(command)
        val workingDir = project.basePath?.let(::File)?.takeIf { it.isDirectory }
            ?: virtualFile.parent?.takeIf { it.isInLocalFileSystem }?.let { File(it.path) }
        workingDir?.let { processBuilder.directory(it) }

        prettierExec.nodeBinDir?.let { nodeDir ->
            val env = processBuilder.environment()
            env["PATH"] = prependToPath(nodeDir, env["PATH"] ?: System.getenv("PATH"))
        }

        var process: Process? = null
        return try {
            val started = processBuilder.start()
            process = started

            // Read output concurrently so the process can never block on a full pipe.
            val stdoutFuture = CompletableFuture.supplyAsync { started.inputStream.bufferedReader(Charsets.UTF_8).readText() }
            val stderrFuture = CompletableFuture.supplyAsync { started.errorStream.bufferedReader(Charsets.UTF_8).readText() }

            started.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }

            if (!started.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                started.destroyForcibly()
                notifier.notifyError(SpotlessBundle.message("notification.prettier.timeout", virtualFile.name, timeoutSeconds))
                return null
            }

            val stdout = stdoutFuture.get(5, TimeUnit.SECONDS)
            val stderr = stderrFuture.get(5, TimeUnit.SECONDS)
            if (started.exitValue() == 0) {
                stdout
            } else {
                notifier.notifyError(SpotlessBundle.message("notification.prettier.failed", virtualFile.name, stderr.ifBlank { stdout }.trim()))
                null
            }
        } catch (e: InterruptedException) {
            process?.destroyForcibly()
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            process?.destroyForcibly()
            notifier.notifyError(SpotlessBundle.message("notification.prettier.execution.error", e.message ?: e.javaClass.simpleName))
            null
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 15L

        /** Full path so Prettier resolves `.prettierrc`, `overrides` and `.prettierignore` relative to the file. */
        internal fun stdinFilePath(virtualFile: VirtualFile): String =
            if (virtualFile.isInLocalFileSystem) File(virtualFile.path).absolutePath else virtualFile.name

        internal fun prependToPath(dir: String, currentPath: String?, separator: String = File.pathSeparator): String =
            if (currentPath.isNullOrEmpty()) dir else "$dir$separator$currentPath"
    }
}
