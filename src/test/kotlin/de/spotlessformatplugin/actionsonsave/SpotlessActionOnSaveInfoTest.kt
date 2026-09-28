package de.spotlessformatplugin.actionsonsave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableGroup
import com.intellij.openapi.options.ex.Settings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatConfigurable
import de.spotlessformatplugin.settings.SpotlessFormatSettings
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class SpotlessActionOnSaveInfoTest : BasePlatformTestCase() {

    private lateinit var settings: SpotlessFormatSettings

    override fun setUp() {
        super.setUp()
        settings = SpotlessFormatSettings.getInstance(project)
        settings.state.executeOnSave = false
    }

    override fun tearDown() {
        settings.state.executeOnSave = false
        super.tearDown()
    }

    private class TestGroup(private val configurable: Configurable) : ConfigurableGroup {
        override fun getDisplayName(): String = "Test"
        override fun getConfigurables(): Array<Configurable> = arrayOf(configurable)
    }

    private class TestSettings(configurable: Configurable) : Settings(listOf(TestGroup(configurable))) {
        override fun selectImpl(p0: Configurable?): Promise<Any?> = resolvedPromise()
        override fun getConfigurableWithInitializedUiComponentImpl(p0: Configurable, p1: Boolean): Configurable = p0
        override fun checkModifiedImpl(p0: Configurable) {}
        override fun setSearchText(p0: String?) {}
    }

    private fun createActionOnSaveContext(settingsObj: Settings? = null): ActionOnSaveContext {
        val dummySettings = settingsObj ?: TestSettings(SpotlessFormatConfigurable(project))
        val constructor = ActionOnSaveContext::class.java.declaredConstructors[0]
        constructor.isAccessible = true
        return constructor.newInstance(project, dummySettings, testRootDisposable) as ActionOnSaveContext
    }

    fun testExtensionIsRegistered() {
        val providers = ActionOnSaveInfoProvider.EP_NAME.extensionList
        assertTrue(providers.any { it is SpotlessActionOnSaveInfoProvider })
    }

    fun testActionOnSaveInfoBasicProperties() {
        val provider = SpotlessActionOnSaveInfoProvider()
        val context = createActionOnSaveContext()
        val infos = provider.getActionOnSaveInfos(context)

        assertEquals(1, infos.size)
        val info = infos.first() as SpotlessActionOnSaveInfo

        assertEquals("Format changed files with Spotless", info.actionOnSaveName)
        assertEquals(1, info.actionLinks.size)

        assertFalse(info.isActionOnSaveEnabled)
        assertFalse(info.isModified)

        info.isActionOnSaveEnabled = true
        assertTrue(info.isActionOnSaveEnabled)
        assertTrue(info.isModified)
        assertFalse(settings.state.executeOnSave)

        info.apply()
        assertTrue(settings.state.executeOnSave)
        assertFalse(info.isModified)
    }

    fun testActionOnSaveInfoInitialStateReflectsSettings() {
        settings.state.executeOnSave = true

        val provider = SpotlessActionOnSaveInfoProvider()
        val context = createActionOnSaveContext()
        val info = provider.getActionOnSaveInfos(context).first() as SpotlessActionOnSaveInfo

        assertTrue(info.isActionOnSaveEnabled)
        assertFalse(info.isModified)

        info.isActionOnSaveEnabled = false
        assertFalse(info.isActionOnSaveEnabled)
        assertTrue(info.isModified)

        info.apply()
        assertFalse(settings.state.executeOnSave)
        assertFalse(info.isModified)
    }

    fun testSynchronizationWithConfigurable() {
        val configurable = SpotlessFormatConfigurable(project)
        val dummySettings = TestSettings(configurable)

        val context = createActionOnSaveContext(dummySettings)
        val info = SpotlessActionOnSaveInfo(context)

        assertFalse(info.isActionOnSaveEnabled)
        assertFalse(configurable.isExecuteOnSave)

        // Change via Configurable
        configurable.isExecuteOnSave = true
        assertTrue(info.isActionOnSaveEnabled)
        assertTrue(info.isModified)

        // Change via ActionOnSaveInfo
        info.isActionOnSaveEnabled = false
        assertFalse(configurable.isExecuteOnSave)
        assertFalse(info.isActionOnSaveEnabled)
        assertFalse(info.isModified)

        info.isActionOnSaveEnabled = true
        assertTrue(configurable.isExecuteOnSave)

        info.apply()
        assertTrue(settings.state.executeOnSave)
    }

    fun testConfigurableComponentLifecycleAndReset() {
        val configurable = SpotlessFormatConfigurable(project)
        settings.state.executeOnSave = true

        val component = configurable.createComponent()
        assertNotNull(component)

        configurable.reset()
        assertTrue(configurable.isExecuteOnSave)

        configurable.isExecuteOnSave = false
        assertTrue(configurable.isModified)

        configurable.apply()
        assertFalse(settings.state.executeOnSave)

        configurable.disposeUIResources()
    }
}
