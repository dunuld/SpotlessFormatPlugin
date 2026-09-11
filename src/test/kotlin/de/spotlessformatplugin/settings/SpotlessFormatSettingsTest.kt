package de.spotlessformatplugin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SpotlessFormatSettingsTest : BasePlatformTestCase() {

    fun testSettingsArePersisted() {
        val settings = SpotlessFormatSettings.getInstance(project)
        val state = settings.state

        state.formatterXmlPath = "/path/to/formatter.xml"
        state.importOrderPath = "/path/to/import.order"
        state.executeOnSave = true
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE

        val newSettings = SpotlessFormatSettings.getInstance(project)
        assertEquals("/path/to/formatter.xml", newSettings.state.formatterXmlPath)
        assertEquals("/path/to/import.order", newSettings.state.importOrderPath)
        assertEquals(SpotlessFormatSettings.FormatterType.ECLIPSE, newSettings.state.formatterType)
        assertTrue(newSettings.state.executeOnSave)
    }

    fun testSettingsModifiedAndApply() {
        val configurable = SpotlessFormatConfigurable(project)
        configurable.createComponent()
        val settings = SpotlessFormatSettings.getInstance(project)

        settings.state.useSpotlessConfig = false
        settings.state.spotlessConfigPath = ""
        configurable.reset()

        assertFalse(configurable.isModified)

        settings.state.spotlessConfigPath = "new-config.gradle"
        configurable.reset()
        assertFalse(configurable.isModified)
    }
}
