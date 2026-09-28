package de.spotlessformatplugin.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessNotifier(private val project: Project) {

    fun notifyError(content: String) {
        if (!isNotificationsEnabled()) return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless configuration error", content, NotificationType.ERROR)
            .notify(project)
    }

    fun notifyInfo(content: String) {
        if (!isNotificationsEnabled()) return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless formatter", content, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun isNotificationsEnabled(): Boolean {
        return try {
            SpotlessFormatSettings.getInstance(project).state.enableNotifications
        } catch (_: Exception) {
            true
        }
    }
}
