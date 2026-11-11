package de.spotlessformatplugin.services

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DocumentUpdaterTest : BasePlatformTestCase() {

    fun testOnlyChangedLinesAreReplaced() {
        val document = DocumentImpl("first\nsecond   \nthird\n")
        val unchangedMarker = document.createRangeMarker(0, 5) // "first"

        WriteCommandAction.runWriteCommandAction(project) {
            DocumentUpdater.applyMinimalChanges(document, "first\nsecond\nthird\n")
        }

        assertEquals("first\nsecond\nthird\n", document.text)
        assertTrue(unchangedMarker.isValid)
        assertEquals(0, unchangedMarker.startOffset)
        assertEquals(5, unchangedMarker.endOffset)
    }

    fun testWindowsLineSeparatorsAreNormalized() {
        val document = DocumentImpl("a\nb\n")

        WriteCommandAction.runWriteCommandAction(project) {
            DocumentUpdater.applyMinimalChanges(document, "a\r\nB\r\n")
        }

        assertEquals("a\nB\n", document.text)
    }

    fun testUnchangedTextDoesNotModifyDocument() {
        val document = DocumentImpl("same\n")
        val stamp = document.modificationStamp

        WriteCommandAction.runWriteCommandAction(project) {
            DocumentUpdater.applyMinimalChanges(document, "same\n")
        }

        assertEquals(stamp, document.modificationStamp)
    }
}
