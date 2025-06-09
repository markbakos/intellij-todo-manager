package com.markbakos.todo.saving

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.markbakos.todo.models.Task
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Type

@Service(Service.Level.PROJECT)
class TaskSavingService(private val project: Project) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val taskListType: Type = object : TypeToken<List<Task>>() {}.type
    private val todoDir: File
        get() {
            val projectDir = project.guessProjectDir()
            val dir = File(projectDir?.path ?: "", ".todo")
            if (!dir.exists()) {
                dir.mkdir()
            }
            return dir
        }

    private val todoFile: File
        get() = File(todoDir, "tasks.json")

    fun saveTasks(tasks: List<Task>) {
        try {
            FileWriter(todoFile).use { writer -> gson.toJson(tasks, writer) }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadTasks(): List<Task> {
        val savedCounter = loadIdCounter()

        if (!todoFile.exists()) {
            Task.updateIdCounter(savedCounter)
            return emptyList()
        }

        try {
            FileReader(todoFile).use {reader ->
                val tasks: List<Task> = gson.fromJson(reader, taskListType)

                var maxId = savedCounter
                tasks.forEach { task ->
                    val idNumber = Task.extractIdNumber(task.id)
                    if (idNumber > maxId) {
                        maxId = idNumber
                    }
                }

                Task.updateIdCounter(maxId)

                return tasks
            }
        }
        catch (e:IOException) {
            e.printStackTrace()
            Task.updateIdCounter(savedCounter)
            return emptyList()
        }
    }

    data class TaskForDeserialization(val id: String)

    private fun loadIdCounter(): Int {
        if (!todoFile.exists()) {
            return 0
        }

        try {
            FileReader(todoFile).use { reader ->
                val jsonText = reader.readText()
                val gson = Gson()
                val taskArray = gson.fromJson(jsonText, Array<TaskForDeserialization>::class.java)

                var maxID = 0

                taskArray.forEach { task ->
                    if (task.id.startsWith("TASK_")) {
                        val idNumber = task.id.substringAfter("TASK_").toIntOrNull()
                        if (idNumber != null && idNumber > maxID) {
                            maxID = idNumber
                        }
                    }
                }

                return maxID
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return 0
        }
    }

    companion object {
        fun getInstance(project: Project): TaskSavingService = project.service()
    }

}