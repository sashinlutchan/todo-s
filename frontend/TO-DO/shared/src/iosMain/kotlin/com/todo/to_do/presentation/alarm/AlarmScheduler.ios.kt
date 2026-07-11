package com.todo.to_do.presentation.alarm

import com.todo.to_do.util.nowInstant
import kotlinx.datetime.Instant
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private const val BURST_COUNT = 3
private const val BURST_SPACING_SECONDS = 25.0

/**
 * iOS caps any notification sound at ~30s and only "Critical Alerts" (a special, Apple-approved
 * entitlement) can bypass that - and even those aren't guaranteed to loop for a full minute. The
 * practical workaround without a special entitlement is a short burst of separate notifications
 * spaced ~25s apart, so the phone alerts repeatedly across roughly a minute.
 */
actual class AlarmScheduler {

    init {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> }
    }

    actual fun schedule(todoId: String, title: String, triggerAt: Instant) {
        cancel(todoId)

        val secondsFromNow = (triggerAt - nowInstant()).inWholeSeconds.toDouble()
        if (secondsFromNow <= 0) return

        val center = UNUserNotificationCenter.currentNotificationCenter()
        repeat(BURST_COUNT) { index ->
            val content = UNMutableNotificationContent()
            content.title = "Reminder"
            content.body = title
            content.sound = UNNotificationSound.defaultSound()

            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                secondsFromNow + index * BURST_SPACING_SECONDS,
                false
            )
            val request = UNNotificationRequest.requestWithIdentifier(requestId(todoId, index), content, trigger)
            center.addNotificationRequest(request) { }
        }
    }

    actual fun cancel(todoId: String) {
        val ids = (0 until BURST_COUNT).map { requestId(todoId, it) }
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    private fun requestId(todoId: String, index: Int) = "$todoId#$index"
}
