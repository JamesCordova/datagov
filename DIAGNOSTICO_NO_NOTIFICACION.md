# Diagnóstico: No Aparece Notificación 🔍

## Tu Situación
✅ Importaste un nuevo JSON a Firebase  
❌ NO recibiste notificación  

## Posibles Causas

### 1. **El campo `createdAt` no existe o está mal** ⚠️

El Worker ordena los proyectos por `createdAt`. Si tu JSON importado NO tiene este campo o tiene valores incorrectos, el sistema no detectará el proyecto correcto.

**Verifica tu JSON en Firebase:**
```json
{
  "Projects": {
    "proj001": {
      "name": "Mi Proyecto",
      "ubicacion": "Lima",
      "categoryId": "cat1",
      "createdAt": 1702483200000  ← ¿EXISTE ESTE CAMPO?
    }
  }
}
```

**Solución:** Asegúrate de que TODOS los proyectos tengan el campo `createdAt` con un timestamp válido.

---

### 2. **El proyecto nuevo tiene `createdAt` más antiguo** 📅

Si importaste proyectos con `createdAt` más antiguo que los existentes, el Worker no lo detectará como "nuevo".

**Ejemplo del problema:**
```json
{
  "Projects": {
    "proj001": {
      "createdAt": 1702483200000  ← Proyecto viejo
    },
    "proj002": {
      "createdAt": 1700000000000  ← Nuevo que importaste (MÁS ANTIGUO)
    }
  }
}
```

El Worker solo verá `proj001` porque tiene el `createdAt` más reciente.

**Solución:** Usa timestamps actuales para proyectos nuevos.

---

### 3. **Los permisos de notificación no están activados** 🔔

En Android 13+, necesitas dar permiso explícito.

**Solución:**
1. Abre Configuración del dispositivo
2. Apps → DataGov
3. Notificaciones
4. Activa "Permitir notificaciones"

---

### 4. **Ya notificaste de ese proyecto antes** 🔄

Si el ID del proyecto importado ya existe en SharedPreferences, no volverá a notificar.

**Solución:** Limpia los datos de la app para resetear.

---

## Pasos de Diagnóstico (HAZLO AHORA) 🔧

### Paso 1: Instala la app actualizada con más logs
```bash
cd C:\Users\james\AndroidStudioProjects\DataGov
.\gradlew installDebug
```

### Paso 2: Conecta Logcat
En Android Studio:
1. Ve a Logcat
2. Filtra por: `CheckNewProjectsWorker`
3. Asegúrate de ver nivel "Debug"

### Paso 3: Ejecuta la prueba
1. Abre la app en tu dispositivo
2. Ve a Settings
3. Presiona "Ejecutar prueba ahora"

### Paso 4: Lee los logs COMPLETOS

Ahora verás logs MUY detallados como:

```
D/CheckNewProjectsWorker: ═══════════════════════════════════════════════════
D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
D/CheckNewProjectsWorker: ═══════════════════════════════════════════════════
D/CheckNewProjectsWorker: PASO 1: Consultando Firebase...
D/CheckNewProjectsWorker:    Consultando: database.child('Projects').orderByChild('createdAt').limitToLast(1)
D/CheckNewProjectsWorker:    Respuesta de Firebase recibida
D/CheckNewProjectsWorker:    Cantidad de proyectos en respuesta: 1
D/CheckNewProjectsWorker:    Proyecto #1 encontrado:
D/CheckNewProjectsWorker:       ID: proj123
D/CheckNewProjectsWorker:       name: Mi Proyecto
D/CheckNewProjectsWorker:       createdAt: 1702483200000
D/CheckNewProjectsWorker: ✅ Último proyecto obtenido:
D/CheckNewProjectsWorker:    - ID: proj123
D/CheckNewProjectsWorker:    - Nombre: Mi Proyecto
D/CheckNewProjectsWorker:    - Ubicación: Lima
D/CheckNewProjectsWorker:    - createdAt: 1702483200000
D/CheckNewProjectsWorker: PASO 2: Leyendo almacenamiento local...
D/CheckNewProjectsWorker: ✅ Último proyecto notificado guardado: proj123
D/CheckNewProjectsWorker: PASO 3: Comparando IDs...
D/CheckNewProjectsWorker:    Firebase ID: 'proj123'
D/CheckNewProjectsWorker:    Local ID:    'proj123'
D/CheckNewProjectsWorker: ℹ️ No hay proyectos nuevos - Los IDs son iguales
```

---

## Análisis de Logs - ¿Qué Buscar? 👀

### ✅ CASO 1: Si ves esto
```
D/CheckNewProjectsWorker:    Cantidad de proyectos en respuesta: 0
D/CheckNewProjectsWorker:    ⚠️ No se encontró ningún proyecto en Firebase
```

**Problema:** El campo `createdAt` no existe o Firebase está vacío.

**Solución:** 
- Verifica la estructura de tu JSON
- Asegúrate de que TODOS los proyectos tengan `createdAt`

---

### ✅ CASO 2: Si ves esto
```
D/CheckNewProjectsWorker:    Firebase ID: 'proj456'
D/CheckNewProjectsWorker:    Local ID:    'proj456'
D/CheckNewProjectsWorker: ℹ️ No hay proyectos nuevos - Los IDs son iguales
```

**Problema:** Ya notificaste de ese proyecto.

**Solución:** Limpia los datos:
```bash
adb shell pm clear com.example.datagov
```

---

### ✅ CASO 3: Si ves esto
```
D/CheckNewProjectsWorker: 🔔 ¡NUEVO PROYECTO DETECTADO!
D/CheckNewProjectsWorker: PASO 4: Enviando notificación...
D/CheckNewProjectsWorker:    → Creando notificación para: Nuevo Proyecto
D/CheckNewProjectsWorker:    → Creando canal de notificación (Android 8+)
D/CheckNewProjectsWorker:    → Canal creado: new_projects_channel
D/CheckNewProjectsWorker:    → Mostrando notificación con ID: 1001
D/CheckNewProjectsWorker:    ✅ Notificación enviada exitosamente
```

**Problema:** El código SÍ envió la notificación, pero no aparece.

**Solución:** Verifica permisos de notificación en Settings del dispositivo.

---

## Soluciones Rápidas 🚀

### Solución 1: Forzar detección limpiando datos
```bash
adb shell pm clear com.example.datagov
```
Luego ejecuta la prueba desde Settings.

### Solución 2: Verificar estructura Firebase

Asegúrate de que tu JSON tenga este formato EXACTO:
```json
{
  "Projects": {
    "proyecto_unico_id_1": {
      "name": "Nombre del Proyecto",
      "ubicacion": "Lima",
      "categoryId": "cat1",
      "createdAt": 1702483200000
    }
  }
}
```

### Solución 3: Generar timestamps válidos

Para obtener un timestamp actual en milisegundos:
- JavaScript: `Date.now()`
- Python: `int(time.time() * 1000)`
- Online: https://currentmillis.com/

Usa timestamps ACTUALES (no valores pequeños como 1000, 2000, etc).

### Solución 4: Verificar permisos

Si los logs muestran "Notificación enviada" pero no aparece:

1. Configuración → Apps → DataGov → Notificaciones
2. Verifica que estén activadas
3. Verifica que el canal "Nuevos Proyectos" esté activado

---

## JSON de Prueba Garantizado ✅

Usa este JSON en Firebase (copia y pega):

```json
{
  "Projects": {
    "test_proyecto_2025": {
      "name": "Proyecto de Prueba Diciembre 2025",
      "ubicacion": "Lima Centro",
      "categoryId": "cat1",
      "createdAt": 1734076800000
    }
  }
}
```

Este proyecto tiene:
- ✅ ID único
- ✅ Todos los campos requeridos
- ✅ `createdAt` con timestamp actual (13 Dic 2025)

---

## Checklist Completo ✓

Antes de volver a probar:

- [ ] Instalé la app actualizada con más logs
- [ ] Tengo Logcat abierto y filtrado por `CheckNewProjectsWorker`
- [ ] Verifiqué que mi JSON en Firebase tiene el campo `createdAt`
- [ ] El timestamp de `createdAt` es un número grande (actual)
- [ ] Limpié los datos de la app con `pm clear`
- [ ] Los permisos de notificación están activados
- [ ] Ejecuté la prueba desde Settings

---

## Comando Todo-en-Uno 🎯

Ejecuta esto para hacer todo el diagnóstico:

```bash
# 1. Instalar app actualizada
cd C:\Users\james\AndroidStudioProjects\DataGov ; .\gradlew installDebug

# 2. Limpiar datos (resetear)
adb shell pm clear com.example.datagov

# 3. Ver logs en tiempo real
adb logcat -c ; adb logcat -s CheckNewProjectsWorker:D WorkManagerScheduler:D
```

Luego:
1. Abre la app
2. Ve a Settings
3. Presiona "Ejecutar prueba ahora"
4. Lee los logs

---

## Próximo Paso INMEDIATO 👉

1. **Instala la versión con más logs**
2. **Ejecuta la prueba**
3. **Copia los logs completos que aparezcan**
4. **Compártelos conmigo para diagnosticar exactamente qué está pasando**

Los nuevos logs te dirán EXACTAMENTE por qué no aparece la notificación.

---

## Estructura de Datos Correcta en Firebase 📊

Tu Firebase Realtime Database debe verse así:

```
DataGov-123456 (tu proyecto)
  └── Projects
        ├── -O1abc123def
        │     ├── name: "Parque Central"
        │     ├── ubicacion: "Lima"
        │     ├── categoryId: "cat1"
        │     └── createdAt: 1702483200000
        │
        ├── -O1abc456ghi
        │     ├── name: "Hospital Regional"
        │     ├── ubicacion: "Callao"
        │     ├── categoryId: "cat2"
        │     └── createdAt: 1702569600000  ← Este es el más reciente
        │
        └── -O1abc789jkl  ← Proyecto nuevo que importaste
              ├── name: "Biblioteca Municipal"
              ├── ubicacion: "Miraflores"
              ├── categoryId: "cat1"
              └── createdAt: 1734076800000  ← Debe ser MÁS GRANDE que los anteriores
```

El Worker tomará el proyecto con el `createdAt` MÁS ALTO (más reciente).

