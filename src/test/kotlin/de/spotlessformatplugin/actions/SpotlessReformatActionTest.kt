package de.spotlessformatplugin.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessReformatActionTest : BasePlatformTestCase() {

    private class RecordingAction(text: String = "Reformat Code") : AnAction(text, "Reformat source code", null) {
        var performed = 0
        override fun actionPerformed(event: AnActionEvent) {
            performed++
        }
    }

    override fun setUp() {
        super.setUp()
        val settings = SpotlessFormatSettings.getInstance(project)
        settings.loadState(SpotlessFormatSettings.State())
        settings.state.useSpotlessConfig = true // generic steps, no external configuration needed
    }

    private fun event(action: AnAction, vararg files: VirtualFile): AnActionEvent {
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, files.firstOrNull())
            .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(*files))
            .build()
        return TestActionEvent.createTestEvent(action, context)
    }

    fun testCopiesOriginalActionTextAndDescription() {
        val action = SpotlessReformatAction(RecordingAction())

        assertEquals("Reformat Code", action.templatePresentation.text)
        assertEquals("Reformat source code", action.templatePresentation.description)
    }

    fun testProvidesFallbackTextWhenOriginalTextIsEmpty() {
        val action = SpotlessReformatAction(RecordingAction(""))

        assertEquals("Reformat Code", action.templatePresentation.text)
    }

    fun testDelegatesToOriginalWhenOptionIsOff() {
        val original = RecordingAction()
        val action = SpotlessReformatAction(original)
        val file = myFixture.configureByText("Test.java", "class Test {}   ").virtualFile

        action.actionPerformed(event(action, file))

        assertEquals(1, original.performed)
        assertEquals("class Test {}   ", myFixture.editor.document.text)
    }

    fun testFormatsAllSelectedFilesWithSpotless() {
        SpotlessFormatSettings.getInstance(project).state.overrideReformatAction = true
        val original = RecordingAction()
        val action = SpotlessReformatAction(original)
        val first = myFixture.addFileToProject("First.java", "class First {}   ").virtualFile
        val second = myFixture.addFileToProject("Second.java", "class Second {}   ").virtualFile

        action.actionPerformed(event(action, first, second))

        assertEquals(0, original.performed)
        assertEquals("class First {}\n", myFixture.psiManager.findFile(first)!!.text)
        assertEquals("class Second {}\n", myFixture.psiManager.findFile(second)!!.text)
    }

    fun testFallsBackForDirectoriesAndUnsupportedFiles() {
        SpotlessFormatSettings.getInstance(project).state.overrideReformatAction = true
        val original = RecordingAction()
        val action = SpotlessReformatAction(original)
        val javaFile = myFixture.addFileToProject("pkg/Test.java", "class Test {}   ").virtualFile
        val textFile = myFixture.addFileToProject("notes.txt", "text").virtualFile

        action.actionPerformed(event(action, javaFile.parent))
        action.actionPerformed(event(action, javaFile, textFile))

        assertEquals(2, original.performed)
        assertEquals("class Test {}   ", myFixture.psiManager.findFile(javaFile)!!.text)
    }

    fun testInstallerReplacesAndRestoresTheReformatAction() {
        val manager = ActionManager.getInstance()
        val before = manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT)
        val installer = SpotlessReformatActionInstaller()
        try {
            installer.install()
            assertTrue(manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT) is SpotlessReformatAction)
        } finally {
            installer.dispose()
        }
        assertSame(before, manager.getAction(IdeActions.ACTION_EDITOR_REFORMAT))
    }
}
