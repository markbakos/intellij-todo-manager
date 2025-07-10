package com.markbakos.todo.models

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.markbakos.todo.saving.TaskSavingService
import java.util.Collections

@Service(Service.Level.PROJECT)
class TagManager(private val project: Project) {
    private val savingService: TaskSavingService by lazy { TaskSavingService.getInstance(project) }

    private val cachedTags = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        refreshTags()
    }

    fun getAllTags(): List<String> {
        return cachedTags.toList().sorted()
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !cachedTags.contains(tag)) {
            cachedTags.add(tag)
        }
    }

    fun removeTag(tag: String) {
        cachedTags.remove(tag)
    }

    fun refreshTags() {
        val tagsFromStorage = getAllTagsFromTasks()

        // clear cached tags, keep existing ones
        val temporaryTags = cachedTags - tagsFromStorage

        cachedTags.clear()
        cachedTags.addAll(tagsFromStorage)
        cachedTags.addAll(temporaryTags)
    }

    private fun getAllTagsFromTasks(): Set<String> {
        return savingService.loadTasks()
            .flatMap { it.tags }
            .filter { it.isNotBlank() }
            .toSet()
    }

    companion object {
        fun getInstance(project: Project): TagManager = project.service()
    }
}