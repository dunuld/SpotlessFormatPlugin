package de.spotlessformatplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SpotlessReformatActionTest : BasePlatformTestCase() {

    fun testCopiesOriginalActionTextAndDescription() {
        val original = object : AnAction("Reformat Code", "Reformat source code", null) {
            override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) = Unit
        }

        val action = SpotlessReformatAction(original)

        assertEquals("Reformat Code", action.templatePresentation.text)
        assertEquals("Reformat source code", action.templatePresentation.description)
    }

    fun testProvidesFallbackTextWhenOriginalTextIsEmpty() {
        val original = object : AnAction("", "Original description", null) {
            override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) = Unit
        }

        val action = SpotlessReformatAction(original)

        assertEquals("Reformat Code", action.templatePresentation.text)
    }
}
