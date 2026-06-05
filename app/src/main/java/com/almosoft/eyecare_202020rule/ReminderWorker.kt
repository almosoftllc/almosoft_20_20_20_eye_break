package com.almosoft.eyecare_202020rule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.almosoft.eyecare_202020rule.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val preferenceManager = PreferenceManager(applicationContext)
        
        val isAutomatic = runBlocking { preferenceManager.isAutomaticWorkHours.first() }
        if (isAutomatic) {
            val startTime = runBlocking { preferenceManager.startTime.first() }
            val endTime = runBlocking { preferenceManager.endTime.first() }
            
            if (!isCurrentTimeInWindow(startTime, endTime)) {
                return Result.success()
            }
        }

        showNotification()
        return Result.success()
    }

    private fun isCurrentTimeInWindow(start: String, end: String): Boolean {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()
            val currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            val currentTime = sdf.parse(currentTimeStr)
            
            val startTime = sdf.parse(start)
            val endTime = sdf.parse(end)
            
            if (currentTime != null && startTime != null && endTime != null) {
                return if (endTime.before(startTime)) {
                    // Window spans across midnight
                    currentTime.after(startTime) || currentTime.before(endTime)
                } else {
                    currentTime.after(startTime) && currentTime.before(endTime)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true // Fallback to show notification if parsing fails
    }

    private fun showNotification() {
        val channelId = "eye_care_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Eye Care Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take 20-20-20 breaks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time for an Eye Break! 👁️")
            .setContentText("Look 20 feet away for 20 seconds.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
