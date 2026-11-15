package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.PageNestDatabase
import com.example.data.local.ReaderPreferencesDataStore
import com.example.model.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class StreakReminderWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val isTest = inputData.getBoolean(KEY_FORCE_TEST, false)
    val prefs = ReaderPreferencesDataStore(applicationContext)
    val settings = prefs.settingsFlow.first()

    if (!isTest && !settings.streakReminderEnabled) {
      return Result.success()
    }

    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val stats = runCatching {
      PageNestDatabase.getInstance(applicationContext).pageNestDao().observeAllDailyStats().first()
    }.getOrDefault(emptyList())
    val todayDbSec = stats.firstOrNull { it.dateKey == todayKey }?.readingSeconds ?: 0
    val prefSec = if (settings.lastReadingDateKey == todayKey) settings.todayReadingSeconds else 0
    val todaySeconds = maxOf(todayDbSec, prefSec)
    val goalSeconds = settings.dailyGoalMinutes * 60

    val goalAlreadyMet = todaySeconds >= goalSeconds
    if (isTest || !goalAlreadyMet) {
      val remainingMinutes = ((goalSeconds - todaySeconds) / 60).coerceAtLeast(1)
      val streakDays = computeStreakDays(stats.associateBy { it.dateKey }, todayKey, todaySeconds > 0)
      postReminderNotification(
        context = applicationContext,
        language = settings.appLanguage,
        remainingMinutes = remainingMinutes,
        streakDays = streakDays,
        goalMet = goalAlreadyMet,
      )
    }

    if (!isTest && settings.streakReminderEnabled) {
      StreakReminderScheduler.scheduleDaily(
        context = applicationContext,
        hour = settings.streakReminderHour,
        minute = settings.streakReminderMinute,
      )
    }

    return Result.success()
  }

  private fun computeStreakDays(
    byDate: Map<String, com.example.data.local.DailyReadingStatEntity>,
    todayKey: String,
    readToday: Boolean,
  ): Int {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    if (!readToday) cal.add(Calendar.DAY_OF_YEAR, -1)
    var streak = 0
    repeat(365) {
      val key = formatter.format(cal.time)
      val sec = if (key == todayKey && readToday) 1 else (byDate[key]?.readingSeconds ?: 0)
      if (sec > 0) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
      } else {
        return streak
      }
    }
    return streak
  }

  companion object {
    const val CHANNEL_ID = "pagenest_streak_reminders"
    const val KEY_FORCE_TEST = "force_test"
    private const val NOTIFICATION_ID = 4108

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
          val channel =
            NotificationChannel(
              CHANNEL_ID,
              "Daily Reading Reminders",
              NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
              description = "Gentle daily reminders to keep your PageNest reading streak alive."
            }
          manager.createNotificationChannel(channel)
        }
      }
    }

    fun hasNotificationPermission(context: Context): Boolean {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted =
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return false
      }
      return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postReminderNotification(
      context: Context,
      language: AppLanguage,
      remainingMinutes: Int,
      streakDays: Int,
      goalMet: Boolean,
    ) {
      if (!hasNotificationPermission(context)) return
      ensureChannel(context)

      val isFa = language == AppLanguage.PERSIAN
      val title =
        when {
          goalMet -> if (isFa) "هدف مطالعه امروز تکمیل است" else "Today's reading goal is complete"
          streakDays > 0 ->
            if (isFa) "رشته $streakDays روزه مطالعه را حفظ کنید"
            else "Keep your $streakDays-day reading streak alive"
          else -> if (isFa) "زمان آرام مطالعه در پیج‌نست" else "Time for calm reading in PageNest"
        }
      val body =
        when {
          goalMet ->
            if (isFa) "آفرین! هر زمان خواستید می‌توانید چند صفحه دیگر بخوانید."
            else "Great work today. Open PageNest whenever you feel like a few quiet pages."
          else ->
            if (isFa) "فقط $remainingMinutes دقیقه تا تکمیل هدف روزانه باقی مانده است."
            else "Only $remainingMinutes min left to reach today's reading goal."
        }

      val openIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          101,
          openIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setContentTitle(title)
          .setContentText(body)
          .setStyle(NotificationCompat.BigTextStyle().bigText(body))
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setContentIntent(pendingIntent)
          .setAutoCancel(true)
          .build()

      runCatching {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      }
    }
  }
}

object StreakReminderScheduler {
  private const val WORK_NAME = "pagenest_daily_streak_reminder"

  fun computeDelayMillisUntil(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target =
      Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    if (!target.after(now)) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return (target.timeInMillis - nowMillis).coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
  }

  fun scheduleDaily(context: Context, hour: Int, minute: Int) {
    StreakReminderWorker.ensureChannel(context)
    val delayMs = computeDelayMillisUntil(hour, minute)
    val request =
      OneTimeWorkRequestBuilder<StreakReminderWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context)
      .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
  }

  fun triggerTestNow(context: Context) {
    StreakReminderWorker.ensureChannel(context)
    val data = Data.Builder().putBoolean(StreakReminderWorker.KEY_FORCE_TEST, true).build()
    val request = OneTimeWorkRequestBuilder<StreakReminderWorker>().setInputData(data).build()
    WorkManager.getInstance(context).enqueue(request)
  }
}
