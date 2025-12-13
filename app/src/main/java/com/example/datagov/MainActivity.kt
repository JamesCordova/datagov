package com.example.datagov

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
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
                        val categoryId = projectSnapshot.child("categoryId").getValue(Long::class.java)?.toString() ?: ""
                        val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val project = Project(
                            id = id,
                            name = name,
                            ubicacion = ubicacion,
                            categoryId = categoryId,
                            createdAt = createdAt
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

@Composable
fun ThirdScreen(onBack: () -> Unit) {
    var isAnimating by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "circleAnimation")

    val animatedSize by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sizeAnimation"
    )

    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF6200EE),
        targetValue = Color(0xFF03DAC5),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorAnimation"
    )

    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    val currentSize = if (isAnimating) animatedSize else 100f
    val currentColor = if (isAnimating) animatedColor else Color(0xFF6200EE)
    val currentAlpha = if (isAnimating) animatedAlpha else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Animación de Círculo Mejorada",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Canvas(
            modifier = Modifier
                .size(350.dp)
                .padding(16.dp)
        ) {
            drawCircle(
                color = currentColor.copy(alpha = currentAlpha),
                radius = currentSize
            )
        }

        Button(
            onClick = { isAnimating = !isAnimating },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(text = if (isAnimating) "Detener Animación" else "Animar Círculo")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Volver al Inicio")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themePreferences: ThemePreferences) {
    val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showTestMessage by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Configuración") }
            )
        },
        snackbarHost = {
            if (showTestMessage) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Worker ejecutado. Revisa los logs y notificaciones.")
                }
            }
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
                        text = "Probar notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Ejecuta manualmente la verificación de nuevos proyectos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
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

