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
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "Worker iniciado - Verificando nuevos proyectos")
        Log.d(TAG, "═══════════════════════════════════════════════════")

        return try {
            // 1. Obtener el último proyecto de Firebase
            Log.d(TAG, "PASO 1: Consultando Firebase...")
            val latestProject = getLatestProjectFromFirebase()

            if (latestProject == null) {
                Log.w(TAG, "❌ No hay proyectos en Firebase o error de conexión")
                return Result.success()
            }

            Log.d(TAG, "✅ Último proyecto obtenido:")
            Log.d(TAG, "   - ID: ${latestProject.id}")
            Log.d(TAG, "   - Nombre: ${latestProject.name}")
            Log.d(TAG, "   - Ubicación: ${latestProject.ubicacion}")
            Log.d(TAG, "   - createdAt: ${latestProject.createdAt}")

            // 2. Obtener el último proyecto notificado
            Log.d(TAG, "PASO 2: Leyendo almacenamiento local...")
            val preferences = ProjectPreferences(applicationContext)
            val lastNotifiedId = preferences.getLastNotifiedProjectId()

            Log.d(TAG, "✅ Último proyecto notificado guardado: $lastNotifiedId")

            // 3. Comparar y notificar si es nuevo
            Log.d(TAG, "PASO 3: Comparando IDs...")
            Log.d(TAG, "   Firebase ID: '${latestProject.id}'")
            Log.d(TAG, "   Local ID:    '$lastNotifiedId'")

            if (latestProject.id != lastNotifiedId) {
                Log.d(TAG, "🔔 ¡NUEVO PROYECTO DETECTADO! Los IDs son diferentes")
                Log.d(TAG, "PASO 4: Enviando notificación...")
                showNotification(latestProject)
                Log.d(TAG, "PASO 5: Guardando nuevo ID en SharedPreferences...")
                preferences.saveLastNotifiedProject(latestProject.id, latestProject.createdAt)
                Log.d(TAG, "✅ Notificación enviada y ID guardado exitosamente")
            } else {
                Log.d(TAG, "ℹ️ No hay proyectos nuevos - Los IDs son iguales")
            }

            Log.d(TAG, "═══════════════════════════════════════════════════")
            Log.d(TAG, "Worker finalizado exitosamente")
            Log.d(TAG, "═══════════════════════════════════════════════════")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR al verificar proyectos: ${e.message}", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun getLatestProjectFromFirebase(): Project? = suspendCancellableCoroutine { continuation ->
        val database = FirebaseDatabase.getInstance()
        val projectsRef = database.getReference("Projects")

        Log.d(TAG, "   Consultando: database.child('Projects').orderByChild('createdAt').limitToLast(1)")

        // Consultar el último proyecto ordenado por createdAt
        projectsRef.orderByChild("createdAt")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d(TAG, "   Respuesta de Firebase recibida")
                        Log.d(TAG, "   Cantidad de proyectos en respuesta: ${snapshot.childrenCount}")

                        var latestProject: Project? = null
                        var projectCount = 0

                        for (projectSnapshot in snapshot.children) {
                            projectCount++
                            val id = projectSnapshot.key ?: ""
                            val name = projectSnapshot.child("name").getValue(String::class.java) ?: ""
                            val ubicacion = projectSnapshot.child("ubicacion").getValue(String::class.java) ?: ""
                            val description = projectSnapshot.child("description").getValue(String::class.java) ?: ""
                            val picUrl = projectSnapshot.child("picUrl").getValue(String::class.java) ?: ""

                            // Manejar categoryId que puede venir como String o Number
                            val categoryIdRaw = projectSnapshot.child("categoryId").value
                            val categoryId = when (categoryIdRaw) {
                                is String -> categoryIdRaw
                                is Long -> categoryIdRaw.toString()
                                is Int -> categoryIdRaw.toString()
                                else -> "0"
                            }

                            val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            // Manejar presupuesto
                            val presupuestoRaw = projectSnapshot.child("presupuesto").value
                            val presupuesto = when (presupuestoRaw) {
                                is Long -> presupuestoRaw
                                is Int -> presupuestoRaw.toLong()
                                is String -> presupuestoRaw.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            // Manejar avance
                            val avanceRaw = projectSnapshot.child("avance").value
                            val avance = when (avanceRaw) {
                                is Int -> avanceRaw
                                is Long -> avanceRaw.toInt()
                                is String -> avanceRaw.toIntOrNull() ?: 0
                                else -> 0
                            }

                            Log.d(TAG, "   Proyecto #$projectCount encontrado:")
                            Log.d(TAG, "      ID: $id")
                            Log.d(TAG, "      name: $name")
                            Log.d(TAG, "      createdAt: $createdAt")

                            latestProject = Project(
                                id = id,
                                name = name,
                                ubicacion = ubicacion,
                                categoryId = categoryId,
                                createdAt = createdAt,
                                description = description,
                                presupuesto = presupuesto,
                                avance = avance,
                                picUrl = picUrl
                            )
                        }

                        if (latestProject == null) {
                            Log.w(TAG, "   ⚠️ No se encontró ningún proyecto en Firebase")
                        }

                        if (continuation.isActive) {
                            continuation.resume(latestProject)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "   ❌ Error al parsear proyecto: ${e.message}", e)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "   ❌ Error de Firebase: ${error.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            })
    }

    private fun showNotification(project: Project) {
        Log.d(TAG, "   → Creando notificación para: ${project.name}")

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "   → Creando canal de notificación (Android 8+)")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nuevos Proyectos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de nuevos proyectos gubernamentales"
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "   → Canal creado: $CHANNEL_ID")
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

        Log.d(TAG, "   → Mostrando notificación con ID: $NOTIFICATION_ID")
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "   ✅ Notificación enviada exitosamente")
    }
}

