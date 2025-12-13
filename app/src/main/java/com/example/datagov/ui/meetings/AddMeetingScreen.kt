package com.example.datagov.ui.meetings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.datagov.data.Meeting
import com.example.datagov.data.MeetingRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeetingScreen(
    repository: MeetingRepository,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var municipality by remember { mutableStateOf("") }
    var specificLocation by remember { mutableStateOf("") }
    var estimatedAttendees by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Reunión") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Información de la Reunión",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la reunión") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dateTime,
                onValueChange = { dateTime = it },
                label = { Text("Fecha y hora") },
                placeholder = { Text("dd/MM/yyyy HH:mm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Ubicación",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            OutlinedTextField(
                value = municipality,
                onValueChange = { municipality = it },
                label = { Text("Municipio (Distrito)") },
                placeholder = { Text("Ej: San José, Heredia, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = specificLocation,
                onValueChange = { specificLocation = it },
                label = { Text("Lugar específico") },
                placeholder = { Text("Ej: Sala de juntas, Auditorio, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = estimatedAttendees,
                onValueChange = { estimatedAttendees = it },
                label = { Text("Número estimado de asistentes") },
                placeholder = { Text("Ej: 20") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validación
                    when {
                        title.isBlank() -> {
                            showError = true
                            errorMessage = "El título es obligatorio"
                        }
                        dateTime.isBlank() -> {
                            showError = true
                            errorMessage = "La fecha y hora son obligatorias"
                        }
                        municipality.isBlank() -> {
                            showError = true
                            errorMessage = "El municipio es obligatorio"
                        }
                        specificLocation.isBlank() -> {
                            showError = true
                            errorMessage = "El lugar específico es obligatorio"
                        }
                        estimatedAttendees.isBlank() || estimatedAttendees.toIntOrNull() == null -> {
                            showError = true
                            errorMessage = "El número de asistentes debe ser un número válido"
                        }
                        else -> {
                            showError = false
                            scope.launch {
                                val meeting = Meeting(
                                    title = title,
                                    dateTime = dateTime,
                                    municipality = municipality,
                                    specificLocation = specificLocation,
                                    estimatedAttendees = estimatedAttendees.toInt()
                                )
                                repository.insertMeeting(meeting)
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Guardar Reunión")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}

