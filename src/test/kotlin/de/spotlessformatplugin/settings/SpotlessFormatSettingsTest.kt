package de.spotlessformatplugin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class SpotlessFormatSettingsTest : BasePlatformTestCase() {

    fun testSettingsArePersisted() {
        val settings = SpotlessFormatSettings.getInstance(project)
        val state = settings.state

        state.formatterXmlPath = "/path/to/formatter.xml"
        state.formatterProfile = "MyProfile"
        state.importOrderPath = "/path/to/import.order"
        state.executeOnSave = true
        state.overrideReformatAction = true
        state.enableNotifications = false
        state.useSpotlessConfig = false
        state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE

        val newSettings = SpotlessFormatSettings.getInstance(project)
        assertEquals("/path/to/formatter.xml", newSettings.state.formatterXmlPath)
        assertEquals("MyProfile", newSettings.state.formatterProfile)
        assertEquals("/path/to/import.order", newSettings.state.importOrderPath)
        assertEquals(SpotlessFormatSettings.FormatterType.ECLIPSE, newSettings.state.formatterType)
        assertTrue(newSettings.state.executeOnSave)
        assertTrue(newSettings.state.overrideReformatAction)
        assertFalse(newSettings.state.enableNotifications)
    }

    fun testDefaultSettings() {
        val state = SpotlessFormatSettings.State()
        assertTrue(state.enableNotifications)
        assertFalse(state.executeOnSave)
        assertFalse(state.overrideReformatAction)
        assertFalse(state.useSpotlessConfig)
        assertEquals(SpotlessFormatSettings.FormatterType.ECLIPSE, state.formatterType)
    }

    fun testSettingsModifiedAndApply() {
        val configurable = SpotlessFormatConfigurable(project)
        configurable.createComponent()
        val settings = SpotlessFormatSettings.getInstance(project)

        settings.state.useSpotlessConfig = false
        settings.state.spotlessConfigPath = ""
        settings.state.executeOnSave = false
        settings.state.overrideReformatAction = false
        settings.state.enableNotifications = true
        settings.state.formatterProfile = ""
        configurable.reset()

        assertFalse(configurable.isModified)

        settings.state.spotlessConfigPath = "new-config.gradle"
        settings.state.executeOnSave = true
        settings.state.overrideReformatAction = true
        settings.state.enableNotifications = false
        configurable.reset()
        assertFalse(configurable.isModified)
        assertTrue(settings.state.overrideReformatAction)
    }

    fun testConfigurablePopulatesAndAppliesProfiles() {
        val tempDir = java.nio.file.Files.createTempDirectory("spotlessTestSettings").toFile()
        try {
            val xmlFile = File(tempDir, "formatter.xml")
            xmlFile.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <profiles version="13">
                    <profile kind="CodeFormatterProfile" name="Profile1" version="13"/>
                    <profile kind="CodeFormatterProfile" name="Profile2" version="13"/>
                </profiles>
                """.trimIndent()
            )

            val settings = SpotlessFormatSettings.getInstance(project)
            settings.state.formatterType = SpotlessFormatSettings.FormatterType.ECLIPSE
            settings.state.formatterXmlPath = xmlFile.absolutePath
            settings.state.formatterProfile = "Profile2"

            val configurable = SpotlessFormatConfigurable(project)
            configurable.createComponent()
            configurable.reset()

            assertFalse(configurable.isModified)

            // Modify state through reset
            settings.state.formatterProfile = "Profile1"
            configurable.reset()
            assertFalse(configurable.isModified)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
