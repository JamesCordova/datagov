# Procedimiento de Detección de Nuevos Proyectos 🔍

## Flujo Completo del Sistema

### 1. Inicio del Proceso ⏰

```
App se abre → WorkManager se registra → Espera 15 minutos → Primera ejecución
                                              ↓
                                    Luego cada 1 hora se repite
```

### 2. El Worker se Ejecuta 🚀

```kotlin
CheckNewProjectsWorker.doWork() {
    1. Log: "Worker iniciado - Verificando nuevos proyectos"
    2. Se conecta a Firebase
    3. Consulta la base de datos
    4. Obtiene el último proyecto
    5. Compara con el registro local
    6. Decide si notificar o no
}
```

---

## Procedimiento Paso a Paso 📋

### PASO 1: Consultar Firebase 🔥

```kotlin
Firebase Realtime Database
    ↓
Colección: "Projects"
    ↓
Ordena por: "createdAt" (descendente)
    ↓
Límite: 1 documento (el más reciente)
    ↓
Obtiene: El último proyecto agregado
```

**Código equivalente:**
```kotlin
database.child("Projects")
    .orderByChild("createdAt")
    .limitToLast(1)
    .get()
```

**Resultado:**
```json
{
  "proj123": {
    "id": "proj123",
    "name": "Construcción de Parque",
    "ubicacion": "Lima",
    "categoryId": "cat1",
    "createdAt": 1702483200000
  }
}
```

---

### PASO 2: Leer el Registro Local 💾

El sistema guarda localmente (SharedPreferences) cuál fue el último proyecto del que ya notificó:

```kotlin
ProjectPreferences.getLastNotifiedProjectId()
    ↓
Lee SharedPreferences
    ↓
Key: "last_notified_project_id"
    ↓
Valor guardado: "proj123" (o null si es primera vez)
```

**Ubicación del archivo:**
```
/data/data/com.example.datagov/shared_prefs/project_prefs.xml
```

**Contenido:**
```xml
<map>
    <string name="last_notified_project_id">proj123</string>
    <long name="last_notified_timestamp">1702483200000</long>
</map>
```

---

### PASO 3: Comparación 🔄

```kotlin
Proyecto de Firebase (paso 1) vs Proyecto guardado (paso 2)
              ↓                              ↓
         ID: "proj456"              ID: "proj123"
              ↓                              ↓
                    ¿Son diferentes?
                          ↓
                    ┌─────┴─────┐
                   SÍ           NO
                    ↓             ↓
              NUEVO PROYECTO   YA CONOCIDO
              (Enviar notif)   (No hacer nada)
```

**Código de comparación:**
```kotlin
val latestProjectId = latestProject.id
val lastNotifiedId = preferences.getLastNotifiedProjectId()

if (latestProjectId != lastNotifiedId) {
    // ¡NUEVO PROYECTO!
    sendNotification(latestProject)
    preferences.saveLastNotifiedProject(latestProjectId, timestamp)
} else {
    // Ya conocemos este proyecto
    Log.d(TAG, "No hay proyectos nuevos")
}
```

---

### PASO 4A: Si HAY Proyecto Nuevo ✅

```
1. Crear la notificación
   ↓
2. Configurar título: "Nuevo proyecto disponible"
   ↓
3. Configurar mensaje: "Se ha agregado: [Nombre del proyecto]"
   ↓
4. Configurar Intent: Abrir app al tocar
   ↓
5. Mostrar notificación (NotificationManager)
   ↓
6. Guardar el nuevo ID en SharedPreferences
   ↓
7. Log: "Notificación enviada"
   ↓
8. Finalizar Worker con Result.success()
```

---

### PASO 4B: Si NO HAY Proyecto Nuevo ❌

```
1. Log: "No hay proyectos nuevos"
   ↓
2. No crear notificación
   ↓
3. No actualizar SharedPreferences
   ↓
4. Finalizar Worker con Result.success()
```

---

## Ejemplo Práctico: Caso Real 🎯

### Situación Inicial
```
Firebase tiene:
- proj001: "Parque Central" (createdAt: 01/12/2025)
- proj002: "Hospital" (createdAt: 05/12/2025)
- proj003: "Escuela" (createdAt: 10/12/2025) ← ÚLTIMO

SharedPreferences tiene:
- last_notified_project_id: "proj003"
```

### Escenario 1: Se Agrega Proyecto Nuevo 🆕

**Día 13/12/2025 - Alguien agrega:**
```json
"proj004": {
  "name": "Biblioteca Municipal",
  "ubicacion": "Miraflores",
  "createdAt": 1702425600000
}
```

**Worker se ejecuta:**
```
1. Consulta Firebase → Obtiene proj004 (es el más reciente por fecha)
2. Lee SharedPreferences → Encuentra proj003
3. Compara: proj004 ≠ proj003 → ¡SON DIFERENTES!
4. Envía notificación: "Biblioteca Municipal"
5. Guarda proj004 en SharedPreferences
6. Fin ✅
```

**Usuario recibe:** 🔔 Notificación

---

### Escenario 2: No Hay Proyectos Nuevos ⏸️

**Worker se ejecuta:**
```
1. Consulta Firebase → Obtiene proj004 (sigue siendo el más reciente)
2. Lee SharedPreferences → Encuentra proj004
3. Compara: proj004 = proj004 → ¡SON IGUALES!
4. No hace nada
5. Fin ✅
```

**Usuario recibe:** Nada (correcto)

---

### Escenario 3: Primera Ejecución (App Nueva) 🆕

**Worker se ejecuta:**
```
1. Consulta Firebase → Obtiene proj004
2. Lee SharedPreferences → Encuentra null (no hay registro)
3. Compara: proj004 ≠ null → ¡ES NUEVO!
4. Envía notificación: "Biblioteca Municipal"
5. Guarda proj004 en SharedPreferences
6. Fin ✅
```

**Usuario recibe:** 🔔 Notificación (primera vez)

---

## Diagrama de Flujo Completo 📊

```
┌─────────────────────────────────────────┐
│   WorkManager Trigger (cada 1 hora)    │
└──────────────────┬──────────────────────┘
                   ↓
         ┌─────────────────────┐
         │ CheckNewProjectsWorker │
         └──────────┬──────────────┘
                    ↓
        ┌───────────────────────┐
        │ ¿Hay conexión Internet? │
        └────┬──────────────┬─────┘
            NO              SÍ
             ↓               ↓
        ┌────────┐   ┌──────────────────┐
        │ Salir  │   │ Consultar Firebase │
        └────────┘   └─────────┬──────────┘
                              ↓
                ┌──────────────────────────┐
                │ Obtener último proyecto   │
                │ (orderBy createdAt desc)  │
                └────────────┬──────────────┘
                             ↓
                ┌────────────────────────────┐
                │ Leer SharedPreferences      │
                │ (último proyecto notificado) │
                └──────────┬─────────────────┘
                           ↓
                  ┌─────────────────┐
                  │ ¿Son diferentes? │
                  └────┬────────┬────┘
                      NO       SÍ
                       ↓        ↓
              ┌─────────────┐  ┌──────────────────┐
              │ Log: "No hay │  │ Crear Notificación│
              │  nuevos"     │  └────────┬──────────┘
              └──────┬───────┘           ↓
                     ↓         ┌──────────────────────┐
                     ↓         │ Mostrar al Usuario    │
                     ↓         └────────┬──────────────┘
                     ↓                  ↓
                     ↓         ┌──────────────────────┐
                     ↓         │ Guardar nuevo ID en   │
                     ↓         │ SharedPreferences     │
                     ↓         └────────┬──────────────┘
                     ↓                  ↓
                ┌────────────────────────────┐
                │   Result.success()          │
                │   (Worker finalizado)       │
                └─────────────────────────────┘
```

---

## Logs Típicos en Cada Paso 📝

### Log Completo: NO hay proyectos nuevos
```
D/WorkManagerScheduler: WorkManager ejecutado inmediatamente
D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
D/CheckNewProjectsWorker: Consultando Firebase...
D/CheckNewProjectsWorker: Último proyecto obtenido: Biblioteca Municipal (ID: proj004)
D/CheckNewProjectsWorker: Leyendo SharedPreferences...
D/CheckNewProjectsWorker: Último proyecto notificado: proj004
D/CheckNewProjectsWorker: Comparando IDs: proj004 vs proj004
D/CheckNewProjectsWorker: No hay proyectos nuevos
D/CheckNewProjectsWorker: Worker finalizado exitosamente
```

### Log Completo: SÍ hay proyecto nuevo
```
D/WorkManagerScheduler: WorkManager ejecutado inmediatamente
D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
D/CheckNewProjectsWorker: Consultando Firebase...
D/CheckNewProjectsWorker: Último proyecto obtenido: Centro Deportivo (ID: proj005)
D/CheckNewProjectsWorker: Leyendo SharedPreferences...
D/CheckNewProjectsWorker: Último proyecto notificado: proj004
D/CheckNewProjectsWorker: Comparando IDs: proj005 vs proj004
D/CheckNewProjectsWorker: ¡Nuevo proyecto detectado!
D/CheckNewProjectsWorker: Creando notificación...
D/CheckNewProjectsWorker: Notificación enviada
D/CheckNewProjectsWorker: Guardando nuevo ID: proj005
D/CheckNewProjectsWorker: Worker finalizado exitosamente
```

---

## Preguntas Frecuentes ❓

### ¿Cómo sabe cuál es el proyecto más reciente?
**R:** Por el campo `createdAt`. Firebase ordena por ese campo descendente y toma el primero (límite 1).

### ¿Qué pasa si dos proyectos tienen el mismo createdAt?
**R:** Firebase tomará uno de ellos (generalmente el primero que encuentra). Es poco probable si usas timestamps precisos.

### ¿Puede notificar de un proyecto viejo si lo modifico?
**R:** No. Solo compara IDs, no fechas de modificación. Solo notifica de proyectos NUEVOS (nuevos IDs).

### ¿Qué pasa si borro SharedPreferences?
**R:** La próxima ejecución será como "primera vez" y notificará del último proyecto que exista.

### ¿Notifica de todos los proyectos nuevos?
**R:** No. Solo del MÁS RECIENTE. Si agregas 5 proyectos entre ejecuciones, solo notifica del último.

### ¿Cómo puedo ver qué proyecto está guardado localmente?
**R:** Puedes usar Device File Explorer en Android Studio o ejecutar:
```bash
adb shell run-as com.example.datagov cat shared_prefs/project_prefs.xml
```

---

## Resumen Ejecutivo 📌

```
1. Worker consulta Firebase → Obtiene último proyecto
2. Worker lee local → Obtiene último proyecto notificado
3. Worker compara → IDs diferentes = nuevo proyecto
4. Si es nuevo → Notifica + guarda
5. Si no es nuevo → No hace nada
6. Repite cada 1 hora
```

**Clave del sistema:** Comparación de IDs entre Firebase y almacenamiento local.

---

## Tu Caso Específico 🎯

**Lo que pasó:**
```
✅ Worker ejecutado
✅ Firebase consultado
✅ Proyecto obtenido: [ID actual]
✅ Local leído: [mismo ID]
✅ Comparación: Iguales
✅ Resultado: No hay proyectos nuevos
❌ No se envió notificación (CORRECTO)
```

**Para ver notificación:**
1. Agrega proyecto en Firebase con nuevo ID
2. Ejecuta prueba desde Settings
3. Verás notificación ✅

---

¿Necesitas ver el código real del Worker para entender mejor algún paso?

