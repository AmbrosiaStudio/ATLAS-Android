package com.example.engine.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.widget.PageNestContinueWidget

interface TtsMediaCallbacks {
  fun onMediaPlay()
  fun onMediaPause()
  fun onMediaNext()
  fun onMediaPrevious()
  fun onMediaStop()
}

/**
 * Bridge connecting the foreground MediaSession service with the active ReaderViewModel.
 */
object TtsMediaBridge {
  @Volatile var callbacks: TtsMediaCallbacks? = null
  @Volatile var bookTitle: String = "PageNest"
  @Volatile var chapterTitle: String = "Listen Mode"
  @Volatile var isPlaying: Boolean = false
  @Volatile var bookId: String? = null
  @Volatile var serviceRunning: Boolean = false

  fun syncState(
    context: Context,
    bookId: String?,
    bookTitle: String,
    chapterTitle: String,
    isPlaying: Boolean,
  ) {
    this.bookId = bookId
    this.bookTitle = bookTitle.ifBlank { "PageNest" }
    this.chapterTitle = chapterTitle.ifBlank { "Listen Mode" }
    this.isPlaying = isPlaying

    if (isPlaying) {
      val intent = Intent(context, TtsPlaybackService::class.java).setAction(TtsPlaybackService.ACTION_SYNC)
      if (!serviceRunning) {
        serviceRunning = true
        runCatching { ContextCompat.startForegroundService(context, intent) }
      } else {
        runCatching { context.startService(intent) }
      }
    } else if (serviceRunning) {
      val intent = Intent(context, TtsPlaybackService::class.java).setAction(TtsPlaybackService.ACTION_SYNC)
      runCatching { context.startService(intent) }
    }
  }

  fun stopPlaybackService(context: Context) {
    isPlaying = false
    if (!serviceRunning) return
    serviceRunning = false
    runCatching { context.stopService(Intent(context, TtsPlaybackService::class.java)) }
  }
}

/**
 * Foreground MediaSession service providing Lock-Screen Media Controls, notification
 * shade playback buttons, and Bluetooth headset/earbud media key routing for TTS Listen Mode.
 */
class TtsPlaybackService : Service() {

  private var mediaSession: MediaSession? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()
    mediaSession =
      MediaSession(this, "PageNestListenSession").apply {
        setCallback(
          object : MediaSession.Callback() {
            override fun onPlay() {
              TtsMediaBridge.callbacks?.onMediaPlay()
            }

            override fun onPause() {
              TtsMediaBridge.callbacks?.onMediaPause()
            }

            override fun onSkipToNext() {
              TtsMediaBridge.callbacks?.onMediaNext()
            }

            override fun onSkipToPrevious() {
              TtsMediaBridge.callbacks?.onMediaPrevious()
            }

            override fun onStop() {
              TtsMediaBridge.callbacks?.onMediaStop()
              stopForegroundAndSelf()
            }
          }
        )
        isActive = true
      }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    TtsMediaBridge.serviceRunning = true
    when (intent?.action) {
      ACTION_PLAY -> TtsMediaBridge.callbacks?.onMediaPlay()
      ACTION_PAUSE -> TtsMediaBridge.callbacks?.onMediaPause()
      ACTION_NEXT -> TtsMediaBridge.callbacks?.onMediaNext()
      ACTION_PREV -> TtsMediaBridge.callbacks?.onMediaPrevious()
      ACTION_STOP -> {
        TtsMediaBridge.callbacks?.onMediaStop()
        stopForegroundAndSelf()
        return START_NOT_STICKY
      }
    }
    publishNotification()
    return START_NOT_STICKY
  }

  private fun publishNotification() {
    val isPlaying = TtsMediaBridge.isPlaying
    val title = TtsMediaBridge.bookTitle
    val subtitle = TtsMediaBridge.chapterTitle

    val stateActions =
      PlaybackState.ACTION_PLAY or
        PlaybackState.ACTION_PAUSE or
        PlaybackState.ACTION_PLAY_PAUSE or
        PlaybackState.ACTION_SKIP_TO_NEXT or
        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
        PlaybackState.ACTION_STOP

    val playbackState =
      PlaybackState.Builder()
        .setActions(stateActions)
        .setState(
          if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
          PlaybackState.PLAYBACK_POSITION_UNKNOWN,
          1.0f,
        )
        .build()
    mediaSession?.setPlaybackState(playbackState)

    val metadata =
      MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
        .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
        .build()
    mediaSession?.setMetadata(metadata)

    val openIntent =
      Intent(this, MainActivity::class.java).apply {
        this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        TtsMediaBridge.bookId?.let { putExtra(PageNestContinueWidget.EXTRA_OPEN_BOOK_ID, it) }
      }
    val contentPendingIntent =
      PendingIntent.getActivity(
        this,
        410,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val prevAction =
      Notification.Action.Builder(
        android.R.drawable.ic_media_previous,
        "Previous",
        serviceActionPendingIntent(ACTION_PREV, 411),
      ).build()

    val toggleAction =
      if (isPlaying) {
        Notification.Action.Builder(
          android.R.drawable.ic_media_pause,
          "Pause",
          serviceActionPendingIntent(ACTION_PAUSE, 412),
        ).build()
      } else {
        Notification.Action.Builder(
          android.R.drawable.ic_media_play,
          "Play",
          serviceActionPendingIntent(ACTION_PLAY, 413),
        ).build()
      }

    val nextAction =
      Notification.Action.Builder(
        android.R.drawable.ic_media_next,
        "Next",
        serviceActionPendingIntent(ACTION_NEXT, 414),
      ).build()

    val stopAction =
      Notification.Action.Builder(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Stop",
        serviceActionPendingIntent(ACTION_STOP, 415),
      ).build()

    val builder =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, CHANNEL_ID)
      } else {
        @Suppress("DEPRECATION")
        Notification.Builder(this)
      }

    val notification =
      builder
        .setSmallIcon(R.drawable.ic_notification_book)
        .setContentTitle(title)
        .setContentText(subtitle)
        .setContentIntent(contentPendingIntent)
        .setOngoing(isPlaying)
        .setOnlyAlertOnce(true)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .addAction(prevAction)
        .addAction(toggleAction)
        .addAction(nextAction)
        .addAction(stopAction)
        .setStyle(
          Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
        )
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, TtsPlaybackService::class.java).setAction(action)
    return PendingIntent.getService(
      this,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "Read Aloud Playback",
            NotificationManager.IMPORTANCE_LOW,
          ).apply {
            description = "Lock-screen and headset controls for PageNest Listen Mode"
            setShowBadge(false)
          }
        manager?.createNotificationChannel(channel)
      }
    }
  }

  private fun stopForegroundAndSelf() {
    TtsMediaBridge.serviceRunning = false
    TtsMediaBridge.isPlaying = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
    stopSelf()
  }

  override fun onDestroy() {
    TtsMediaBridge.serviceRunning = false
    mediaSession?.isActive = false
    mediaSession?.release()
    mediaSession = null
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val CHANNEL_ID = "pagenest_listen_playback"
    const val NOTIFICATION_ID = 3201
    const val ACTION_SYNC = "ir.pagenest.mrambrosia.action.TTS_SYNC"
    const val ACTION_PLAY = "ir.pagenest.mrambrosia.action.TTS_PLAY"
    const val ACTION_PAUSE = "ir.pagenest.mrambrosia.action.TTS_PAUSE"
    const val ACTION_NEXT = "ir.pagenest.mrambrosia.action.TTS_NEXT"
    const val ACTION_PREV = "ir.pagenest.mrambrosia.action.TTS_PREV"
    const val ACTION_STOP = "ir.pagenest.mrambrosia.action.TTS_STOP"
  }
}
