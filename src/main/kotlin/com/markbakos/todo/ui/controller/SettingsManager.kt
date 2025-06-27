package com.markbakos.todo.ui.controller

import com.intellij.openapi.project.Project

// Manager class for handling settings
class SettingsManager private constructor(private val project: Project) {
    companion object {
        private val instances = mutableMapOf<Project, SettingsManager>()

        fun getInstance(project: Project): SettingsManager {
            return instances.getOrPut(project) { SettingsManager(project) }
        }
    }

    private var currentLanguage: String = "en" //default english

    fun getCurrentLanguage(): String = currentLanguage

    fun setCurrentLanguage(language: String) {
        currentLanguage = language
        // TODO: implement language change logic
    }

    fun getAvailableLanguages(): List<String> {
        return listOf("en", "fr", "es")
    }

    fun saveSettings() {
        // TODO: implement saving settings logic to PropertiesCOmponent
    }

    fun loadSettings() {
        // TODO: implement settings loading
    }
}