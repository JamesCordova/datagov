# 🚀 GUÍA RÁPIDA: Cómo Diagnosticar el Problema AHORA

## Paso 1️⃣: Compilar e Instalar (30 segundos)

**En Android Studio:**
1. Presiona el botón ▶️ **Run 'app'** (o Shift+F10)
2. Espera a que compile e instale

---

## Paso 2️⃣: Abrir Logcat (10 segundos)

**En Android Studio:**
1. Click en la pestaña **Logcat** (abajo)
2. En el filtro, escribe: `CheckNewProjectsWorker`
3. Cambia el nivel a: **Debug** o **Verbose**

Deberías ver algo así:
```
┌─────────────────────────────────────┐
│ Logcat                         [ × ]│
├─────────────────────────────────────┤
│ [Filtro: CheckNewProjectsWorker   ]│
│ [Nivel: Debug ▼]                   │
├─────────────────────────────────────┤
│ (logs aparecerán aquí)              │
└─────────────────────────────────────┘
```

---

## Paso 3️⃣: Ejecutar Prueba (5 segundos)

**En tu dispositivo/emulador:**
1. Abre la app DataGov
2. Ve a la pestaña **Settings** (última pestaña)
3. Scroll down hasta "Notificaciones"
4. Presiona el botón **"Ejecutar prueba ahora"**

---

## Paso 4️⃣: LEER LOS LOGS (IMPORTANTE) 👀

Inmediatamente verás logs MUY detallados. **COPIA TODO** y léelo.

### ¿Qué verás?

#### Escenario A: Problema con Firebase
```
D/CheckNewProjectsWorker: ═══════════════════════════════
D/CheckNewProjectsWorker: Worker iniciado
D/CheckNewProjectsWorker: PASO 1: Consultando Firebase...
D/CheckNewProjectsWorker:    Cantidad de proyectos: 0  ← ⚠️ PROBLEMA AQUÍ
D/CheckNewProjectsWorker:    ⚠️ No se encontró ningún proyecto
```

**Significado:** Tu JSON no tiene el campo `createdAt` o está mal estructurado.

---

#### Escenario B: Ya notificaste ese proyecto
```
D/CheckNewProjectsWorker: ✅ Último proyecto obtenido:
D/CheckNewProjectsWorker:    - ID: proj123
D/CheckNewProjectsWorker:    - Nombre: Mi Proyecto
D/CheckNewProjectsWorker: ✅ Último proyecto notificado: proj123  ← MISMO ID
D/CheckNewProjectsWorker: PASO 3: Comparando IDs...
D/CheckNewProjectsWorker:    Firebase ID: 'proj123'
D/CheckNewProjectsWorker:    Local ID:    'proj123'  ← SON IGUALES
D/CheckNewProjectsWorker: ℹ️ No hay proyectos nuevos
```

**Significado:** Ya notificaste de ese proyecto. Necesitas limpiar los datos.

---

#### Escenario C: Notificación enviada pero no aparece
```
D/CheckNewProjectsWorker: 🔔 ¡NUEVO PROYECTO DETECTADO!
D/CheckNewProjectsWorker: PASO 4: Enviando notificación...
D/CheckNewProjectsWorker:    ✅ Notificación enviada exitosamente  ← SE ENVIÓ
```

**Significado:** El código funciona, pero los permisos de notificación están desactivados.

---

## Paso 5️⃣: Solución Según el Escenario

### Si viste Escenario A (Firebase vacío):
1. Ve a Firebase Console
2. Verifica que tu JSON tenga el campo `createdAt`:
   ```json
   {
     "Projects": {
       "proj001": {
         "name": "Mi Proyecto",
         "createdAt": 1734076800000  ← DEBE EXISTIR
       }
     }
   }
   ```

---

### Si viste Escenario B (IDs iguales):
Ejecuta este comando para limpiar:
```bash
adb shell pm clear com.example.datagov
```

**O desde Android Studio:**
1. Ve a Run → Edit Configurations
2. En "Before launch" → Add → Clear app data
3. Run de nuevo

Luego vuelve a ejecutar la prueba.

---

### Si viste Escenario C (notificación enviada pero no aparece):
1. Abre **Configuración** del dispositivo
2. **Apps** → **DataGov**
3. **Notificaciones**
4. Activa **"Permitir notificaciones"**
5. Verifica que el canal **"Nuevos Proyectos"** esté activado

---

## Paso 6️⃣: Probar con Proyecto Garantizado ✅

Si nada funciona, **agrega este proyecto en Firebase**:

```json
{
  "Projects": {
    "test_2025_diciembre_13": {
      "name": "Proyecto de Prueba Definitivo",
      "ubicacion": "Lima",
      "categoryId": "cat1",
      "createdAt": 1734076800000
    }
  }
}
```

Luego:
1. Limpia datos: `adb shell pm clear com.example.datagov`
2. Abre la app
3. Ve a Settings → "Ejecutar prueba ahora"
4. **DEBERÍAS VER NOTIFICACIÓN** 🔔

---

## Resumen Ultra-Rápido 🎯

```
1. Run app (Android Studio)
2. Abrir Logcat → Filtrar por "CheckNewProjectsWorker"
3. Settings → "Ejecutar prueba ahora"
4. Leer logs
5. Aplicar solución según lo que veas
```

---

## ¿Qué Me Debes Compartir? 📋

Después de ejecutar la prueba, compárteme:

1. **Los logs completos** (copia todo desde Logcat)
2. **Captura de pantalla** de tu Firebase (estructura de datos)
3. **Qué pasó**: ¿Apareció notificación? ¿Qué dijeron los logs?

Con eso te diré EXACTAMENTE qué está mal.

---

## Comando Rápido para Ver Logs (Alternativa)

Si prefieres terminal en lugar de Logcat:

```bash
adb logcat -c
adb logcat -s CheckNewProjectsWorker:D
```

Luego ejecuta la prueba y verás los logs en la terminal.

---

## HAZLO AHORA 👇

1. ✅ Click en ▶️ Run en Android Studio
2. ✅ Abre Logcat
3. ✅ Filtra por `CheckNewProjectsWorker`
4. ✅ Ejecuta prueba desde Settings
5. ✅ Copia los logs y compártelos

**Te espero con los logs para diagnosticar el problema exacto.** 🔍

