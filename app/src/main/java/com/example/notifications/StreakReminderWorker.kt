package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reliable BroadcastReceiver triggered by AlarmManager at the user's chosen reminder time,
 * and on device boot / time / timezone / package updates to keep the daily schedule alive.
 */
class StreakReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    val pendingResult = goAsync()
    val appContext = context.applicationContext

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val prefs = ReaderPreferencesDataStore(appContext)
        val settings = prefs.settingsFlow.first()
        when (action) {
          StreakReminderScheduler.ACTION_STREAK_REMINDER -> {
            if (settings.streakReminderEnabled) {
              StreakReminderWorker.executeReminderCheck(
                context = appContext,
                isTest = false,
              )
              // Immediately schedule tomorrow's exact alarm (adding 90s offset so same-minute re-triggers are avoided)
              StreakReminderScheduler.scheduleDaily(
                context = appContext,
                hour = settings.streakReminderHour,
                minute = settings.streakReminderMinute,
                nowMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(90),
              )
            }
          }
          Intent.ACTION_BOOT_COMPLETED,
          Intent.ACTION_TIME_CHANGED,
          Intent.ACTION_TIMEZONE_CHANGED,
          Intent.ACTION_MY_PACKAGE_REPLACED -> {
            if (settings.streakReminderEnabled) {
              StreakReminderScheduler.scheduleDaily(
                context = appContext,
                hour = settings.streakReminderHour,
                minute = settings.streakReminderMinute,
              )
            }
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}

class StreakReminderWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val isTest = inputData.getBoolean(KEY_FORCE_TEST, false)
    executeReminderCheck(applicationContext, isTest = isTest)
    return Result.success()
  }

  companion object {
    const val CHANNEL_ID = "atlas_daily_reminders"
    const val KEY_FORCE_TEST = "force_test"
    private const val NOTIFICATION_ID = 4108
    private const val ATLAS_BLUE_ARGB = 0xFF111111.toInt()

    suspend fun executeReminderCheck(context: Context, isTest: Boolean) {
      val prefs = ReaderPreferencesDataStore(context)
      val settings = prefs.settingsFlow.first()

      if (!isTest && !settings.streakReminderEnabled) {
        return
      }

      val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val stats = runCatching {
        PageNestDatabase.getInstance(context).pageNestDao().observeAllDailyStats().first()
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
          context = context,
          language = settings.appLanguage,
          remainingMinutes = remainingMinutes,
          streakDays = streakDays,
          goalMet = goalAlreadyMet,
        )
      }
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

    fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
          val channel =
            NotificationChannel(
              CHANNEL_ID,
              "ATLAS Daily Reading Reminders",
              NotificationManager.IMPORTANCE_HIGH,
            ).apply {
              description = "Timely daily reminders to keep your ATLAS reading streak alive."
              enableVibration(true)
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
          else -> if (isFa) "زمان آرام مطالعه در اطلس" else "Time for calm reading in ATLAS"
        }
      val body =
        when {
          goalMet ->
            if (isFa) "آفرین! هر زمان خواستید می‌توانید چند صفحه دیگر در اطلس بخوانید."
            else "Great work today. Open ATLAS whenever you feel like a few quiet pages."
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
          .setSmallIcon(R.drawable.ic_notification_book)
          .setColor(ATLAS_BLUE_ARGB)
          .setContentTitle(title)
          .setContentText(body)
          .setStyle(NotificationCompat.BigTextStyle().bigText(body))
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setCategory(NotificationCompat.CATEGORY_REMINDER)
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
  const val ACTION_STREAK_REMINDER = "atlas.mrmabrosia.ir.ACTION_STREAK_REMINDER"
  private const val WORK_NAME = "atlas_daily_streak_reminder"
  private const val ALARM_REQUEST_CODE = 4109

  fun computeNextTriggerMillis(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val target =
      Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    // If the chosen time is in the past or within the last/next second, roll cleanly to tomorrow
    if (target.timeInMillis <= nowMillis + 1_000L) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
  }

  fun computeDelayMillisUntil(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val triggerAt = computeNextTriggerMillis(hour, minute, nowMillis)
    return (triggerAt - nowMillis).coerceAtLeast(1_000L)
  }

  private fun buildAlarmPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, StreakReminderReceiver::class.java).apply {
        action = ACTION_STREAK_REMINDER
      }
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  fun scheduleDaily(
    context: Context,
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val appContext = context.applicationContext
    StreakReminderWorker.ensureChannel(appContext)
    val triggerAtMillis = computeNextTriggerMillis(hour, minute, nowMillis)
    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val pendingIntent = buildAlarmPendingIntent(appContext)

    if (alarmManager != null) {
      alarmManager.cancel(pendingIntent)
      runCatching {
        val canExact =
          Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        when {
          canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            alarmManager.setExactAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent,
            )
          canExact ->
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
            alarmManager.setAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent,
            )
          else ->
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      }
    }

    // Cancel legacy WorkManager unique work so it cannot fire duplicate notifications
    runCatching {
      WorkManager.getInstance(appContext).cancelUniqueWork("pagenest_daily_streak_reminder")
      WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }
  }

  fun cancel(context: Context) {
    val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val pendingIntent = buildAlarmPendingIntent(appContext)
    alarmManager?.cancel(pendingIntent)
    runCatching {
      WorkManager.getInstance(appContext).cancelUniqueWork("pagenest_daily_streak_reminder")
      WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }
  }

  fun triggerTestNow(context: Context) {
    val appContext = context.applicationContext
    StreakReminderWorker.ensureChannel(appContext)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      StreakReminderWorker.executeReminderCheck(appContext, isTest = true)
    }
  }
}
