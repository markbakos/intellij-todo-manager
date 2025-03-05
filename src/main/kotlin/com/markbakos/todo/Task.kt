package com.markbakos.todo

data class Task(
    val id: String = generateUniqueID(),
    var title: String,
    var description: String = "",
    var tags: MutableList<String> = mutableListOf(),
    var priority: Priority = Priority.MEDIUM,
    var Status: TaskStatus = TaskStatus.TODO
) {
    enum class Priority {
        LOW, MEDIUM, HIGH
    }

    enum class TaskStatus {
        TODO, IN_PROGRESS, DONE
    }

    companion object {
        private var idCounter = 0

        fun generateUniqueID(): String {
            return "TASK_${++idCounter}"
        }
    }
}
