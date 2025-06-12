package com.markbakos.todo.models

import java.time.LocalDateTime

data class Task(
    val id: String = generateUniqueID(),
    var priority: Priority = Priority.MEDIUM,
    var tags: MutableList<String> = mutableListOf(),
    var description: String = "",
    var status: TaskStatus = TaskStatus.TODO,
    var date: LocalDateTime = LocalDateTime.now(),
    var finishDate: LocalDateTime? = null,
    var link: String? = null
) {
    enum class Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum class TaskStatus {
        TODO, IN_PROGRESS, DONE
    }

    fun getTagsAsString(): String {
        return tags.joinToString { ", " }
    }

    fun setTagsFromString(tagsString: String) {
        tags = tagsString.split(",")
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toMutableList()
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
