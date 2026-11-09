package de.spotlessformatplugin.services.formatters

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.services.SpotlessNotifier
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import java.io.File

class PrettierFormatterTest : BasePlatformTestCase() {

    private lateinit var tempDir: File
    private lateinit var binDir: File
    private lateinit var argsFile: File

    override fun setUp() {
        super.setUp()
        tempDir = java.nio.file.Files.createTempDirectory("spotlessTest").toFile()
        binDir = File(tempDir, "bin").apply { mkdirs() }
        argsFile = File(tempDir, "args.txt")
    }

    override fun tearDown() {
        try {
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    /** A fake executable that records its arguments, consumes stdin and prints CRLF output like `endOfLine: crlf`. */
    private fun createFakeExecutable(name: String) {
        val script = File(binDir, name)
        script.writeText(
            """
            #!/bin/sh
            for arg in "${'$'}@"; do echo "${'$'}arg" >> "${argsFile.absolutePath}"; done
            cat > /dev/null
            printf 'const x = 1;\r\nconst y = 2;\r\n'
            """.trimIndent() + "\n"
        )
        script.setExecutable(true)
    }

    private fun formatter(trusted: Boolean = true) =
        PrettierFormatter(project, SpotlessNotifier(project), { trusted }, listOf(binDir), 15)

    private fun localJsFile(): com.intellij.openapi.vfs.VirtualFile {
        val ioFile = File(tempDir, "src/test.js")
        ioFile.parentFile.mkdirs()
        ioFile.writeText("const x=1")
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)!!
    }

    fun testFormatsWithPrettierAndNormalizesLineSeparators() {
        if (SystemInfo.isWindows) return
        createFakeExecutable("prettier")
        val configFile = File(tempDir, ".prettierrc").apply { writeText("{}") }
        val settings = SpotlessFormatSettings.State().apply { prettierConfigPath = configFile.absolutePath }
        val file = localJsFile()

        val result = formatter().format(file, "const x=1", settings)

        assertEquals("const x = 1;\nconst y = 2;\n", result)
        val args = argsFile.readLines()
        // Full path so that .prettierrc lookup, overrides and .prettierignore work relative to the file
        assertEquals(File(file.path).absolutePath, args[args.indexOf("--stdin-filepath") + 1])
        assertEquals(configFile.absolutePath, args[args.indexOf("--config") + 1])
    }

    fun testUntrustedProjectDoesNotRunPrettier() {
        if (SystemInfo.isWindows) return
        createFakeExecutable("prettier")

        val result = formatter(trusted = false).format(localJsFile(), "const x=1", SpotlessFormatSettings.State())

        assertNull(result)
        assertFalse("Prettier must not be executed for untrusted projects", argsFile.exists())
    }

    fun testNpxNeverInstallsPackages() {
        if (SystemInfo.isWindows) return
        val globalPrettierOnPath = (System.getenv("PATH") ?: "").split(File.pathSeparator)
            .any { File(it, "prettier").isFile }
        if (globalPrettierOnPath) return
        createFakeExecutable("npx")

        val executable = formatter().findPrettierExecutable()

        assertNotNull(executable)
        assertEquals(listOf(File(binDir, "npx").absolutePath, "--no-install", "prettier"), executable!!.command)
    }

    fun testPathIsPrependedWithPlatformSeparator() {
        assertEquals("C:\\node;C:\\Windows", PrettierFormatter.prependToPath("C:\\node", "C:\\Windows", ";"))
        assertEquals("/opt/node:/usr/bin", PrettierFormatter.prependToPath("/opt/node", "/usr/bin", ":"))
        assertEquals("/opt/node", PrettierFormatter.prependToPath("/opt/node", null))
    }
}
