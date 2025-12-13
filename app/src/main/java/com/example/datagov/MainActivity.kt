package com.example.datagov

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.example.datagov.ui.theme.DataGovTheme
import com.example.datagov.data.ThemePreferences
import com.example.datagov.data.Project
import com.google.firebase.database.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import com.example.datagov.data.MeetingDatabase
import com.example.datagov.data.MeetingRepository
import com.example.datagov.ui.meetings.MeetingsScreen
import com.example.datagov.ui.meetings.AddMeetingScreen
import com.example.datagov.ui.meetings.MeetingDetailScreen
import com.example.datagov.workers.WorkManagerScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themePreferences = ThemePreferences(this)

        // Inicializar WorkManager para verificar nuevos proyectos
        WorkManagerScheduler.schedulePeriodicWork(this)

        setContent {
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            DataGovTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        themePreferences = themePreferences
                    )
                }
            }
        }
    }
}

// Modelos para la navegación
sealed class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : NavigationItem("home", "Proyectos", Icons.Filled.Home, Icons.Outlined.Home)
    object Meetings : NavigationItem("meetings", "Meetings", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Settings : NavigationItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

sealed class Screen {
    object First : Screen()
    data class Second(val text: String) : Screen()
    object Third : Screen()
    object Meetings : Screen()
    object AddMeeting : Screen()
    data class MeetingDetail(val meetingId: Long) : Screen()
    object Settings : Screen()
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier, themePreferences: ThemePreferences) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.First) }
    var selectedTab by remember { mutableStateOf(0) }

    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Meetings,
        NavigationItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            currentScreen = when (index) {
                                0 -> Screen.First
                                1 -> Screen.Meetings
                                2 -> Screen.Settings
                                else -> Screen.First
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val screen = currentScreen) {
                is Screen.First -> {
                    FirstScreen(
                        onNavigateToSecond = { text ->
                            currentScreen = Screen.Second(text)
                        },
                        onNavigateToThird = {
                            currentScreen = Screen.Third
                        }
                    )
                }
                is Screen.Second -> {
                    SecondScreen(
                        text = screen.text,
                        onBack = {
                            currentScreen = Screen.First
                            selectedTab = 0
                        },
                        onNavigateToThird = {
                            currentScreen = Screen.Third
                        }
                    )
                }
                is Screen.Third -> {
                    ThirdScreen(
                        onBack = {
                            currentScreen = Screen.First
                            selectedTab = 0
                        }
                    )
                }
                is Screen.Meetings -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = remember { MeetingDatabase.getDatabase(context) }
                    val repository = remember { MeetingRepository(database.meetingDao()) }

                    MeetingsScreen(
                        repository = repository,
                        onNavigateToAdd = {
                            currentScreen = Screen.AddMeeting
                        },
                        onNavigateToDetail = { meetingId ->
                            currentScreen = Screen.MeetingDetail(meetingId)
                        }
                    )
                }
                is Screen.AddMeeting -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = remember { MeetingDatabase.getDatabase(context) }
                    val repository = remember { MeetingRepository(database.meetingDao()) }

                    AddMeetingScreen(
                        repository = repository,
                        onBack = {
                            currentScreen = Screen.Meetings
                            selectedTab = 1
                        }
                    )
                }
                is Screen.MeetingDetail -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val database = remember { MeetingDatabase.getDatabase(context) }
                    val repository = remember { MeetingRepository(database.meetingDao()) }

                    MeetingDetailScreen(
                        meetingId = screen.meetingId,
                        repository = repository,
                        onBack = {
                            currentScreen = Screen.Meetings
                            selectedTab = 1
                        }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(themePreferences = themePreferences)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstScreen(onNavigateToSecond: (String) -> Unit, onNavigateToThird: () -> Unit) {
    val database = FirebaseDatabase.getInstance()
    val projectsRef = database.getReference("Projects")
    val projects = remember { mutableStateListOf<Project>() }
    val isLoading = remember { mutableStateOf(true) }

    // Listener para obtener datos de Firebase
    LaunchedEffect(Unit) {
        projectsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                projects.clear()
                Log.d("Firebase", "Datos en snapshot: ${snapshot.value}")
                for (projectSnapshot in snapshot.children) {
                    Log.d("Firebase", "Procesando proyecto: ${projectSnapshot.key} -> ${projectSnapshot.value}")
                    try {
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

                        // Manejar createdAt
                        val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        // Manejar presupuesto que puede venir como String o Number
                        val presupuestoRaw = projectSnapshot.child("presupuesto").value
                        val presupuesto = when (presupuestoRaw) {
                            is Long -> presupuestoRaw
                            is Int -> presupuestoRaw.toLong()
                            is String -> presupuestoRaw.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        // Manejar avance que puede venir como String o Number
                        val avanceRaw = projectSnapshot.child("avance").value
                        val avance = when (avanceRaw) {
                            is Int -> avanceRaw
                            is Long -> avanceRaw.toInt()
                            is String -> avanceRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val project = Project(
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
                        projects.add(project)
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error al mapear el proyecto: ${projectSnapshot.value}", e)
                    }
                }
                Log.d("Firebase", "Proyectos obtenidos: $projects")
                isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos: ${error.message}")
                isLoading.value = false
            }
        })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Lista de Proyectos") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay proyectos disponibles.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(projects) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onNavigateToSecond(project.name) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Nombre: ${project.name}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Ubicación: ${project.ubicacion}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Categoría ID: ${project.categoryId}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onNavigateToThird,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Navegar a la tercera pantalla")
            }
        }
    }
}


@Composable
fun SecondScreen(text: String, onBack: () -> Unit, onNavigateToThird: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Segunda Pantalla",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Texto recibido: $text",
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Volver")
        }

        TextButton(
            onClick = onNavigateToThird,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Ver animación", fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("0") }
    var presupuesto by remember { mutableStateOf("") }
    var avance by remember { mutableStateOf("0") }
    var picUrl by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Nuevo Proyecto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Settings, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSuccessMessage) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("✅ Proyecto creado exitosamente")
                }
            }
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(error)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Información del Proyecto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Proyecto *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5
                )
            }

            item {
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("ID de Categoría (0-5) *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = presupuesto,
                    onValueChange = { presupuesto = it },
                    label = { Text("Presupuesto") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = avance,
                    onValueChange = { avance = it },
                    label = { Text("Avance (0-100) *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = picUrl,
                    onValueChange = { picUrl = it },
                    label = { Text("URL de Imagen") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Button(
                    onClick = {
                        // Validación básica
                        if (name.isBlank() || ubicacion.isBlank() || categoryId.isBlank() || avance.isBlank()) {
                            errorMessage = "Por favor completa los campos obligatorios (*)"
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                errorMessage = null
                            }
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        // Crear proyecto en Firebase
                        val database = FirebaseDatabase.getInstance()
                        val projectsRef = database.getReference("Projects")

                        val projectId = "proj_${System.currentTimeMillis()}"
                        val projectData = hashMapOf<String, Any>(
                            "id" to projectId,
                            "name" to name,
                            "ubicacion" to ubicacion,
                            "description" to description,
                            "categoryId" to categoryId,
                            "presupuesto" to (presupuesto.toLongOrNull() ?: 0),
                            "avance" to (avance.toIntOrNull() ?: 0),
                            "picUrl" to picUrl,
                            "createdAt" to System.currentTimeMillis()
                        )

                        projectsRef.child(projectId).setValue(projectData)
                            .addOnSuccessListener {
                                isLoading = false
                                showSuccessMessage = true

                                // Limpiar formulario
                                name = ""
                                ubicacion = ""
                                description = ""
                                categoryId = "0"
                                presupuesto = ""
                                avance = "0"
                                picUrl = ""

                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    showSuccessMessage = false
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = "Error: ${exception.message}"
                                scope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    errorMessage = null
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Crear Proyecto")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themePreferences: ThemePreferences) {
    val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Configuración") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Oscuro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isDarkTheme) "Activado" else "Desactivado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                themePreferences.saveDarkTheme(isChecked)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Notificaciones",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Verificación Automática",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Información sobre la frecuencia
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏰ Frecuencia:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Cada 1 hora",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 Primera vez:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "15 min después de abrir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔔 Notifica cuando:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = "Hay proyecto nuevo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Prueba Manual",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                    )
                    Text(
                        text = "Ejecuta la verificación inmediatamente sin esperar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    var showTestMessage by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            WorkManagerScheduler.executeNow(context)
                            showTestMessage = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                showTestMessage = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ejecutar prueba ahora")
                    }

                    if (showTestMessage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Worker ejecutado. Revisa los logs y notificaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Más opciones de configuración estarán disponibles próximamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// Previews
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FirstScreenPreview() {
    DataGovTheme {
        FirstScreen(onNavigateToSecond = {}, onNavigateToThird = {})
    }
}

@Preview(device = "spec:parent=pixel_7_pro,orientation=landscape", showSystemUi = true, showBackground = false)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Composable
fun SecondScreenPreview() {
    DataGovTheme {
        SecondScreen(text = "Texto de ejemplo", onBack = {}, onNavigateToThird = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ThirdScreenPreview() {
    DataGovTheme {
        ThirdScreen(onBack = {})
    }
}

