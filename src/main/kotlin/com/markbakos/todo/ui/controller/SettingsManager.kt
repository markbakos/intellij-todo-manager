package com.markbakos.todo.ui.controller

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

// Manager class for handling settings
class SettingsManager private constructor(private val project: Project) {
    companion object {
        private val instances = mutableMapOf<Project, SettingsManager>()
        private const val LANGUAGE_KEY = "todo.plugin.language"
        private const val DEFAULT_LANGUAGE = "en"

        fun getInstance(project: Project): SettingsManager {
            return instances.getOrPut(project) {
                SettingsManager(project).apply {
                    loadSettings()
                }
            }
        }
    }

    private var currentLanguage: String = DEFAULT_LANGUAGE //default english
    private val propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance(project)
    private val i18nManager: I18nManager = I18nManager.getInstance(project)

    fun getCurrentLanguage(): String = currentLanguage

    fun setCurrentLanguage(language: String) {
        if (currentLanguage != language) {
            currentLanguage = language
            i18nManager.setLanguage(language)
            saveSettings()
        }
    }

    fun getAvailableLanguages(): List<String> {
        return i18nManager.getAvailableLanguages().keys.toList()
    }

    fun getLanguageDisplayName(languageCode: String): String {
        return i18nManager.getAvailableLanguages()[languageCode] ?: languageCode
    }

    fun saveSettings() {
        propertiesComponent.setValue(LANGUAGE_KEY, currentLanguage)
    }

    fun loadSettings() {
        currentLanguage = propertiesComponent.getValue(LANGUAGE_KEY, DEFAULT_LANGUAGE)
        i18nManager.setLanguage(currentLanguage)
    }

    fun getI18nManager(): I18nManager = i18nManager
}