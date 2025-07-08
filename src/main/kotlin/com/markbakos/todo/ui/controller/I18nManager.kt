package com.markbakos.todo.ui.controller

import com.intellij.openapi.project.Project
import java.util.*

class I18nManager private constructor(private val project: Project) {
    companion object {
        private val instances = mutableMapOf<Project, I18nManager>()
        private const val BUNDLE_BASE_NAME = "messages.TodoBundle"

        fun getInstance(project: Project): I18nManager {
            return instances.getOrPut(project) {
                I18nManager(project)
            }
        }
    }

    private var currentLocale: Locale = Locale.ENGLISH
    private var resourceBundle: ResourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale)
    private val languageChangeListeners = mutableListOf<LanguageChangeListener>()

    // interface for components that need to be notified when language changes
    interface LanguageChangeListener {
        fun onLanguageChanged(newLocale: Locale)
    }

    // set current language and notify listeners
    fun setLanguage(languageCode: String) {
        val newLocale = when (languageCode) {
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "hu" -> Locale("hu") // Hungarian
            "pl" -> Locale("pl") // Polish
            "sr" -> Locale("sr") // Serbian
            "sk" -> Locale("sk") // Slovak
            "ru" -> Locale("ru") // Russian
            else -> Locale.ENGLISH // default to English if unknown
        }

        if (newLocale != currentLocale) {
            currentLocale = newLocale
            resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale)
            notifyLanguageChangeListeners()
        }
    }

    // get the current locale
    fun getCurrentLocale(): Locale = currentLocale

    // get current language code
    fun getCurrentLanguageCode(): String {
        return when (currentLocale.language) {
            "en" -> "en"
            "fr" -> "fr"
            "de" -> "de"
            "hu" -> "hu"
            "pl" -> "pl"
            "sr" -> "sr"
            "sk" -> "sk"
            "ru" -> "ru"
            else -> "en"
        }
    }

    // get translated string by key
    fun getString(key: String): String {
        return try {
            resourceBundle.getString(key)
        } catch (e: MissingResourceException) {
            //fallback to key not found
            "!$key!" // return key with ! for debugging
        }
    }

    // get translated string with parameters
    fun getString(key: String, vararg args: Any): String {
        val template = getString(key)
        return String.format(currentLocale, template, *args)
    }

    // get available languages with display names
    fun getAvailableLanguages(): Map<String, String> {
        return mapOf(
            "en" to "English",
            "fr" to "Français",
            "de" to "Deutsch",
            "hu" to "Magyar",
            "pl" to "Polski",
            "sr" to "Srpski",
            "sk" to "Slovenčina",
            "ru" to "Русский",
            )
    }

    // add listener for language changes
    fun addLanguageChangeListener(listener: LanguageChangeListener) {
        languageChangeListeners.add(listener)
    }

    // remove language change listener
    fun removeLanguageChangeListener(listener: LanguageChangeListener) {
        languageChangeListeners.remove(listener)
    }

    // notify all listeners about language change
    private fun notifyLanguageChangeListeners() {
        val listenersCopy = languageChangeListeners.toList()
        listenersCopy.forEach { listener ->
            try {
                listener.onLanguageChanged(currentLocale)
            } catch (e: Exception) {
                println("Error notifying language change listener: ${e.message}")
            }
        }
    }

    // check if translation key exists
    fun hasKey(key: String): Boolean {
        return try {
            resourceBundle.getString(key)
            true
        } catch (e: MissingResourceException) {
            false
        }
    }

    // for debug: get all keys
    fun getAllKeys(): Set<String> {
        return resourceBundle.keys.asSequence()
            .toSet()
            .map { it.toString() } // convert to String
            .toSet() // ensure it's a Set<String>
    }
}