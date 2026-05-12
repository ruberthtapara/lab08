package com.example.lab08
// Asegúrate de tener esta línea arriba en MainActivity.kt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lab08.ui.theme.Lab08Theme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.room.Room
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.lab08.com.example.lab08.Task



// ID ÚNICO PARA EL CANAL
const val CHANNEL_ID = "task_notifications"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CREAR EL CANAL DE NOTIFICACIONES
        createNotificationChannel()

        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java, "task_db"
        ).fallbackToDestructiveMigration().build()

        val taskDao = db.taskDao()
        val viewModel = TaskViewModel(taskDao)

        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                // 2. SOLICITAR PERMISOS (PARA ANDROID 13+)
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Aquí puedes manejar si el usuario aceptó o no
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                TaskScreen(viewModel)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de Tareas"
            val descriptionText = "Notificaciones para nuevas tareas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

// 3. FUNCIÓN PARA DISPARAR LA NOTIFICACIÓN
fun sendTaskNotification(context: Context, taskTitle: String) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono del sistema
        .setContentTitle("¡Tarea Agregada!")
        .setContentText("No olvides completar: $taskTitle")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    var newTaskDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Media") }
    var selectedCategory by remember { mutableStateOf("General") }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    "Mis Tareas Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Ordenar por:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { viewModel.loadTasks("NOMBRE") }) { Text("Nombre") }
                    TextButton(onClick = { viewModel.loadTasks("FECHA") }) { Text("Fecha") }
                    TextButton(onClick = { viewModel.loadTasks("ESTADO") }) { Text("Estado") }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            Card(modifier = Modifier.padding(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (editingTask != null) "Editando Tarea" else "Nueva Tarea",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = newTaskDescription,
                        onValueChange = { newTaskDescription = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- SELECCIÓN DE PRIORIDAD ACTUALIZADA ---
                    Row(
                        Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedPriority = "Alta" },
                            colors = ButtonDefaults.buttonColors(if(selectedPriority == "Alta") Color.Red else Color.Gray)
                        ) { Text("Alta") }

                        Button(
                            onClick = { selectedPriority = "Media" },
                            colors = ButtonDefaults.buttonColors(if(selectedPriority == "Media") Color.Blue else Color.Gray)
                        ) { Text("Media") }

                        // NUEVO BOTÓN: BAJA
                        Button(
                            onClick = { selectedPriority = "Baja" },
                            colors = ButtonDefaults.buttonColors(if(selectedPriority == "Baja") Color(0xFF4CAF50) else Color.Gray)
                        ) { Text("Baja") }
                    }

                    Button(
                        onClick = {
                            if (newTaskDescription.isNotEmpty()) {
                                if (editingTask != null) {
                                    viewModel.updateTask(editingTask!!, newTaskDescription, selectedPriority, selectedCategory)
                                    editingTask = null
                                } else {
                                    viewModel.addTask(newTaskDescription, selectedPriority, selectedCategory)
                                    sendTaskNotification(context, newTaskDescription)
                                }
                                newTaskDescription = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (editingTask != null) Icons.Default.Edit else Icons.Default.Add, null)
                        Text(if (editingTask != null) " Actualizar" else " Agregar")
                    }

                    if (editingTask != null) {
                        TextButton(
                            onClick = {
                                editingTask = null
                                newTaskDescription = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cancelar edición", color = Color.Red) }
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks) { task ->
                    TaskItemView(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = {
                            editingTask = task
                            newTaskDescription = task.description
                            selectedPriority = task.priority
                            selectedCategory = task.category
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItemView(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Definimos el color de fondo incluyendo la nueva prioridad BAJA
    val backgroundColor = when (task.priority) {
        "Alta" -> Color(0xFFFFEBEE) // Rojo claro
        "Media" -> Color(0xFFE3F2FD) // Azul claro
        "Baja" -> Color(0xFFE8F5E9)  // Verde claro (NUEVO)
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Punto indicador de estado
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) Color.Green else Color.Red)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Información de la tarea
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (task.isCompleted) "Completada" else "Pendiente - ${task.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isCompleted) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            // 3. Botones de acción
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Blue)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }

            // 4. Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Lab08Theme {
        Greeting("Android")
    }
}