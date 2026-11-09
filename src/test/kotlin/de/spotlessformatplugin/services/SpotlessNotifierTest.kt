package de.spotlessformatplugin.services

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessNotifierTest : BasePlatformTestCase() {

    private val received = mutableListOf<Notification>()

    override fun setUp() {
        super.setUp()
        SpotlessFormatSettings.getInstance(project).loadState(SpotlessFormatSettings.State())
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == SpotlessNotifier.GROUP_ID) received.add(notification)
            }
        })
    }

    fun testSuccessNotificationsAreOffByDefault() {
        SpotlessNotifier(project).notifySuccess("formatted")
        assertEmpty(received)

        SpotlessFormatSettings.getInstance(project).state.showSuccessNotifications = true
        SpotlessNotifier(project).notifySuccess("formatted")
        assertEquals(1, received.size)
    }

    fun testErrorsUseSpecificTitlesAndCanBeDisabled() {
        val notifier = SpotlessNotifier(project)
        notifier.notifyError("boom")
        notifier.notifyConfigurationError("missing file")
        assertEquals(listOf("Spotless formatting failed", "Spotless configuration error"), received.map { it.title })

        SpotlessFormatSettings.getInstance(project).state.enableNotifications = false
        notifier.notifyError("boom")
        notifier.notifyWarning("careful")
        assertEquals(2, received.size)
    }
}
