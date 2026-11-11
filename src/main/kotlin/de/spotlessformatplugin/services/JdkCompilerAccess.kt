package de.spotlessformatplugin.services

import com.intellij.openapi.diagnostic.logger

/**
 * google-java-format uses internal `jdk.compiler` APIs. The IDE only opens `com.sun.tools.javac.api` to unnamed
 * modules, so without further access GJF fails with an `IllegalAccessError` at runtime. This opens the required
 * packages to the module that contains the formatter classes (the plugin's unnamed module).
 */
object JdkCompilerAccess {
    private val LOG = logger<JdkCompilerAccess>()

    private val REQUIRED_PACKAGES = listOf("api", "code", "comp", "file", "main", "parser", "tree", "util")
        .map { "com.sun.tools.javac.$it" }

    /** VM options that grant the same access manually (shown to the user if the automatic approach fails). */
    val MANUAL_VM_OPTIONS: String = REQUIRED_PACKAGES.joinToString("\n") { "--add-opens=jdk.compiler/$it=ALL-UNNAMED" }

    @Volatile
    private var granted = false

    /** @throws IllegalStateException if access could not be granted. */
    @Synchronized
    fun ensureAccess(target: Class<*>) {
        if (granted) return
        val compilerModule = ModuleLayer.boot().findModule("jdk.compiler").orElseThrow {
            IllegalStateException("The IDE runtime does not contain the jdk.compiler module.")
        }
        val targetModule = target.module
        val missing = REQUIRED_PACKAGES.filter {
            compilerModule.packages.contains(it) && !compilerModule.isOpen(it, targetModule)
        }
        if (missing.isNotEmpty()) {
            try {
                val addOpens = Module::class.java.getDeclaredMethod("implAddOpens", String::class.java, Module::class.java)
                addOpens.isAccessible = true
                for (pkg in missing) {
                    addOpens.invoke(compilerModule, pkg, targetModule)
                }
            } catch (e: Exception) {
                LOG.warn("Could not open jdk.compiler packages for google-java-format", e)
                throw IllegalStateException("Could not open jdk.compiler packages: ${e.message}", e)
            }
        }
        granted = true
    }
}
