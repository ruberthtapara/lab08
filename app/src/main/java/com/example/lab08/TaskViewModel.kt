package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab08.com.example.lab08.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    // Guardamos el criterio de orden actual (por defecto Fecha)
    private var currentSort = "FECHA"

    init {
        loadTasks()
    }

    // Función de carga con lógica de ordenamiento local
    fun loadTasks(sortBy: String = currentSort) {
        currentSort = sortBy
        viewModelScope.launch(Dispatchers.IO) {
            val listaBase = dao.getAllTasks()
            _tasks.value = when (sortBy) {
                "NOMBRE" -> listaBase.sortedBy { it.description.lowercase() }
                "ESTADO" -> listaBase.sortedBy { it.isCompleted }
                else -> listaBase.sortedByDescending { it.createdAt }
            }
        }
    }

    // Agregar tarea con soporte para prioridad "Baja"
    fun addTask(description: String, priority: String, category: String, repeatDays: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTask = Task(
                description = description,
                priority = priority,
                category = category,
                repeatInterval = repeatDays,
                createdAt = System.currentTimeMillis()
            )
            dao.insertTask(newTask)
            loadTasks()
        }
    }

    // Cambiar estado de completado y manejar tareas recurrentes
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowCompleted = !task.isCompleted
            val updatedTask = task.copy(isCompleted = isNowCompleted)
            dao.updateTask(updatedTask)

            // Si se completa y es recurrente, crea una copia nueva
            if (isNowCompleted && task.repeatInterval > 0) {
                val recurringTask = task.copy(
                    id = 0,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                )
                dao.insertTask(recurringTask)
            }
            loadTasks()
        }
    }

    // Actualizar una tarea existente
    fun updateTask(task: Task, newDescription: String, newPriority: String, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(
                description = newDescription,
                priority = newPriority,
                category = newCategory
            )
            dao.updateTask(updatedTask)
            loadTasks()
        }
    }

    // Eliminar una tarea
    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTask(task)
            loadTasks()
        }
    }

    // Borrar todo el historial
    fun deleteAllTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllTasks()
            loadTasks()
        }
    }
}