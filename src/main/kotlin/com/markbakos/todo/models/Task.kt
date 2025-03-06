package com.markbakos.todo.models

data class Task(
    val id: String = generateUniqueID(),
    var title: String,
    var description: String = "",
    var tags: MutableList<String> = mutableListOf(),
    var priority: Priority = Priority.MEDIUM,
    var status: TaskStatus = TaskStatus.TODO
) {
    enum class Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum class TaskStatus {
        TODO, IN_PROGRESS, DONE
    }

    companion object {
        private var idCounter = 0

        fun generateUniqueID(): String {
            return "TASK_${++idCounter}"
        }

        fun updateIdCounter(maxId: Int) {
            if (maxId > idCounter) {
                idCounter = maxId
            }
        }

        fun getCurrentIdCounter(): Int {
            return idCounter
        }

        fun extractIdNumber(id: String): Int {
            return id.substringAfter("TASK_").toIntOrNull() ?: 0
        }
    }
}
