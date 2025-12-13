package com.example.datagov.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.datagov.MainActivity
import com.example.datagov.R
import com.example.datagov.data.Project
import com.example.datagov.data.ProjectPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CheckNewProjectsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CheckNewProjectsWorker"
        private const val CHANNEL_ID = "new_projects_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker iniciado - Verificando nuevos proyectos")

        return try {
            // 1. Obtener el último proyecto de Firebase
            val latestProject = getLatestProjectFromFirebase()

            if (latestProject == null) {
                Log.d(TAG, "No hay proyectos en Firebase")
                return Result.success()
            }

            Log.d(TAG, "Último proyecto obtenido: ${latestProject.name} (ID: ${latestProject.id})")

            // 2. Obtener el último proyecto notificado
            val preferences = ProjectPreferences(applicationContext)
            val lastNotifiedId = preferences.getLastNotifiedProjectId()

            Log.d(TAG, "Último proyecto notificado: $lastNotifiedId")

            // 3. Comparar y notificar si es nuevo
            if (latestProject.id != lastNotifiedId) {
                Log.d(TAG, "¡Nuevo proyecto detectado! Enviando notificación")
                showNotification(latestProject)
                preferences.saveLastNotifiedProject(latestProject.id, latestProject.createdAt)
            } else {
                Log.d(TAG, "No hay proyectos nuevos")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar proyectos: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun getLatestProjectFromFirebase(): Project? = suspendCancellableCoroutine { continuation ->
        val database = FirebaseDatabase.getInstance()
        val projectsRef = database.getReference("Projects")

        // Consultar el último proyecto ordenado por createdAt
        projectsRef.orderByChild("createdAt")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        var latestProject: Project? = null

                        for (projectSnapshot in snapshot.children) {
                            val id = projectSnapshot.key ?: ""
                            val name = projectSnapshot.child("name").getValue(String::class.java) ?: ""
                            val ubicacion = projectSnapshot.child("ubicacion").getValue(String::class.java) ?: ""
                            val categoryId = projectSnapshot.child("categoryId").getValue(String::class.java) ?: ""
                            val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            latestProject = Project(
                                id = id,
                                name = name,
                                ubicacion = ubicacion,
                                categoryId = categoryId,
                                createdAt = createdAt
                            )
                        }

                        if (continuation.isActive) {
                            continuation.resume(latestProject)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al parsear proyecto: ${e.message}", e)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error de Firebase: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            })
    }

    private fun showNotification(project: Project) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nuevos Proyectos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de nuevos proyectos gubernamentales"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Crear la notificación
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nuevo proyecto disponible")
            .setContentText("Se ha agregado un nuevo proyecto del gobierno: ${project.name}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Se ha agregado un nuevo proyecto del gobierno:\n\n${project.name}\n\nUbicación: ${project.ubicacion}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notificación enviada")
    }
}

