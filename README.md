# DataGov - Aplicación de Gestión de Proyectos

## Descripción

DataGov es una aplicación Android para gestión de proyectos gubernamentales, desarrollada en Kotlin con Jetpack Compose.

## Requisitos previos

Para ejecutar la aplicación necesita:

1. **Android Studio** (Hedgehog o superior) - [Descargar aquí](https://developer.android.com/studio)
2. **JDK 11** o superior (incluido en Android Studio)
3. **SDK de Android** - API mínimo 24, Target SDK 36
4. **Conexión a Internet** para dependencias y Firebase

## Pasos para ejecutar la aplicación

### 1. Clonar el repositorio

**Opción A: Desde Android Studio**
1. Abra Android Studio
2. Seleccione **"Get from VCS"** en la pantalla de bienvenida
3. En el campo URL, pegue: `https://github.com/JamesCordova/DataGov.git`
4. Seleccione el directorio donde desee guardar el proyecto
5. Haga clic en **"Clone"**

**Opción B: Clonar desde terminal y abrir en Android Studio**
1. Abra una terminal o PowerShell
2. Ejecute el comando: `git clone https://github.com/JamesCordova/DataGov.git`
3. Abra Android Studio
4. Seleccione **"Open"**
5. Navegue hasta la carpeta clonada `DataGov` y selecciónela
6. Haga clic en **"OK"**

### 2. Sincronizar Gradle

1. Android Studio sincronizará automáticamente el proyecto
2. Si no lo hace, haga clic en el ícono **"Sync Project with Gradle Files"** en la barra superior
3. Espere a que se descarguen todas las dependencias

### 3. Configurar Firebase (opcional)

El proyecto incluye un archivo `google-services.json` preconfigurado en `app/google-services.json`.

Si necesita configurar su propio Firebase:
1. Acceda a [Firebase Console](https://console.firebase.google.com/)
2. Cree o seleccione un proyecto
3. Agregue una app Android con el ID de paquete: `com.example.datagov`
4. Descargue el archivo `google-services.json` y reemplace el existente

### 4. Preparar dispositivo para ejecución

**Opción A: Usar emulador Android**
1. Vaya a **Tools → Device Manager → Create Device**
2. Seleccione un dispositivo (recomendado: Pixel 5)
3. Seleccione una imagen del sistema (API 34 o superior)
4. Finalice la configuración e inicie el emulador

**Opción B: Usar dispositivo físico**
1. En su dispositivo, active las **Opciones de desarrollador**
2. Active la opción **Depuración USB**
3. Conecte su dispositivo al PC mediante USB
4. Autorice la depuración cuando se le solicite

### 5. Ejecutar la aplicación

1. Seleccione su dispositivo o emulador en el menú desplegable de la barra superior
2. Haga clic en el botón **Run** (▶️) o presione `Shift + F10`
3. Espere a que la aplicación compile e instale
4. La aplicación se iniciará automáticamente en su dispositivo

## Solución de problemas

### Error de sincronización de Gradle

- Verifique su conexión a Internet
- Vaya a **File → Invalidate Caches / Restart**
- Ejecute **Build → Clean Project** y luego **Build → Rebuild Project**

### La aplicación no se instala

- Desinstale versiones anteriores de la aplicación del dispositivo
- Ejecute **Build → Clean Project**
- Vuelva a ejecutar la aplicación

### Errores de Firebase

- Verifique que `google-services.json` esté en la carpeta `app/`
- Confirme que el ID del paquete sea `com.example.datagov`

## Tecnologías utilizadas

- Kotlin
- Jetpack Compose
- Firebase
- Room Database
- Material Design 3

---

**Versión**: 1.0 | **Min SDK**: 24 | **Target SDK**: 36
