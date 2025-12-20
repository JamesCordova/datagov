package com.example.datagov.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.datagov.MainActivity
import com.example.datagov.R
import kotlinx.coroutines.*

class TimerService : Service() {

    companion object {
        private const val TAG = "TimerService"
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 2001
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_IS_RUNNING = "is_running"

        const val ACTION_START = "com.example.datagov.ACTION_START"
        const val ACTION_PAUSE = "com.example.datagov.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.datagov.ACTION_RESUME"
        const val ACTION_STOP = "com.example.datagov.ACTION_STOP"
        
        const val BROADCAST_TIMER_UPDATE = "com.example.datagov.TIMER_UPDATE"
        const val EXTRA_ELAPSED_TIME = "elapsed_time"
        const val EXTRA_IS_RUNNING = "is_running"

        // Función para verificar si el servicio está activo
        fun isServiceRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_RUNNING, false)
        }
    }

    private var elapsedSeconds = 0
    private var isRunning = false
    private var isPaused = false
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente del temporizador"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun startTimer() {
        if (isRunning) {
            Log.d(TAG, "Timer already running")
            return
        }
        
        Log.d(TAG, "Starting timer")
        isRunning = true
        isPaused = false
        elapsedSeconds = 0
        
        // Guardar estado en SharedPreferences
        saveServiceState(true)

        startForeground(NOTIFICATION_ID, createNotification())
        startTimerJob()
        broadcastUpdate()
    }

    private fun pauseTimer() {
        if (!isRunning || isPaused) {
            Log.d(TAG, "Timer not running or already paused")
            return
        }
        
        Log.d(TAG, "Pausing timer at $elapsedSeconds seconds")
        isPaused = true
        timerJob?.cancel()
        updateNotification()
        broadcastUpdate()
    }

    private fun resumeTimer() {
        if (!isRunning || !isPaused) {
            Log.d(TAG, "Timer not paused")
            return
        }
        
        Log.d(TAG, "Resuming timer from $elapsedSeconds seconds")
        isPaused = false
        startTimerJob()
        broadcastUpdate()
    }

    private fun stopTimer() {
        Log.d(TAG, "Stopping timer")
        isRunning = false
        isPaused = false
        timerJob?.cancel()

        // Guardar estado en SharedPreferences
        saveServiceState(false)

        // Enviar broadcast primero
        broadcastUpdate()

        // Detener el servicio después de un pequeño delay para asegurar que el broadcast llegue
        serviceScope.launch {
            delay(100) // 100ms de delay
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && isRunning && !isPaused) {
                delay(1000)
                elapsedSeconds++
                updateNotification()
                broadcastUpdate()
            }
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Temporizador")
        .setContentText(formatTime(elapsedSeconds))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setContentIntent(createContentIntent())
        .addAction(createPauseAction())
        .addAction(createStopAction())
        .build()

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Temporizador ${if (isPaused) "(Pausado)" else ""}")
            .setContentText(formatTime(elapsedSeconds))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(createContentIntent())
            .apply {
                if (isPaused) {
                    addAction(createResumeAction())
                } else {
                    addAction(createPauseAction())
                }
                addAction(createStopAction())
            }
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPauseAction(): NotificationCompat.Action {
        val pauseIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            0,
            "Pausar",
            pausePendingIntent
        ).build()
    }

    private fun createResumeAction(): NotificationCompat.Action {
        val resumeIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            resumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            0,
            "Reanudar",
            resumePendingIntent
        ).build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            0,
            "Detener",
            stopPendingIntent
        ).build()
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun broadcastUpdate() {
        val intent = Intent(BROADCAST_TIMER_UPDATE).apply {
            putExtra(EXTRA_ELAPSED_TIME, elapsedSeconds)
            putExtra(EXTRA_IS_RUNNING, isRunning && !isPaused)
        }
        sendBroadcast(intent)
    }

    private fun saveServiceState(running: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_RUNNING, running).apply()
        Log.d(TAG, "Service state saved: running=$running")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        // Asegurar que el estado se marca como no corriendo
        isRunning = false
        isPaused = false

        // Guardar estado y enviar broadcast final
        saveServiceState(false)
        broadcastUpdate()

        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}

