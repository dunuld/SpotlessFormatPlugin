package de.spotlessformatplugin.services.formatters

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.spotlessformatplugin.services.DocumentTextService
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PrettierFormatter(
    private val project: Project,
    private val documentTextService: DocumentTextService,
    private val notifier: SpotlessNotifier
) {

    private data class PrettierExecutable(
        val command: List<String>,
        val nodeBinDir: String? = null
    )

    fun format(virtualFile: VirtualFile, settings: SpotlessFormatSettings.State) {
        val text = documentTextService.getFileText(virtualFile) ?: return
        val formatted = runPrettier(virtualFile, text, settings.prettierConfigPath)
        if (formatted != null) {
            documentTextService.updateDocumentText(virtualFile, formatted, "Prettier Formatting")
            notifier.notifyInfo("Applying Prettier using config: ${settings.prettierConfigPath}")
        }
    }

    private fun findPrettierExecutable(): PrettierExecutable? {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
        val projectBase = project.basePath

        // 1. Check local node_modules in project
        if (projectBase != null) {
            val localPrettier = File(projectBase, if (isWindows) "node_modules/.bin/prettier.cmd" else "node_modules/.bin/prettier")
            if (localPrettier.exists() && (isWindows || localPrettier.canExecute())) {
                return PrettierExecutable(listOf(localPrettier.absolutePath))
            }
        }

        // Candidate directories where node/npx/prettier might reside
        val candidateNodeDirs = mutableListOf<File>()
        val pathEnv = System.getenv("PATH") ?: ""
        pathEnv.split(File.pathSeparator).forEach {
            if (it.isNotBlank()) candidateNodeDirs.add(File(it))
        }

        val userHome = System.getProperty("user.home") ?: ""
        listOf(
            "/opt/homebrew/bin",
            "/usr/local/bin",
            "/usr/bin",
            "/bin",
            "$userHome/.volta/bin",
            "$userHome/.asdf/shims",
            "$userHome/.nodenv/shims"
        ).forEach { candidateNodeDirs.add(File(it)) }

        // Search nvm versions: ~/.nvm/versions/node/*/bin
        val nvmNodeDir = File(userHome, ".nvm/versions/node")
        if (nvmNodeDir.exists() && nvmNodeDir.isDirectory) {
            nvmNodeDir.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }?.forEach { versionDir ->
                val binDir = File(versionDir, "bin")
                if (binDir.exists() && binDir.isDirectory) {
                    candidateNodeDirs.add(binDir)
                }
            }
        }

        // 2. Search for direct 'prettier' binary in candidate dirs
        val prettierName = if (isWindows) "prettier.cmd" else "prettier"
        for (dir in candidateNodeDirs) {
            val file = File(dir, prettierName)
            if (file.exists() && (isWindows || file.canExecute())) {
                return PrettierExecutable(listOf(file.absolutePath), dir.absolutePath)
            }
        }

        // 3. Search for 'npx' binary in candidate dirs
        val npxName = if (isWindows) "npx.cmd" else "npx"
        for (dir in candidateNodeDirs) {
            val file = File(dir, npxName)
            if (file.exists() && (isWindows || file.canExecute())) {
                return PrettierExecutable(listOf(file.absolutePath, "prettier"), dir.absolutePath)
            }
        }

        return null
    }

    private fun runPrettier(virtualFile: VirtualFile, content: String, configPath: String?): String? {
        val prettierExec = findPrettierExecutable()
        if (prettierExec == null) {
            notifier.notifyError("Prettier executable not found. Please ensure prettier or npx is installed.")
            return null
        }

        val command = mutableListOf<String>()
        command.addAll(prettierExec.command)
        command.add("--stdin-filepath")
        command.add(virtualFile.name)

        if (!configPath.isNullOrBlank()) {
            val configFile = File(configPath)
            if (configFile.exists()) {
                command.add("--config")
                command.add(configFile.absolutePath)
            }
        }

        val processBuilder = ProcessBuilder(command)
        val workingDir = project.basePath?.let { File(it) } ?: File(virtualFile.path).parentFile
        if (workingDir != null && workingDir.exists()) {
            processBuilder.directory(workingDir)
        }

        prettierExec.nodeBinDir?.let { nodeDir ->
            val env = processBuilder.environment()
            val currentPath = env["PATH"] ?: System.getenv("PATH") ?: ""
            env["PATH"] = "$nodeDir:$currentPath"
        }

        return try {
            val process = processBuilder.start()

            process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }

            val stdoutFuture = CompletableFuture.supplyAsync {
                process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            }
            val stderrFuture = CompletableFuture.supplyAsync {
                process.errorStream.bufferedReader(Charsets.UTF_8).readText()
            }

            val finished = process.waitFor(15, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                notifier.notifyError("Prettier timed out while formatting ${virtualFile.name}.")
                return null
            }

            val exitCode = process.exitValue()
            val stdout = stdoutFuture.get(2, TimeUnit.SECONDS)
            val stderr = stderrFuture.get(2, TimeUnit.SECONDS)

            if (exitCode == 0) {
                stdout
            } else {
                notifier.notifyError("Prettier failed for ${virtualFile.name}: ${stderr.ifBlank { stdout }}")
                null
            }
        } catch (e: Exception) {
            notifier.notifyError("Error executing Prettier: ${e.message}")
            null
        }
    }
}
