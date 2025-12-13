package com.example.datagov.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import androidx.work.Constraints
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    private const val TAG = "WorkManagerScheduler"
    private const val WORK_NAME = "check_new_projects_work"

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE FRECUENCIA DE VERIFICACIÓN
    // ═══════════════════════════════════════════════════════════════
    //
    // Opciones disponibles (elige UNA):
    //
    // TESTING/DESARROLLO (más frecuente):
    // private const val REPEAT_INTERVAL_HOURS = 1L      // Cada 1 hora
    //
    // PRODUCCIÓN (recomendado):
    // private const val REPEAT_INTERVAL_HOURS = 3L      // Cada 3 horas
    // private const val REPEAT_INTERVAL_HOURS = 6L      // Cada 6 horas
    // private const val REPEAT_INTERVAL_HOURS = 12L     // Cada 12 horas (2 veces al día)
    // private const val REPEAT_INTERVAL_HOURS = 24L     // Cada 24 horas (1 vez al día)
    //
    // NOTA: Android WorkManager tiene un intervalo MÍNIMO de 15 minutos
    //       pero para verificaciones periódicas se recomienda al menos 1 hora
    // ═══════════════════════════════════════════════════════════════

    private const val REPEAT_INTERVAL_HOURS = 1L  // ← CAMBIA ESTE VALOR según necesites

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

