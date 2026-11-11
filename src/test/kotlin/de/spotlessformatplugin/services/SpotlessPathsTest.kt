package de.spotlessformatplugin.services

import junit.framework.TestCase
import java.io.File

class SpotlessPathsTest : TestCase() {

    fun testBlankPathResolvesToNull() {
        assertNull(SpotlessPaths.resolve("/project", ""))
        assertNull(SpotlessPaths.resolve("/project", "   "))
        assertNull(SpotlessPaths.resolve("/project", null))
    }

    fun testRelativePathIsResolvedAgainstProjectRoot() {
        assertEquals(File("/project", "config/formatter.xml"), SpotlessPaths.resolve("/project", "config/formatter.xml"))
    }

    fun testProjectDirMacroIsExpanded() {
        assertEquals(File("/project/config/formatter.xml"), SpotlessPaths.resolve("/project", "\$PROJECT_DIR\$/config/formatter.xml"))
    }

    fun testAbsolutePathIsKept() {
        val absolute = File("/etc/formatter.xml").absoluteFile
        assertEquals(absolute, SpotlessPaths.resolve("/project", absolute.path))
    }

    fun testHomeDirectoryIsExpanded() {
        assertEquals(File(System.getProperty("user.home") + "/formatter.xml"), SpotlessPaths.resolve("/project", "~/formatter.xml"))
    }
}
