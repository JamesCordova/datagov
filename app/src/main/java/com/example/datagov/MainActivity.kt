package com.example.datagov

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.content.res.Configuration
import com.example.datagov.ui.theme.DataGovTheme
import com.example.datagov.data.ThemePreferences
import com.example.datagov.data.Project
import com.example.datagov.data.Category
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.datagov.data.MeetingDatabase
import com.example.datagov.data.MeetingRepository
import com.example.datagov.ui.meetings.MeetingsScreen
import com.example.datagov.ui.meetings.AddMeetingScreen
import com.example.datagov.ui.meetings.MeetingDetailScreen
import com.example.datagov.workers.WorkManagerScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.datagov.services.TimerService
import android.content.Intent
import android.os.Build
// import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

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
    object Dashboard : NavigationItem("dashboard", "Dashboard", Icons.Filled.Home, Icons.Outlined.Home)
    object Home : NavigationItem("home", "Proyectos", Icons.Filled.List, Icons.Outlined.List)
    object Meetings : NavigationItem("meetings", "Meetings", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Settings : NavigationItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

sealed class Screen {
    object First : Screen()
    data class Second(val projectId: String) : Screen()
    object Third : Screen()
    object Dashboard : Screen()
    object Meetings : Screen()
    object AddMeeting : Screen()
    data class MeetingDetail(val meetingId: Long) : Screen()
    object Settings : Screen()
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier, themePreferences: ThemePreferences) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedTab by remember { mutableStateOf(0) }

    val navigationItems = listOf(
        NavigationItem.Dashboard,
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
                                0 -> Screen.Dashboard
                                1 -> Screen.First
                                2 -> Screen.Meetings
                                3 -> Screen.Settings
                                else -> Screen.Dashboard
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
                        onNavigateToSecond = { projectId ->
                            currentScreen = Screen.Second(projectId)
                        },
                        onNavigateToThird = {
                            currentScreen = Screen.Third
                        }
                    )
                }
                is Screen.Second -> {
                    SecondScreen(
                        projectId = screen.projectId,
                        onBack = {
                            currentScreen = Screen.First
                            selectedTab = 1
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
                            selectedTab = 1
                        }
                    )
                }
                is Screen.Dashboard -> {
                    DashboardScreen()
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
                            selectedTab = 2
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
                            selectedTab = 2
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
    val categoriesRef = database.getReference("Category")
    val projects = remember { mutableStateListOf<Project>() }
    val categories = remember { mutableStateListOf<Category>() }
    val isLoading = remember { mutableStateOf(true) }

    // Estados de búsqueda y filtros
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedLocationFilter by remember { mutableStateOf("") }

    // Listener para obtener categorías de Firebase
    LaunchedEffect(Unit) {
        categoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    try {
                        val id = categorySnapshot.child("id").getValue(String::class.java) ?: ""
                        val title = categorySnapshot.child("title").getValue(String::class.java) ?: ""
                        val picUrl = categorySnapshot.child("picUrl").getValue(String::class.java) ?: ""

                        val category = Category(
                            id = id,
                            title = title,
                            picUrl = picUrl
                        )
                        categories.add(category)
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error al cargar categoría: ${categorySnapshot.value}", e)
                    }
                }
                Log.d("Firebase", "Categorías obtenidas: ${categories.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer categorías: ${error.message}")
            }
        })
    }

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
                            is Double -> presupuestoRaw.toLong()
                            is Long -> presupuestoRaw
                            is Int -> presupuestoRaw.toLong()
                            is String -> presupuestoRaw.toDoubleOrNull()?.toLong() ?: 0L
                            else -> 0L
                        }

                        // Manejar avance que puede venir como String o Number
                        val avanceRaw = projectSnapshot.child("avance").value
                        val avance = when (avanceRaw) {
                            is Double -> avanceRaw.toInt()
                            is Int -> avanceRaw
                            is Long -> avanceRaw.toInt()
                            is String -> avanceRaw.toDoubleOrNull()?.toInt() ?: 0
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

    // Filtrar proyectos según búsqueda y filtros
    val filteredProjects = remember(projects, searchQuery, selectedCategoryFilter, selectedLocationFilter) {
        derivedStateOf {
            projects.filter { project ->
                val matchesSearch = searchQuery.isEmpty() ||
                    project.name.contains(searchQuery, ignoreCase = true) ||
                    project.ubicacion.contains(searchQuery, ignoreCase = true) ||
                    categories.find { it.id == project.categoryId }?.title?.contains(searchQuery, ignoreCase = true) == true

                val matchesCategory = selectedCategoryFilter == null || project.categoryId == selectedCategoryFilter

                val matchesLocation = selectedLocationFilter.isEmpty() ||
                    project.ubicacion.contains(selectedLocationFilter, ignoreCase = true)

                matchesSearch && matchesCategory && matchesLocation
            }
        }
    }.value

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Proyectos") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Barra de búsqueda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Campo de búsqueda principal
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nombre, ubicación o categoría...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar búsqueda"
                                        )
                                    }
                                }
                                IconButton(onClick = { showFilters = !showFilters }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Filtros",
                                        tint = if (selectedCategoryFilter != null || selectedLocationFilter.isNotEmpty())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Panel de filtros expandible
                    if (showFilters) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filtro por categoría
                        Text(
                            text = "Filtrar por categoría",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chips de categorías
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Opción "Todas"
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Todas") }
                            )

                            // Chips de categorías
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategoryFilter == category.id,
                                    onClick = {
                                        selectedCategoryFilter = if (selectedCategoryFilter == category.id) null else category.id
                                    },
                                    label = { Text(category.title) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filtro por ubicación
                        Text(
                            text = "Filtrar por ubicación",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = selectedLocationFilter,
                            onValueChange = { selectedLocationFilter = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Lima, Arequipa, Cusco...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Ubicación"
                                )
                            },
                            trailingIcon = {
                                if (selectedLocationFilter.isNotEmpty()) {
                                    IconButton(onClick = { selectedLocationFilter = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Botón para limpiar todos los filtros
                        if (selectedCategoryFilter != null || selectedLocationFilter.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    selectedCategoryFilter = null
                                    selectedLocationFilter = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Limpiar filtros")
                            }
                        }
                    }
                }
            }

            // Resultados de búsqueda
            if (isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isEmpty() && selectedCategoryFilter == null && selectedLocationFilter.isEmpty())
                                "No hay proyectos disponibles"
                            else
                                "No se encontraron proyectos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty() || selectedCategoryFilter != null || selectedLocationFilter.isNotEmpty()) {
                            Text(
                                text = "Intenta con otros filtros",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Contador de resultados
                Text(
                    text = "${filteredProjects.size} proyecto${if (filteredProjects.size != 1) "s" else ""} encontrado${if (filteredProjects.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProjects) { project ->
                        // Buscar la categoría correspondiente
                        val category = categories.find { it.id == project.categoryId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSecond(project.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Nombre del proyecto sin etiqueta
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Ubicación con ícono
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Ubicación",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = project.ubicacion,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Categoría con nombre real (sin etiqueta)
                                Text(
                                    text = category?.title ?: "Sin categoría", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Espacio final para el FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FloatingActionButton
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FloatingActionButton(
                onClick = onNavigateToThird,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar proyecto"
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondScreen(projectId: String, onBack: () -> Unit, onNavigateToThird: () -> Unit) {
    val database = FirebaseDatabase.getInstance()
    val projectsRef = database.getReference("Projects")
    val categoriesRef = database.getReference("Category")
    
    var project by remember { mutableStateOf<Project?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    // Cargar el proyecto desde Firebase
    LaunchedEffect(projectId) {
        projectsRef.child(projectId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val id = snapshot.key ?: ""
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val ubicacion = snapshot.child("ubicacion").getValue(String::class.java) ?: ""
                    val description = snapshot.child("description").getValue(String::class.java) ?: ""
                    val picUrl = snapshot.child("picUrl").getValue(String::class.java) ?: ""

                    val categoryIdRaw = snapshot.child("categoryId").value
                    val categoryId = when (categoryIdRaw) {
                        is String -> categoryIdRaw
                        is Long -> categoryIdRaw.toString()
                        is Int -> categoryIdRaw.toString()
                        else -> "0"
                    }

                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                    val presupuestoRaw = snapshot.child("presupuesto").value
                    val presupuesto = when (presupuestoRaw) {
                        is Double -> presupuestoRaw.toLong()
                        is Long -> presupuestoRaw
                        is Int -> presupuestoRaw.toLong()
                        is String -> presupuestoRaw.toDoubleOrNull()?.toLong() ?: 0L
                        else -> 0L
                    }

                    val avanceRaw = snapshot.child("avance").value
                    val avance = when (avanceRaw) {
                        is Double -> avanceRaw.toInt()
                        is Int -> avanceRaw
                        is Long -> avanceRaw.toInt()
                        is String -> avanceRaw.toDoubleOrNull()?.toInt() ?: 0
                        else -> 0
                    }

                    project = Project(
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
                    
                    // Cargar la categoría
                    categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(catSnapshot: DataSnapshot) {
                            for (categorySnapshot in catSnapshot.children) {
                                val catId = categorySnapshot.child("id").getValue(String::class.java) ?: ""
                                if (catId == categoryId) {
                                    val title = categorySnapshot.child("title").getValue(String::class.java) ?: ""
                                    val catPicUrl = categorySnapshot.child("picUrl").getValue(String::class.java) ?: ""
                                    category = Category(id = catId, title = title, picUrl = catPicUrl)
                                    break
                                }
                            }
                            isLoading.value = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                            isLoading.value = false
                        }
                    })
                } catch (e: Exception) {
                    Log.e("Firebase", "Error al cargar proyecto: ${snapshot.value}", e)
                    isLoading.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer proyecto: ${error.message}")
                isLoading.value = false
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Proyecto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (project != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Imagen del proyecto
                if (project!!.picUrl.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Imagen del Proyecto",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // TODO: Descomentar cuando Gradle sincronice Coil
                        // AsyncImage(
                        //     model = project!!.picUrl,
                        //     contentDescription = "Imagen del proyecto",
                        //     modifier = Modifier.fillMaxSize(),
                        //     contentScale = ContentScale.Crop
                        // )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Nombre del proyecto
                Text(
                    text = project!!.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Categoría
                Text(
                    text = category?.title ?: "Sin categoría",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Ubicación con ícono
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project!!.ubicacion,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Descripción
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = project!!.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Información financiera y de avance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Presupuesto
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Presupuesto",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "S/ ${String.format("%,.0f", project!!.presupuesto.toDouble())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Avance
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Avance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${project!!.avance}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de progreso
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Progreso del Proyecto",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { project!!.avance / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Fecha de creación
                Text(
                    text = "Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(project!!.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Proyecto no encontrado")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var presupuestoText by remember { mutableStateOf("") }
    var avanceSlider by remember { mutableFloatStateOf(0f) }
    var picUrl by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados para cargar categorías
    val categories = remember { mutableStateListOf<Category>() }
    var categoriesLoaded by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val database = FirebaseDatabase.getInstance()

    // Cargar categorías desde Firebase
    LaunchedEffect(Unit) {
        val categoriesRef = database.getReference("Category")
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    try {
                        val id = categorySnapshot.child("id").getValue(String::class.java) ?: ""
                        val title = categorySnapshot.child("title").getValue(String::class.java) ?: ""
                        val catPicUrl = categorySnapshot.child("picUrl").getValue(String::class.java) ?: ""
                        categories.add(Category(id = id, title = title, picUrl = catPicUrl))
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error al cargar categoría", e)
                    }
                }
                categoriesLoaded = true
                if (categories.isNotEmpty() && selectedCategoryId.isEmpty()) {
                    selectedCategoryId = categories[0].id
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al cargar categorías: ${error.message}")
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Nuevo Proyecto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSuccessMessage) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Proyecto creado exitosamente")
                    }
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
            // Encabezado
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nuevo Proyecto",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Completa la información del proyecto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Sección: Información Básica
            item {
                Text(
                    text = "Información Básica",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Proyecto *") },
                    placeholder = { Text("Ej: Construcción de Hospital Regional") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación *") },
                    placeholder = { Text("Ej: Lima, Arequipa, Cusco") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe el proyecto...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 4,
                    maxLines = 8
                )
            }

            // Sección: Categoría
            item {
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory && !isLoading }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.title ?: "Selecciona una categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría del Proyecto *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.title) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }

            // Sección: Información Financiera
            item {
                Text(
                    text = "Información Financiera",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = presupuestoText,
                    onValueChange = {
                        // Solo permitir números
                        if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                            presupuestoText = it
                        }
                    },
                    label = { Text("Presupuesto (S/)") },
                    placeholder = { Text("Ej: 1500000") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    leadingIcon = {
                        Text(
                            text = "S/",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    supportingText = {
                        if (presupuestoText.isNotEmpty()) {
                            val valor = presupuestoText.toDoubleOrNull() ?: 0.0
                            Text("≈ S/ ${String.format(java.util.Locale("es", "PE"), "%,.2f", valor)}")
                        }
                    }
                )
            }

            // Sección: Avance del Proyecto
            item {
                Text(
                    text = "Avance del Proyecto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Porcentaje de avance:",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${avanceSlider.toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = avanceSlider,
                            onValueChange = { avanceSlider = it },
                            valueRange = 0f..100f,
                            steps = 99,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { avanceSlider / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                avanceSlider < 30 -> MaterialTheme.colorScheme.error
                                avanceSlider < 70 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }

            // Sección: Imagen (Opcional)
            item {
                Text(
                    text = "Imagen del Proyecto (Opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = picUrl,
                    onValueChange = { picUrl = it },
                    label = { Text("URL de Imagen") },
                    placeholder = { Text("https://ejemplo.com/imagen.png") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )
            }

            // Botones de acción
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Button(
                    onClick = {
                        // Validación mejorada
                        when {
                            name.isBlank() -> {
                                errorMessage = "❌ El nombre del proyecto es obligatorio"
                            }
                            name.length < 10 -> {
                                errorMessage = "❌ El nombre debe tener al menos 10 caracteres"
                            }
                            ubicacion.isBlank() -> {
                                errorMessage = "❌ La ubicación es obligatoria"
                            }
                            selectedCategoryId.isEmpty() -> {
                                errorMessage = "❌ Debes seleccionar una categoría"
                            }
                            presupuestoText.isNotEmpty() && presupuestoText.toDoubleOrNull() == null -> {
                                errorMessage = "❌ El presupuesto debe ser un número válido"
                            }
                            else -> {
                                isLoading = true
                                errorMessage = null

                                // Crear proyecto en Firebase
                                val projectsRef = database.getReference("Projects")
                                val projectId = "proj_${System.currentTimeMillis()}"

                                val projectData = hashMapOf<String, Any>(
                                    "id" to projectId,
                                    "name" to name.trim(),
                                    "ubicacion" to ubicacion.trim(),
                                    "description" to (description.trim().ifEmpty { "Descripción no disponible" }),
                                    "categoryId" to selectedCategoryId,
                                    "presupuesto" to (presupuestoText.toDoubleOrNull() ?: 0.0),
                                    "avance" to avanceSlider.toDouble(),
                                    "picUrl" to (picUrl.trim().ifEmpty { "https://example.com/default-pic.png" }),
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
                                        selectedCategoryId = if (categories.isNotEmpty()) categories[0].id else ""
                                        presupuestoText = ""
                                        avanceSlider = 0f
                                        picUrl = ""

                                        scope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            showSuccessMessage = false
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = "❌ Error: ${exception.message}"
                                        scope.launch {
                                            kotlinx.coroutines.delay(3000)
                                            errorMessage = null
                                        }
                                    }
                            }
                        }

                        if (errorMessage != null) {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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

            // Espacio final
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themePreferences: ThemePreferences) {
    val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Estado del temporizador (solo para saber si está activo o no)
    var isTimerRunning by remember { mutableStateOf(false) }

    // Verificar estado inicial y cuando se muestra la pantalla
    LaunchedEffect(Unit) {
        val running = TimerService.isServiceRunning(context)
        Log.d("SettingsScreen", "Estado inicial del temporizador: $running")
        isTimerRunning = running

        // Verificar estado periódicamente cada segundo para asegurar sincronización
        while (true) {
            kotlinx.coroutines.delay(1000)
            val currentState = TimerService.isServiceRunning(context)
            if (isTimerRunning != currentState) {
                Log.d("SettingsScreen", "Estado actualizado por polling: $currentState")
                isTimerRunning = currentState
            }
        }
    }

    // Broadcast Receiver para recibir actualizaciones del temporizador
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val running = it.getBooleanExtra(TimerService.EXTRA_IS_RUNNING, false)
                    Log.d("SettingsScreen", "Broadcast recibido - isRunning: $running")
                    isTimerRunning = running
                }
            }
        }
        
        val filter = IntentFilter(TimerService.BROADCAST_TIMER_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

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
                .verticalScroll(rememberScrollState())
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Frecuencia:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Primera vez:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Notifica cuando:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Worker ejecutado. Revisa los logs y notificaciones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Temporizador
            Text(
                text = "Temporizador de Prueba",
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
                        text = "Temporizador con notificación persistente",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Prueba de notificación con botones de control",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Estado del temporizador (solo mostrar cuando está activo)
                    if (isTimerRunning) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Temporizador activo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Revisa la notificación para ver el tiempo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Botones de control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Log.d("SettingsScreen", "Botón Iniciar presionado - Estado actual: $isTimerRunning")
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_START
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                // Actualizar estado inmediatamente
                                isTimerRunning = true
                                Log.d("SettingsScreen", "Estado actualizado a: $isTimerRunning")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTimerRunning
                        ) {
                            Text("Iniciar")
                        }

                        Button(
                            onClick = {
                                Log.d("SettingsScreen", "Botón Detener presionado - Estado actual: $isTimerRunning")
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_STOP
                                }
                                context.startService(intent)
                                // Actualizar estado inmediatamente
                                isTimerRunning = false
                                Log.d("SettingsScreen", "Estado actualizado a: $isTimerRunning")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isTimerRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Detener")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• La notificación aparecerá cuando inicies el temporizador\n" +
                               "• Usa los botones en la notificación para pausar/reanudar\n" +
                               "• El botón 'Detener' eliminará la notificación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val database = FirebaseDatabase.getInstance()
    val projectsRef = database.getReference("Projects")
    val categoriesRef = database.getReference("Category")
    val projects = remember { mutableStateListOf<Project>() }
    val categories = remember { mutableStateListOf<Category>() }
    val isLoading = remember { mutableStateOf(true) }

    // Cargar categorías
    LaunchedEffect(Unit) {
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    try {
                        val id = categorySnapshot.child("id").getValue(String::class.java) ?: ""
                        val title = categorySnapshot.child("title").getValue(String::class.java) ?: ""
                        val picUrl = categorySnapshot.child("picUrl").getValue(String::class.java) ?: ""
                        categories.add(Category(id = id, title = title, picUrl = picUrl))
                    } catch (e: Exception) {
                        Log.e("Dashboard", "Error al cargar categoría", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Dashboard", "Error al leer categorías: ${error.message}")
            }
        })
    }

    // Cargar proyectos
    LaunchedEffect(Unit) {
        projectsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                projects.clear()
                for (projectSnapshot in snapshot.children) {
                    try {
                        val id = projectSnapshot.key ?: ""
                        val name = projectSnapshot.child("name").getValue(String::class.java) ?: ""
                        val ubicacion = projectSnapshot.child("ubicacion").getValue(String::class.java) ?: ""
                        val description = projectSnapshot.child("description").getValue(String::class.java) ?: ""
                        val picUrl = projectSnapshot.child("picUrl").getValue(String::class.java) ?: ""

                        val categoryIdRaw = projectSnapshot.child("categoryId").value
                        val categoryId = when (categoryIdRaw) {
                            is String -> categoryIdRaw
                            is Long -> categoryIdRaw.toString()
                            is Int -> categoryIdRaw.toString()
                            else -> "0"
                        }

                        val createdAt = projectSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val presupuestoRaw = projectSnapshot.child("presupuesto").value
                        val presupuesto = when (presupuestoRaw) {
                            is Double -> presupuestoRaw.toLong()
                            is Long -> presupuestoRaw
                            is Int -> presupuestoRaw.toLong()
                            is String -> presupuestoRaw.toDoubleOrNull()?.toLong() ?: 0L
                            else -> 0L
                        }

                        val avanceRaw = projectSnapshot.child("avance").value
                        val avance = when (avanceRaw) {
                            is Double -> avanceRaw.toInt()
                            is Int -> avanceRaw
                            is Long -> avanceRaw.toInt()
                            is String -> avanceRaw.toDoubleOrNull()?.toInt() ?: 0
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
                        Log.e("Dashboard", "Error al mapear proyecto", e)
                    }
                }
                isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Dashboard", "Error al leer proyectos: ${error.message}")
                isLoading.value = false
            }
        })
    }

    // Cálculos de estadísticas
    val totalProyectos = projects.size
    val presupuestoTotal = projects.sumOf { it.presupuesto }
    val avancePromedio = if (projects.isNotEmpty()) projects.map { it.avance }.average() else 0.0
    val proyectosCompletados = projects.count { it.avance >= 100 }
    val proyectosEnProgreso = projects.count { it.avance > 0 && it.avance < 100 }
    val proyectosNoIniciados = projects.count { it.avance == 0 }

    // Proyectos por categoría
    val proyectosPorCategoria = categories.map { category ->
        category to projects.count { it.categoryId == category.id }
    }.sortedByDescending { it.second }

    // Top 5 proyectos con mayor presupuesto
    val topProyectosPorPresupuesto = projects.sortedByDescending { it.presupuesto }.take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }
            )
        }
    ) { innerPadding ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column {
                                    Text(
                                        text = "Resumen General",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Vista general de todos los proyectos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Tarjetas de estadísticas principales
                item {
                    Text(
                        text = "Estadísticas Principales",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total de proyectos
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$totalProyectos",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Proyectos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Avance promedio
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${String.format("%.1f", avancePromedio)}%",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Avance Promedio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    // Presupuesto total
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Presupuesto Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "S/ ${String.format(java.util.Locale("es", "PE"), "%,.0f", presupuestoTotal.toDouble())}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Estado de los proyectos
                item {
                    Text(
                        text = "Estado de Proyectos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Completados
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Completados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$proyectosCompletados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider()

                            // En progreso
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "En Progreso",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$proyectosEnProgreso",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            HorizontalDivider()

                            // No iniciados
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "No Iniciados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$proyectosNoIniciados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            // Gráfica simple de barras
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxValue = maxOf(proyectosCompletados, proyectosEnProgreso, proyectosNoIniciados).toFloat()

                            if (maxValue > 0) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Barra completados
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(proyectosCompletados / maxValue)
                                                .height(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        if (proyectosCompletados / maxValue < 1f) {
                                            Box(modifier = Modifier.weight(1f - (proyectosCompletados / maxValue)))
                                        }
                                    }

                                    // Barra en progreso
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(proyectosEnProgreso / maxValue)
                                                .height(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondary,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        if (proyectosEnProgreso / maxValue < 1f) {
                                            Box(modifier = Modifier.weight(1f - (proyectosEnProgreso / maxValue)))
                                        }
                                    }

                                    // Barra no iniciados
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(proyectosNoIniciados / maxValue)
                                                .height(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.tertiary,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        if (proyectosNoIniciados / maxValue < 1f) {
                                            Box(modifier = Modifier.weight(1f - (proyectosNoIniciados / maxValue)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Proyectos por categoría
                item {
                    Text(
                        text = "Proyectos por Categoría",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (proyectosPorCategoria.isNotEmpty()) {
                                val maxCategoryValue = proyectosPorCategoria.firstOrNull()?.second?.toFloat() ?: 1f

                                proyectosPorCategoria.take(8).forEach { (category, count) ->
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = category.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(count / maxCategoryValue)
                                                    .height(8.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            if (count / maxCategoryValue < 1f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f - (count / maxCategoryValue))
                                                        .height(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No hay datos disponibles",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Espacio final
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
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
        SecondScreen(projectId = "proj_001", onBack = {}, onNavigateToThird = {})
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

