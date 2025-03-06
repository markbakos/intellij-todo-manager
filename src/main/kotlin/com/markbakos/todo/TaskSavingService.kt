package com.markbakos.todo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.reflect.Type

@Service(Service.Level.PROJECT)
class TaskSavingService(private val project: Project) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val taskListType: Type = object : TypeToken<List<Task>>() {}.type
    private val todoFile: File
        get() {
            val projectDir = project.guessProjectDir()
            val todoDir = File(projectDir?.path ?: "", ".todo")
            if (!todoDir.exists()) {
                todoDir.mkdir()
            }
            return File(todoDir, "tasks.json")
        }

    fun saveTasks(tasks: List<Task>) {
        try {
            FileWriter(todoFile).use { writer -> gson.toJson(tasks, writer) }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadTasks(): List<Task> {
        if (!todoFile.exists()) {
            return emptyList()
        }

        try {
            FileReader(todoFile).use {reader -> return gson.fromJson(reader, taskListType)}
        }
        catch (e:IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }

    companion object {
        fun getInstance(project: Project): TaskSavingService = project.service()
    }

}