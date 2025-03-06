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

    private val counterFile: File
        get() = File(todoDir, "counter.json")

    fun saveTasks(tasks: List<Task>) {
        try {
            FileWriter(todoFile).use { writer -> gson.toJson(tasks, writer) }

            saveIdCounter(Task.getCurrentIdCounter())
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

    private fun saveIdCounter(counter: Int) {
        try {
            val counterData = mapOf("idCounter" to counter)
            FileWriter(counterFile).use { writer ->
                gson.toJson(counterData, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadIdCounter(): Int {
        if (!counterFile.exists()) {
            return 0
        }

        try {
            FileReader(counterFile).use { reader ->
                val counterType = object : TypeToken<Map<String, Int>>() {}.type
                val counterData: Map<String, Int> = gson.fromJson(reader, counterType)
                return counterData["idCounter"] ?: 0
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