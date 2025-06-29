package com.markbakos.todo.ui.panels

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import com.markbakos.todo.ui.controller.I18nManager
import com.markbakos.todo.ui.controller.SettingsManager
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.Locale
import javax.swing.JLabel
import javax.swing.JPanel

class SettingsPanel(private val project: Project): JPanel(BorderLayout()), I18nManager.LanguageChangeListener {
    private val settingsManager = SettingsManager.getInstance(project)
    private val i18nManager = settingsManager.getI18nManager()
    private lateinit var languageComboBox: ComboBox<String>
    private lateinit var titleLabel: JLabel
    private lateinit var languageLabel: JLabel

    init {
        i18nManager.addLanguageChangeListener(this)
        setupUI()
    }

    // Setup UI components for Settings Panel
    private fun setupUI() {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // title
        titleLabel = JLabel(i18nManager.getString("label.title"))
        titleLabel.font = titleLabel.font.deriveFont(18f)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insetsBottom(20)
        mainPanel.add(titleLabel, gbc)

        // language section
        val languageSection = createLanguageSection()
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insetsBottom(16)
        mainPanel.add(languageSection, gbc)

        // spacer to push content to the top
        gbc.gridy = 2
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(JPanel(), gbc)

        add(mainPanel, BorderLayout.CENTER)
    }

    private fun createLanguageSection(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // language label
        languageLabel = JLabel(i18nManager.getString("label.language"))
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insetsRight(10)
        panel.add(languageLabel, gbc)

        // language combobox
        val availableLanguages = settingsManager.getAvailableLanguages()
        val displayNames = availableLanguages.map { settingsManager.getLanguageDisplayName(it) }.toTypedArray()

        languageComboBox = ComboBox(displayNames)

        // set current selection based on saved settings
        val currentLanguage = settingsManager.getCurrentLanguage()
        val currentIndex = availableLanguages.indexOf(currentLanguage)
        if (currentIndex >= 0) {
            languageComboBox.selectedIndex = currentIndex
        }

        languageComboBox.addActionListener(object: ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val selectedIndex = languageComboBox.selectedIndex
                if (selectedIndex >= 0) {
                    val selectedLanguageCode = availableLanguages[selectedIndex]
                    settingsManager.setCurrentLanguage(selectedLanguageCode)
                }
            }
        })

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.emptyInsets()
        panel.add(languageComboBox, gbc)

        return panel
    }

    // called when language changes
    override fun onLanguageChanged(newLocale: Locale) {
        titleLabel.text = i18nManager.getString("label.title")
        languageLabel.text = i18nManager.getString("label.language")

        val availableLanguages = settingsManager.getAvailableLanguages()
        val displayNames = availableLanguages.map { settingsManager.getLanguageDisplayName(it) }.toTypedArray()

        val currentSelection = languageComboBox.selectedIndex
        languageComboBox.removeAllItems()
        displayNames.forEach { languageComboBox.addItem(it) }

        // for invalid selections
        if (currentSelection >= 0 && currentSelection < displayNames.size) {
            languageComboBox.selectedIndex = currentSelection
        }

        revalidate()
        repaint()
    }
}