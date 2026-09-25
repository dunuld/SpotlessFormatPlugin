package de.spotlessformatplugin.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessNotifierTest : BasePlatformTestCase() {

    fun testNotifyWhenEnabledAndDisabled() {
        val settings = SpotlessFormatSettings.getInstance(project)
        val notifier = SpotlessNotifier(project)

        settings.state.enableNotifications = true
        // Should not throw any exception when notifications are enabled
        notifier.notifyInfo("Info message")
        notifier.notifyError("Error message")

        settings.state.enableNotifications = false
        // Should not throw any exception and should skip notification when disabled
        notifier.notifyInfo("Info message")
        notifier.notifyError("Error message")
    }
}
