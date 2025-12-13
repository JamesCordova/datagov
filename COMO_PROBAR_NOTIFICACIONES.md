"# 🔔 Guía Completa: Cómo Probar las Notificaciones

## ✅ Todo está listo, ahora vamos a probar

---

## 🎯 Método 1: Prueba Inmediata (Recomendado) ⚡

### Paso 1: Limpia el historial de notificaciones
Esto hará que la app "olvide" qué proyectos ya notificó:

```bash
adb shell pm clear com.example.datagov
```

**O desde Android Studio:**
- Run → Stop (detén la app)
- Run → Edit Configurations → Before launch → Add → Clear app data
- Run de nuevo

### Paso 2: Abre la app
1. La app se iniciará limpia (sin historial)
2. Ve a la pestaña **Settings** (última)
3. Scroll down hasta "Notificaciones"
4. Presiona **"Ejecutar prueba ahora"**

### Paso 3: Verifica
- 🔔 **Deberías recibir una notificación inmediatamente**
- La notificación dirá: "Nuevo proyecto disponible"
- Mostrará el nombre del último proyecto en Firebase

### Logs esperados:
```
D/WorkManagerScheduler: WorkManager ejecutado inmediatamente
D/CheckNewProjectsWorker: ═══════════════════════════════
D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
D/CheckNewProjectsWorker: PASO 1: Consultando Firebase...
D/CheckNewProjectsWorker:    Cantidad de proyectos en respuesta: 1
D/CheckNewProjectsWorker:    Proyecto #1 encontrado:
D/CheckNewProjectsWorker:       ID: proj_1765659342541
D/CheckNewProjectsWorker:       name: Teleferico
D/CheckNewProjectsWorker: ✅ Último proyecto obtenido:
D/CheckNewProjectsWorker:    - ID: proj_1765659342541
D/CheckNewProjectsWorker:    - Nombre: Teleferico
D/CheckNewProjectsWorker:    - createdAt: 1765659342541
D/CheckNewProjectsWorker: PASO 2: Leyendo almacenamiento local...
D/CheckNewProjectsWorker: ✅ Último proyecto notificado guardado: null
D/CheckNewProjectsWorker: PASO 3: Comparando IDs...
D/CheckNewProjectsWorker:    Firebase ID: 'proj_1765659342541'
D/CheckNewProjectsWorker:    Local ID:    'null'
D/CheckNewProjectsWorker: 🔔 ¡NUEVO PROYECTO DETECTADO! Los IDs son diferentes
D/CheckNewProjectsWorker: PASO 4: Enviando notificación...
D/CheckNewProjectsWorker:    → Creando notificación para: Teleferico
D/CheckNewProjectsWorker:    → Creando canal de notificación (Android 8+)
D/CheckNewProjectsWorker:    → Mostrando notificación con ID: 1001
D/CheckNewProjectsWorker:    ✅ Notificación enviada exitosamente
D/CheckNewProjectsWorker: PASO 5: Guardando nuevo ID en SharedPreferences...
D/CheckNewProjectsWorker: ✅ Notificación enviada y ID guardado exitosamente
D/CheckNewProjectsWorker: ═══════════════════════════════
```

---

## 🎯 Método 2: Crear un Nuevo Proyecto y Probarlo 📝

### Paso 1: Abre la app
En la primera pantalla (lista de proyectos)

### Paso 2: Presiona el botón flotante (+)
Se abrirá el formulario

### Paso 3: Completa el formulario
```
Nombre: Proyecto de Prueba Notificaciones
Ubicación: Lima
Descripción: Este es un proyecto para probar las notificaciones
ID Categoría: 1
Presupuesto: 1000000
Avance: 25
URL Imagen: (deja vacío)
```

### Paso 4: Presiona "Crear Proyecto"
- Verás mensaje: ✅ Proyecto creado exitosamente
- El proyecto se guardará en Firebase con `createdAt` = timestamp actual

### Paso 5: Prueba la notificación
1. Ve a **Settings**
2. Presiona **"Ejecutar prueba ahora"**
3. 🔔 **Recibirás notificación del proyecto que acabas de crear**

---

## 🎯 Método 3: Esperar la Verificación Automática ⏰

El WorkManager está configurado para ejecutarse automáticamente:
- **Primera ejecución**: 15 minutos después de abrir la app
- **Ejecuciones siguientes**: Cada 1 hora

### Para probarlo:
1. Crea un proyecto nuevo desde el formulario
2. Cierra la app completamente
3. **Espera 1 hora** ⏱️
4. 🔔 Recibirás la notificación automáticamente

**Nota:** El sistema Android puede retrasar la ejecución para ahorrar batería.

---

## 🎯 Método 4: Ver Logs en Tiempo Real 📊

### Mientras pruebas:

**Terminal 1 - Logcat del Worker:**
```bash
adb logcat -c
adb logcat -s CheckNewProjectsWorker:D WorkManagerScheduler:D
```

**Terminal 2 - Actividad de la app:**
```bash
adb logcat -s Firebase:D
```

Deja estos terminales abiertos mientras ejecutas las pruebas para ver qué está pasando.

---

## 🎯 Método 5: Verificar Permisos de Notificación 🔐

Si NO recibes notificación pero los logs dicen "✅ Notificación enviada exitosamente":

### Android 13+ (API 33+):
1. Abre **Configuración** del dispositivo
2. **Apps** → **DataGov**
3. **Notificaciones**
4. Verifica que estén **ACTIVADAS** ✅
5. Verifica que el canal **"Nuevos Proyectos"** esté **ACTIVADO** ✅

### Desde código (opcional):
Puedes agregar una solicitud de permisos al abrir Settings:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Settings", "Permiso de notificaciones concedido")
        }
    }
    
    Button(onClick = {
        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }) {
        Text("Solicitar Permiso de Notificaciones")
    }
}
```

---

## 📋 Checklist de Verificación ✓

Antes de probar, verifica:

- [ ] La app está instalada y abierta
- [ ] Tienes conexión a Internet
- [ ] Firebase tiene proyectos con campo `createdAt`
- [ ] Los permisos de notificación están activados
- [ ] Logcat está abierto para ver los logs
- [ ] Has compilado la última versión del código

---

## 🧪 Escenarios de Prueba Completos

### Escenario 1: Primera vez (Sin historial)
```
Estado inicial: SharedPreferences vacío (null)
Acción: Ejecutar prueba desde Settings
Resultado esperado: 🔔 Notificación del último proyecto
Log: "Local ID: 'null'" → "¡NUEVO PROYECTO DETECTADO!"
```

### Escenario 2: Proyecto existente
```
Estado inicial: Ya notificó del proyecto "Teleferico"
Acción: Ejecutar prueba desde Settings
Resultado esperado: ❌ NO notificación
Log: "Firebase ID: 'proj_1765659342541'" = "Local ID: 'proj_1765659342541'"
      "No hay proyectos nuevos"
```

### Escenario 3: Proyecto nuevo agregado
```
Estado inicial: Ya notificó del proyecto "Teleferico"
Acción: 1. Crear proyecto nuevo "Biblioteca"
        2. Ejecutar prueba desde Settings
Resultado esperado: 🔔 Notificación del nuevo proyecto "Biblioteca"
Log: "Firebase ID: 'proj_1734076900000'" ≠ "Local ID: 'proj_1765659342541'"
      "¡NUEVO PROYECTO DETECTADO!"
```

---

## 🎬 Video Tutorial Paso a Paso

### Para probar AHORA MISMO:

**Paso 1:** Abre terminal y ejecuta:
```bash
adb shell pm clear com.example.datagov
```

**Paso 2:** Abre la app en tu dispositivo

**Paso 3:** Ve a Settings (última pestaña)

**Paso 4:** Presiona "Ejecutar prueba ahora"

**Paso 5:** 🎉 **¡DEBERÍAS VER LA NOTIFICACIÓN!**

---

## 🔍 Troubleshooting

### Problema: No aparece notificación

#### Solución 1: Verificar logs
```bash
adb logcat -s CheckNewProjectsWorker:D | findstr "Notificación"
```

¿Dice "✅ Notificación enviada exitosamente"?
- **Sí** → El problema son los permisos
- **No** → El problema es la detección

#### Solución 2: Verificar que hay proyectos en Firebase
```bash
adb logcat -s CheckNewProjectsWorker:D | findstr "Cantidad"
```

¿Dice "Cantidad de proyectos en respuesta: 1"?
- **Sí** → Hay proyectos ✅
- **No (0)** → Firebase está vacío o `createdAt` no existe

#### Solución 3: Forzar "primera vez"
```bash
# Limpiar datos
adb shell pm clear com.example.datagov

# Abrir la app
adb shell am start -n com.example.datagov/.MainActivity

# Esperar 3 segundos
timeout /t 3

# Ejecutar worker manualmente (desde Settings en la app)
```

---

## 📱 Apariencia de la Notificación

Cuando funcione, verás algo así:

```
┌──────────────────────────────────────────┐
│ 🔔 Nuevo proyecto disponible            │
├──────────────────────────────────────────┤
│ Se ha agregado un nuevo proyecto del    │
│ gobierno: Teleferico                     │
│                                          │
│ Ubicación: Arequipa                     │
├──────────────────────────────────────────┤
│                                    [×]   │
└──────────────────────────────────────────┘
```

Al tocar la notificación → Abre la app

---

## 🎯 Prueba Definitiva (100% Garantizada)

Si quieres estar 100% seguro de que funciona:

### Script completo:
```bash
# 1. Detener app
adb shell am force-stop com.example.datagov

# 2. Limpiar datos
adb shell pm clear com.example.datagov

# 3. Abrir app
adb shell am start -n com.example.datagov/.MainActivity

# 4. Esperar que cargue (5 segundos)
timeout /t 5

# 5. Abrir Logcat en otra terminal
start cmd /k "adb logcat -s CheckNewProjectsWorker:D"

# 6. Ahora en la app: Settings → "Ejecutar prueba ahora"
# 7. Verás logs Y notificación
```

---

## 🎊 ¡Éxito!

Si ves la notificación, significa que:
✅ WorkManager funciona
✅ Firebase se consulta correctamente
✅ La detección de nuevos proyectos funciona
✅ Las notificaciones se envían
✅ El sistema completo está operativo

---

## 📝 Resumen de Comandos Útiles

### Ver si el Worker está registrado:
```bash
adb shell dumpsys jobscheduler | findstr DataGov
```

### Ver estado de WorkManager:
```bash
adb shell dumpsys activity provider androidx.work.impl.WorkManagerInitializer
```

### Ver logs en tiempo real:
```bash
adb logcat -s CheckNewProjectsWorker:D WorkManagerScheduler:D
```

### Limpiar y probar desde cero:
```bash
adb shell pm clear com.example.datagov && adb shell am start -n com.example.datagov/.MainActivity
```

---

## 🚀 Próximo Nivel

Una vez que compruebes que funciona:

1. **Cambia el intervalo** (opcional):
   - Abre `WorkManagerScheduler.kt`
   - Cambia `REPEAT_INTERVAL_HOURS = 1L` a `15L` (cada 15 minutos para testing)
   - O déjalo en 1 hora para producción

2. **Personaliza la notificación**:
   - Abre `CheckNewProjectsWorker.kt`
   - Modifica el método `showNotification()`
   - Cambia el texto, icono, sonido, etc.

3. **Agrega estadísticas**:
   - Cuenta cuántas veces se ha ejecutado el Worker
   - Guarda en SharedPreferences
   - Muestra en Settings

---

**¡Ahora ve y prueba!** 🎉

**Método más fácil:**
1. `adb shell pm clear com.example.datagov`
2. Abre la app
3. Settings → "Ejecutar prueba ahora"
4. 🔔 ¡Notificación!

