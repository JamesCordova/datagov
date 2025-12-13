# Guía de Verificación del WorkManager - DataGov

## ¿Cómo saber si el WorkManager está funcionando?

### 1. Verificación a través de la App (Método más fácil) ✅

1. **Abre la app** que ya instalaste
2. **Ve a la pestaña "Settings"** (última pestaña en el bottom navigation)
3. **Busca la sección "Notificaciones"**
4. **Presiona el botón "Ejecutar prueba ahora"**
5. **Espera unos segundos** y deberías:
   - Ver una notificación si hay un proyecto nuevo en Firebase
   - Ver logs en el Logcat (si está conectado)

### 2. Verificación a través de Logcat (Método más detallado) 🔍

#### Paso 1: Filtrar los logs
En Android Studio, abre el Logcat y filtra por:
```
CheckNewProjectsWorker
```

#### Paso 2: Buscar estos mensajes
Si el Worker funciona correctamente, verás:
```
D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
D/CheckNewProjectsWorker: Último proyecto obtenido: [Nombre] (ID: [ID])
D/CheckNewProjectsWorker: Último proyecto notificado: [ID]
D/CheckNewProjectsWorker: ¡Nuevo proyecto detectado! Enviando notificación
D/CheckNewProjectsWorker: Notificación enviada
```

O si no hay proyectos nuevos:
```
D/CheckNewProjectsWorker: No hay proyectos nuevos
```

### 3. Verificación en Firebase (Prueba Real) 🔥

#### Para probar que detecta nuevos proyectos:

1. **Anota el ID del último proyecto** en tu Firebase Database
2. **Ejecuta la prueba** desde Settings
3. **Agrega un nuevo proyecto** en Firebase Database:
   ```json
   {
     "Projects": {
       "nuevo_id_unico": {
         "name": "Proyecto de Prueba",
         "ubicacion": "Lima",
         "categoryId": "cat1",
         "createdAt": [timestamp actual]
       }
     }
   }
   ```
4. **Ejecuta la prueba nuevamente** desde Settings
5. **Deberías recibir una notificación** 📱

### 4. Verificación con WorkManager Inspector (Android Studio) 🔧

#### En Android Studio Flamingo o superior:

1. Con tu dispositivo/emulador conectado
2. Ve a **View → Tool Windows → App Inspection**
3. Selecciona la pestaña **Background Task Inspector**
4. Busca el trabajo: `check_new_projects_work`
5. Podrás ver:
   - Estado del trabajo (ENQUEUED, RUNNING, SUCCEEDED)
   - Próxima ejecución programada
   - Historial de ejecuciones

### 5. Verificación Manual con ADB 📱

Ejecuta este comando para ver el estado del WorkManager:
```bash
adb shell dumpsys jobscheduler | findstr DataGov
```

### 6. Verificación de Permisos de Notificación ⚠️

**MUY IMPORTANTE**: En Android 13+ necesitas dar permiso de notificaciones:

1. **Abre Configuración del dispositivo**
2. **Aplicaciones → DataGov**
3. **Notificaciones**
4. **Activa "Permitir notificaciones"**

O desde código, la app debería solicitar el permiso automáticamente.

### 7. Verificación del Intervalo de Ejecución ⏰

El Worker está configurado para ejecutarse:
- **Intervalo**: Cada 1 hora
- **Primera ejecución**: 15 minutos después de instalar/abrir la app
- **Requiere**: Conexión a Internet

Para verificar que se ejecutará periódicamente:
1. Ejecuta la prueba manual primero
2. Deja la app en segundo plano
3. Espera 1 hora
4. Revisa Logcat nuevamente

## Solución de Problemas 🛠️

### Problema: No aparecen logs
**Solución**:
- Asegúrate de estar filtrando por "CheckNewProjectsWorker"
- Verifica que el nivel de log esté en "Debug" o "Verbose"

### Problema: No aparece notificación
**Soluciones**:
1. Verifica que los permisos de notificación estén activados
2. Revisa que haya un proyecto nuevo en Firebase
3. Ejecuta la prueba manual desde Settings
4. Revisa los logs para ver si hay errores

### Problema: Worker no se ejecuta periódicamente
**Soluciones**:
1. Verifica que la app tenga conexión a Internet
2. En Android 12+, verifica que no esté en modo ahorro de batería
3. Abre la app al menos una vez para que se registre el Worker
4. Espera al menos 15 minutos para la primera ejecución

### Problema: Notificaciones duplicadas
**Solución**:
- El sistema ya está diseñado para evitar esto usando SharedPreferences
- Si ocurre, elimina los datos de la app y reinstala

## Ejemplo de Flujo Completo 🎯

1. ✅ Instalar la app
2. ✅ Abrir la app (se registra el WorkManager)
3. ✅ Ir a Settings
4. ✅ Presionar "Ejecutar prueba ahora"
5. ✅ Ver en Logcat:
   ```
   D/WorkManagerScheduler: WorkManager ejecutado inmediatamente
   D/CheckNewProjectsWorker: Worker iniciado - Verificando nuevos proyectos
   D/CheckNewProjectsWorker: Último proyecto obtenido: Construcción Parque (ID: proj123)
   D/CheckNewProjectsWorker: No hay proyectos nuevos
   ```
6. ✅ Agregar un nuevo proyecto en Firebase
7. ✅ Presionar "Ejecutar prueba ahora" de nuevo
8. ✅ Ver notificación: "Nuevo proyecto disponible"
9. ✅ Tocar la notificación (abre la app)

## Comandos Útiles 💻

### Ver logs del Worker:
```bash
adb logcat -s CheckNewProjectsWorker:D
```

### Ver logs del Scheduler:
```bash
adb logcat -s WorkManagerScheduler:D
```

### Limpiar datos de la app (para testing):
```bash
adb shell pm clear com.example.datagov
```

### Forzar ejecución inmediata (requiere root):
```bash
adb shell cmd jobscheduler run -f com.example.datagov [JOB_ID]
```

## Configuración Actual ⚙️

- **Intervalo**: 1 hora
- **Delay inicial**: 15 minutos
- **Requiere**: Internet
- **Política**: KEEP (no reemplaza trabajos existentes)
- **Tipo**: Periódico

## Notas Importantes 📝

1. El WorkManager NO garantiza ejecución exacta cada hora
2. El sistema Android puede retrasar la ejecución para ahorrar batería
3. En modo Doze, las ejecuciones pueden ser menos frecuentes
4. La primera ejecución es 15 minutos después de abrir la app
5. Se requiere conexión a Internet para funcionar
6. Las notificaciones solo aparecen si hay un proyecto nuevo
7. El sistema evita notificaciones duplicadas automáticamente

## Próximos Pasos 🚀

1. ✅ Verificar funcionamiento básico
2. ✅ Probar con proyecto nuevo en Firebase
3. ✅ Ajustar intervalo si es necesario
4. ✅ Personalizar notificaciones
5. ✅ Agregar estadísticas de verificaciones

