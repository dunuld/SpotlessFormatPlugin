package de.spotlessformatplugin.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class SpotlessNotifier(private val project: Project) {

    fun notifyError(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless Configuration Error", content, NotificationType.ERROR)
            .notify(project)
    }

    fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Spotless Formatter")
            .createNotification("Spotless Formatter", content, NotificationType.INFORMATION)
            .notify(project)
    }
}
