# Código Explicado Paso a Paso 📖

## Estructura del Worker

```kotlin
class CheckNewProjectsWorker : CoroutineWorker {
    
    // Método principal que se ejecuta automáticamente
    override suspend fun doWork(): Result {
        
        // ╔════════════════════════════════════════╗
        // ║         PASO 1: Obtener de Firebase    ║
        // ╚════════════════════════════════════════╝
        
        val latestProject = getLatestProjectFromFirebase()
        // ↑ Llama a Firebase y obtiene el último proyecto
        // Retorna: Project o null
        
        if (latestProject == null) {
            // No hay proyectos → Salir
            return Result.success()
        }
        
        // ╔════════════════════════════════════════╗
        // ║    PASO 2: Leer almacenamiento local   ║
        // ╚════════════════════════════════════════╝
        
        val preferences = ProjectPreferences(applicationContext)
        val lastNotifiedId = preferences.getLastNotifiedProjectId()
        // ↑ Lee SharedPreferences
        // Retorna: "proj123" o null
        
        // ╔════════════════════════════════════════╗
        // ║       PASO 3: Comparar IDs             ║
        // ╚════════════════════════════════════════╝
        
        if (latestProject.id != lastNotifiedId) {
            // SON DIFERENTES → ¡Es nuevo!
            
            // ╔════════════════════════════════════════╗
            // ║      PASO 4: Enviar notificación       ║
            // ╚════════════════════════════════════════╝
            
            showNotification(latestProject)
            
            // ╔════════════════════════════════════════╗
            // ║      PASO 5: Guardar nuevo ID          ║
            // ╚════════════════════════════════════════╝
            
            preferences.saveLastNotifiedProject(
                latestProject.id, 
                latestProject.createdAt
            )
        } else {
            // SON IGUALES → Ya lo conocemos
            Log.d(TAG, "No hay proyectos nuevos")
        }
        
        return Result.success()
    }
}
```

---

## PASO 1: Obtener de Firebase (Detallado) 🔥

```kotlin
private suspend fun getLatestProjectFromFirebase(): Project? {
    
    // 1.1 Obtener referencia a la base de datos
    val database = FirebaseDatabase.getInstance()
    val projectsRef = database.getReference("Projects")
    //                                         ↑
    //                     Apunta a la colección "Projects"
    
    // 1.2 Hacer la consulta
    projectsRef
        .orderByChild("createdAt")  // ← Ordenar por fecha de creación
        .limitToLast(1)            // ← Solo el último (más reciente)
        .addListenerForSingleValueEvent(...)
        //        ↑
        //  Obtener UNA VEZ (no escuchar cambios continuos)
    
    // 1.3 Procesar respuesta
    for (projectSnapshot in snapshot.children) {
        val id = projectSnapshot.key           // Obtener ID
        val name = projectSnapshot.child("name").getValue(String::class.java)
        val ubicacion = projectSnapshot.child("ubicacion").getValue(String::class.java)
        val categoryId = projectSnapshot.child("categoryId").getValue(String::class.java)
        val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java)
        
        // 1.4 Crear objeto Project
        latestProject = Project(
            id = id,
            name = name,
            ubicacion = ubicacion,
            categoryId = categoryId,
            createdAt = createdAt
        )
    }
    
    return latestProject  // Retornar el proyecto encontrado
}
```

### Ejemplo Visual de la Consulta:

**Firebase tiene:**
```json
Projects: {
  "proj001": {
    "name": "Parque",
    "createdAt": 1000
  },
  "proj002": {
    "name": "Hospital",
    "createdAt": 2000
  },
  "proj003": {
    "name": "Escuela",
    "createdAt": 3000  ← ESTE es el más reciente
  }
}
```

**La consulta retorna:**
```kotlin
Project(
  id = "proj003",
  name = "Escuela",
  createdAt = 3000
)
```

---

## PASO 2: Leer Almacenamiento Local 💾

```kotlin
// Archivo: ProjectPreferences.kt

class ProjectPreferences(context: Context) {
    
    // SharedPreferences es como un archivo XML local
    private val prefs: SharedPreferences =
        context.getSharedPreferences("project_prefs", Context.MODE_PRIVATE)
    
    // Leer el último ID guardado
    fun getLastNotifiedProjectId(): String? {
        return prefs.getString("last_notified_project_id", null)
        //                       ↑                          ↑
        //                    Clave                   Valor default
    }
    
    // Guardar nuevo ID
    fun saveLastNotifiedProject(projectId: String, timestamp: Long) {
        prefs.edit().apply {
            putString("last_notified_project_id", projectId)
            putLong("last_notified_timestamp", timestamp)
            apply()  // ← Guardar cambios
        }
    }
}
```

### Ejemplo Visual del Almacenamiento:

**Archivo: /data/data/com.example.datagov/shared_prefs/project_prefs.xml**
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="last_notified_project_id">proj003</string>
    <long name="last_notified_timestamp" value="3000" />
</map>
```

---

## PASO 3: Comparación (Lógica Detallada) 🔄

```kotlin
// Valores obtenidos:
val latestProject.id = "proj004"      // De Firebase (PASO 1)
val lastNotifiedId = "proj003"        // De local (PASO 2)

// Comparación:
if (latestProject.id != lastNotifiedId) {
    //  "proj004"     !=  "proj003"
    //        ↓              ↓
    //      NUEVO         VIEJO
    //                ↓
    //         ¡SON DIFERENTES!
    //                ↓
    //       ENVIAR NOTIFICACIÓN
}
```

### Tabla de Casos:

| Firebase ID | Local ID  | ¿Son diferentes? | Acción            |
|-------------|-----------|------------------|-------------------|
| proj004     | proj003   | ✅ SÍ           | Notificar         |
| proj003     | proj003   | ❌ NO           | No hacer nada     |
| proj005     | null      | ✅ SÍ           | Notificar (1ra vez)|
| null        | proj003   | N/A              | Salir (sin datos) |

---

## PASO 4: Crear y Mostrar Notificación 🔔

```kotlin
private fun showNotification(project: Project) {
    
    // 4.1 Obtener el servicio de notificaciones
    val notificationManager = 
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
        as NotificationManager
    
    // 4.2 Crear canal (necesario en Android 8+)
    val channel = NotificationChannel(
        "new_projects_channel",        // ID único
        "Nuevos Proyectos",            // Nombre visible
        NotificationManager.IMPORTANCE_DEFAULT
    )
    notificationManager.createNotificationChannel(channel)
    
    // 4.3 Crear Intent (qué pasa al tocar la notificación)
    val intent = Intent(applicationContext, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(...)
    //                        ↑
    //              Abrirá MainActivity al tocar
    
    // 4.4 Construir la notificación
    val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)  // ← Icono
        .setContentTitle("Nuevo proyecto disponible")     // ← Título
        .setContentText("Se ha agregado: ${project.name}")// ← Texto corto
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("Nombre: ${project.name}\nUbicación: ${project.ubicacion}")
        )  // ← Texto expandido
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)   // ← Acción al tocar
        .setAutoCancel(true)               // ← Se elimina al tocar
        .build()
    
    // 4.5 Mostrar la notificación
    notificationManager.notify(1001, notification)
    //                          ↑
    //                    ID de la notificación
}
```

### Ejemplo Visual de la Notificación:

```
┌──────────────────────────────────────────┐
│ 🔔 Nuevo proyecto disponible            │← Título
├──────────────────────────────────────────┤
│ Se ha agregado un nuevo proyecto del    │← Texto
│ gobierno: Biblioteca Municipal           │
│                                          │
│ Ubicación: Miraflores                   │← Expandido
├──────────────────────────────────────────┤
│                                    [×]   │← Botón cerrar
└──────────────────────────────────────────┘
        ↓ (al tocar)
   Abre la app
```

---

## PASO 5: Guardar Nuevo Estado 💾

```kotlin
preferences.saveLastNotifiedProject(latestProject.id, latestProject.createdAt)
//                                   ↓                  ↓
//                              "proj004"           1702483200000

// Esto actualiza SharedPreferences:
// ANTES:
// last_notified_project_id: "proj003"
// last_notified_timestamp: 3000

// DESPUÉS:
// last_notified_project_id: "proj004"  ← Actualizado
// last_notified_timestamp: 1702483200000  ← Actualizado
```

---

## Flujo Completo con Código Real 🎯

### Situación: Hay un Proyecto Nuevo

```kotlin
// ═══════════════════════════════════════════════════════════
// EJECUCIÓN DEL WORKER
// ═══════════════════════════════════════════════════════════

override suspend fun doWork(): Result {
    Log.d(TAG, "Worker iniciado - Verificando nuevos proyectos")
    
    // ────────────────────────────────────────────────────────
    // PASO 1: Consultar Firebase
    // ────────────────────────────────────────────────────────
    
    val latestProject = getLatestProjectFromFirebase()
    // Firebase retorna:
    // Project(id="proj004", name="Biblioteca", createdAt=1702483200000)
    
    if (latestProject == null) {
        return Result.success()  // No llegaríamos aquí
    }
    
    Log.d(TAG, "Último proyecto obtenido: Biblioteca (ID: proj004)")
    
    // ────────────────────────────────────────────────────────
    // PASO 2: Leer almacenamiento local
    // ────────────────────────────────────────────────────────
    
    val preferences = ProjectPreferences(applicationContext)
    val lastNotifiedId = preferences.getLastNotifiedProjectId()
    // SharedPreferences retorna: "proj003"
    
    Log.d(TAG, "Último proyecto notificado: proj003")
    
    // ────────────────────────────────────────────────────────
    // PASO 3: Comparar
    // ────────────────────────────────────────────────────────
    
    if (latestProject.id != lastNotifiedId) {
        // "proj004" != "proj003" → TRUE
        
        Log.d(TAG, "¡Nuevo proyecto detectado! Enviando notificación")
        
        // ────────────────────────────────────────────────────
        // PASO 4: Notificar
        // ────────────────────────────────────────────────────
        
        showNotification(latestProject)
        // Se crea y muestra la notificación
        
        Log.d(TAG, "Notificación enviada")
        
        // ────────────────────────────────────────────────────
        // PASO 5: Guardar nuevo estado
        // ────────────────────────────────────────────────────
        
        preferences.saveLastNotifiedProject("proj004", 1702483200000)
        // SharedPreferences ahora tiene: "proj004"
    } else {
        Log.d(TAG, "No hay proyectos nuevos")
        // No llegaríamos aquí en este caso
    }
    
    return Result.success()
}
```

---

## Resumen del Procedimiento 📋

```
┌─────────────────────────────────────────────────────────────┐
│                    INICIO DEL WORKER                        │
└────────────────────────┬────────────────────────────────────┘
                         ↓
        ╔════════════════════════════════════════╗
        ║  1. Consultar Firebase                 ║
        ║     → Obtener último proyecto         ║
        ║     → Ordenar por createdAt desc       ║
        ║     → Límite 1                         ║
        ╚═══════════════════╦════════════════════╝
                            ↓
                    Retorna: Project
                            ↓
        ╔════════════════════════════════════════╗
        ║  2. Leer SharedPreferences             ║
        ║     → Key: last_notified_project_id    ║
        ║     → Obtener último ID guardado       ║
        ╚═══════════════════╦════════════════════╝
                            ↓
                Retorna: String o null
                            ↓
        ╔════════════════════════════════════════╗
        ║  3. Comparar IDs                       ║
        ║     Firebase ID vs Local ID            ║
        ╚═══════════════════╦════════════════════╝
                            ↓
                  ┌─────────┴─────────┐
                  │   ¿Diferentes?    │
                  └─────┬───────┬─────┘
                       SÍ      NO
                        ↓       ↓
        ╔═══════════════════╗  Log: "No hay nuevos"
        ║ 4. Notificación   ║           ↓
        ║    → Crear        ║       Finalizar
        ║    → Mostrar      ║
        ╚═══════╦═══════════╝
                ↓
        ╔═══════════════════╗
        ║ 5. Guardar Estado ║
        ║    → Update ID    ║
        ║    → Update time  ║
        ╚═══════╦═══════════╝
                ↓
        ┌───────────────────┐
        │  Result.success() │
        │  (Finalizar)      │
        └───────────────────┘
```

---

## Archivos Involucrados 📁

```
DataGov/
├── app/src/main/java/com/example/datagov/
│   ├── workers/
│   │   ├── CheckNewProjectsWorker.kt     ← Hace la verificación
│   │   └── WorkManagerScheduler.kt       ← Programa el Worker
│   ├── data/
│   │   ├── ProjectPreferences.kt         ← Guarda/lee IDs
│   │   └── Project.kt                    ← Modelo de datos
│   └── MainActivity.kt                   ← Inicia WorkManager
│
├── Firebase Realtime Database            ← Base de datos remota
│   └── Projects/                         ← Colección
│       ├── proj001/
│       ├── proj002/
│       └── proj003/
│
└── Almacenamiento Local (Dispositivo)
    └── shared_prefs/
        └── project_prefs.xml             ← IDs guardados
```

---

## Comandos para Debugging 🐛

### Ver el archivo de preferencias:
```bash
adb shell run-as com.example.datagov cat shared_prefs/project_prefs.xml
```

### Ver logs en tiempo real:
```bash
adb logcat -s CheckNewProjectsWorker:D WorkManagerScheduler:D
```

### Limpiar estado (forzar "primera vez"):
```bash
adb shell pm clear com.example.datagov
```

---

¿Te quedó claro el procedimiento? 🎯

