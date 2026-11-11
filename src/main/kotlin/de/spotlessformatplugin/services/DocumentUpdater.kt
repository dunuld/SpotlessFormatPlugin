package de.spotlessformatplugin.services

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.comparison.DiffTooBigException
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.util.text.StringUtil

/**
 * Applies formatter output to a [Document] by replacing only the changed lines, so caret, folding, markers and undo
 * stay intact as far as possible. Must be called inside a write command.
 */
object DocumentUpdater {

    fun applyMinimalChanges(document: Document, formattedText: String) {
        val newText = StringUtil.convertLineSeparators(formattedText)
        val oldText = document.immutableCharSequence
        if (StringUtil.equals(oldText, newText)) return

        val fragments = try {
            ComparisonManager.getInstance()
                .compareLines(oldText, newText, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE)
        } catch (_: DiffTooBigException) {
            null
        }

        if (fragments == null) {
            document.setText(newText)
            return
        }

        for (fragment in fragments.asReversed()) {
            document.replaceString(
                fragment.startOffset1,
                fragment.endOffset1,
                newText.subSequence(fragment.startOffset2, fragment.endOffset2)
            )
        }

        if (!StringUtil.equals(document.immutableCharSequence, newText)) {
            document.setText(newText)
        }
    }
}
