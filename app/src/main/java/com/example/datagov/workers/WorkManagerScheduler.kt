package com.example.datagov.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    private const val TAG = "WorkManagerScheduler"
    private const val WORK_NAME = "check_new_projects_work"

    // Intervalo de verificación (en horas)
    private const val REPEAT_INTERVAL_HOURS = 1L

    fun schedulePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requiere conexión a internet
            .build()

        val workRequest = PeriodicWorkRequestBuilder<CheckNewProjectsWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES) // Primera ejecución después de 15 minutos
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene el trabajo si ya existe
            workRequest
        )

        Log.d(TAG, "WorkManager programado para verificar nuevos proyectos cada $REPEAT_INTERVAL_HOURS hora(s)")
    }

    fun cancelWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "WorkManager cancelado")
    }

    // Método para ejecutar el trabajo inmediatamente (útil para pruebas)
    fun executeNow(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<CheckNewProjectsWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "WorkManager ejecutado inmediatamente")
    }
}

