package com.example.aio_prodapp.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aio_prodapp.MainActivity
import com.example.aio_prodapp.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val CHANNEL_ID = "reminders"
    const val CHANNEL_NAME = "Reminders"

    /** Create notification channel on Android 8.0+ */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for notes and tasks"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /** Schedules a WorkManager job for a note reminder at 09:00 local time on the reminder date. */
    fun scheduleNoteReminder(context: Context, noteId: Int, reminderDateUtcMs: Long?) {
        if (reminderDateUtcMs == null) {
            cancelNoteReminder(context, noteId)
            return
        }
        val delayMs = calculateDelayMs(reminderDateUtcMs) ?: run {
            cancelNoteReminder(context, noteId)
            return
        }

        val data = Data.Builder()
            .putString("type", "note")
            .putInt("id", noteId)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "note-$noteId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelNoteReminder(context: Context, noteId: Int) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("note-$noteId")
    }

    /** Schedules a WorkManager job for a task reminder at 09:00 local time on the reminder date. */
    fun scheduleTaskReminder(context: Context, taskId: Int, reminderDateUtcMs: Long?) {
        if (reminderDateUtcMs == null) {
            cancelTaskReminder(context, taskId)
            return
        }
        val delayMs = calculateDelayMs(reminderDateUtcMs) ?: run {
            cancelTaskReminder(context, taskId)
            return
        }

        val data = Data.Builder()
            .putString("type", "task")
            .putInt("id", taskId)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "task-$taskId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("task-$taskId")
    }

    /** Calculates initial delay until 09:00 local time on [reminderDateUtcMs]. */
    private fun calculateDelayMs(reminderDateUtcMs: Long): Long? {
        val utcDate = Instant.ofEpochMilli(reminderDateUtcMs).atZone(ZoneOffset.UTC).toLocalDate()
        val todayLocal = LocalDate.now(ZoneId.systemDefault())

        if (utcDate.isBefore(todayLocal)) {
            // Past date from earlier days -> do not schedule
            return null
        }

        val localTargetTime = LocalDateTime.of(utcDate, LocalTime.of(9, 0, 0))
        val targetEpochMs = localTargetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMs = System.currentTimeMillis()

        return if (utcDate == todayLocal && nowMs >= targetEpochMs) {
            // Today and 09:00 has passed -> deliver within a second
            1000L
        } else {
            maxOf(0L, targetEpochMs - nowMs)
        }
    }

    fun postNoteSystemNotification(context: Context, noteId: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("note_id", noteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        postSystemNotification(context, notificationId = 1000 + noteId, title = title, body = body, intent = intent)
    }

    fun postTaskSystemNotification(context: Context, taskId: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        postSystemNotification(context, notificationId = 2000 + taskId, title = title, body = body, intent = intent)
    }

    private fun postSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        intent: Intent
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission denied: skip posting system notification
                return
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // SecurityException when permission revoked at runtime
        }
    }
}
