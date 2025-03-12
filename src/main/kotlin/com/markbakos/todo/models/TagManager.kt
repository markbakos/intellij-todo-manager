package com.markbakos.todo.models

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
class TagManager(private val project: Project) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val tagListType: Type = object : TypeToken<List<String>>() {}.type
    private val tagsFile: File
        get() {
            val projectDir = project.guessProjectDir()
            val todoDir = File(projectDir?.path ?: "", ".todo")
            if (!todoDir.exists()) {
                todoDir.mkdir()
            }
            return File(todoDir, "tags.json")
        }

    private val tags = mutableSetOf<String>()

    init {
        loadTags()
    }

    fun getAllTags(): List<String> {
        return tags.toList().sorted()
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !tags.contains(tag)) {
            tags.add(tag)
            saveTags()
        }
    }

    fun removeTag(tag: String) {
        if (tag.contains(tag)) {
            tags.remove(tag)
            saveTags()
        }
    }

    fun addTags(newTags: List<String>) {
        var changed = false
        for (tag in newTags) {
            if (tag.isNotBlank() && !tags.contains(tag)) {
                tags.add(tag)
                changed = true
            }
        }
        if (changed) {
            saveTags()
        }
    }


    private fun saveTags() {
        try {
            FileWriter(tagsFile).use { writer -> gson.toJson(tags.toList(), writer) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadTags() {
        if (!tagsFile.exists()) {
            return
        }

        try {
            FileReader(tagsFile).use { reader ->
                val loadedTags: List<String> = gson.fromJson(reader, tagListType)
                tags.addAll(loadedTags)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        fun getInstance(project: Project): TagManager = project.service()
    }
}