package de.spotlessformatplugin.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import de.spotlessformatplugin.SpotlessBundle
import de.spotlessformatplugin.settings.SpotlessFormatSettings

class SpotlessNotifier(private val project: Project) {

    /** A problem with the plugin settings (missing or unreadable files etc.). */
    fun notifyConfigurationError(content: String) =
        notifyProblem(SpotlessBundle.message("notification.title.configuration.error"), content, NotificationType.ERROR)

    /** Formatting itself failed (syntax error, formatter crashed, external tool failed). */
    fun notifyError(content: String) =
        notifyProblem(SpotlessBundle.message("notification.title.formatting.failed"), content, NotificationType.ERROR)

    fun notifyWarning(content: String) =
        notifyProblem(SpotlessBundle.message("notification.title.warning"), content, NotificationType.WARNING)

    /** Shown only when success notifications are enabled (off by default to avoid a balloon on every save). */
    fun notifySuccess(content: String) {
        LOG.debug(content)
        if (!settings().showSuccessNotifications) return
        notify(SpotlessBundle.message("notification.title.success"), content, NotificationType.INFORMATION)
    }

    private fun notifyProblem(title: String, content: String, type: NotificationType) {
        LOG.info("$title: $content")
        if (!settings().enableNotifications) return
        notify(title, content, type)
    }

    private fun notify(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, type)
            .notify(project)
    }

    private fun settings(): SpotlessFormatSettings.State =
        SpotlessFormatSettings.getInstance(project).state

    companion object {
        const val GROUP_ID = "Spotless Formatter"
        private val LOG = logger<SpotlessNotifier>()
    }
}
