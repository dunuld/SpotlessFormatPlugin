package de.spotlessformatplugin.actionsonsave

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import kotlinx.coroutines.runBlocking

class SpotlessActionOnSaveTest : BasePlatformTestCase() {

    private lateinit var settings: SpotlessFormatSettings

    override fun setUp() {
        super.setUp()
        settings = SpotlessFormatSettings.getInstance(project)
        settings.loadState(SpotlessFormatSettings.State())
        // Generic steps need no external configuration.
        settings.state.useSpotlessConfig = true
    }

    private fun updateDocument(document: Document) {
        val future = ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking { SpotlessActionOnSave().updateDocument(project, document) }
        }
        PlatformTestUtil.waitForFuture(future, 60_000)
    }

    fun testIsRegisteredAsActionOnSave() {
        val extensions = ApplicationManager.getApplication().extensionArea
            .getExtensionPoint<ActionsOnSaveFileDocumentManagerListener.ActionOnSave>("com.intellij.actionOnSave")
            .extensionList
        assertTrue(extensions.any { it is SpotlessActionOnSave })
    }

    fun testEnabledOnlyWhenExecuteOnSaveIsSet() {
        settings.state.executeOnSave = false
        assertFalse(SpotlessActionOnSave().isEnabledForProject(project))
        settings.state.executeOnSave = true
        assertTrue(SpotlessActionOnSave().isEnabledForProject(project))
    }

    fun testFormatsProjectFile() {
        val psiFile = myFixture.configureByText("Test.java", "class Test {}   ")
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)!!

        updateDocument(document)

        assertEquals("class Test {}\n", document.text)
    }

    fun testSkipsUnsupportedFiles() {
        val psiFile = myFixture.configureByText("notes.txt", "text   ")
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)!!

        updateDocument(document)

        assertEquals("text   ", document.text)
    }

    fun testSkipsFilesOutsideTheProject() {
        val external = LightVirtualFile("External.java", "class External {}   ")
        val document = FileDocumentManager.getInstance().getDocument(external)!!

        updateDocument(document)

        assertEquals("class External {}   ", document.text)
    }
}
