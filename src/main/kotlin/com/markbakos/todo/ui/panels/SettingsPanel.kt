package com.markbakos.todo.ui.panels

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import com.markbakos.todo.ui.controller.I18nManager
import com.markbakos.todo.ui.controller.SettingsManager
import com.markbakos.todo.services.ProjectTodoExtractor
import com.markbakos.todo.ui.dialog.TodoExtractionDialog
import com.markbakos.todo.models.Task
import com.markbakos.todo.ui.controller.TodoItem
import com.markbakos.todo.saving.TaskSavingService
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.Locale
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities


class SettingsPanel(private val project: Project, private val onRefreshTasks: () -> Unit): JPanel(BorderLayout()), I18nManager.LanguageChangeListener {
    private val settingsManager = SettingsManager.getInstance(project)
    private val i18nManager = settingsManager.getI18nManager()
    private lateinit var languageComboBox: ComboBox<String>
    private lateinit var titleLabel: JLabel
    private lateinit var languageLabel: JLabel
    private lateinit var refreshButton: JButton
    private lateinit var extractTodosButton: JButton

    private var updatingComboBox = false

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

        val refreshSection = createRefreshSection()
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insetsBottom(16)
        mainPanel.add(refreshSection, gbc)

        val todoExtractionSection = createTodoExtractionSection()
        gbc.gridy = 3
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insetsBottom(16)
        mainPanel.add(todoExtractionSection, gbc)

        // spacer to push content to the top
        gbc.gridy = 4
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
                if (updatingComboBox) {
                    return // prevent infinite loop
                }

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

    private fun createRefreshSection(): JPanel {
        val panel = JPanel(BorderLayout())

        refreshButton = JButton(i18nManager.getString("button.refreshTasks"))
        refreshButton.addActionListener { onRefreshTasks() }

        panel.add(refreshButton)
        return panel
    }

    private fun createTodoExtractionSection(): JPanel {
        val panel = JPanel(BorderLayout())

        extractTodosButton = JButton(i18nManager.getString("button.extractTodos"))
        extractTodosButton.addActionListener { extractTodoComments() }

        panel.add(extractTodosButton)
        return panel
    }

    private fun extractTodoComments() {
        val extractor = ProjectTodoExtractor(project)
        extractor.extractAllTodosAsync { todoItems ->
            SwingUtilities.invokeLater {
                if (todoItems.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        this,
                        i18nManager.getString("message.noTodosInProject"),
                        i18nManager.getString("error.noTodos"),
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    val dialog = TodoExtractionDialog(project, todoItems) { selectedTodos ->
                        addTodosToTasks(selectedTodos)
                    }
                    dialog.show()
                }
            }
        }
    }

    private fun addTodosToTasks(todoItems: List<TodoItem>) {
        val savingService = TaskSavingService.getInstance(project)
        val existingTasks = savingService.loadTasks().toMutableList()
        
        todoItems.forEach { todoItem ->
            val task = Task(
                description = "[${todoItem.keyword}] ${todoItem.text}",
                priority = Task.Priority.MEDIUM,
                status = Task.TaskStatus.TODO,
                tags = mutableListOf(todoItem.keyword.lowercase()),
                link = null,
                prerequisiteTaskId = null
            )
            existingTasks.add(task)
        }

        TagSelectionPanel(project).refreshTagList()
        
        savingService.saveTasks(existingTasks)
        onRefreshTasks()
    }


    // called when language changes
    override fun onLanguageChanged(newLocale: Locale) {
        titleLabel.text = i18nManager.getString("label.title")
        languageLabel.text = i18nManager.getString("label.language")
        refreshButton.text = i18nManager.getString("button.refreshTasks")
        extractTodosButton.text = i18nManager.getString("button.extractTodos")

        // use flag to prevent recursive updates
        updatingComboBox = true
        try {
            val availableLanguages = settingsManager.getAvailableLanguages()
            val displayNames = availableLanguages.map { settingsManager.getLanguageDisplayName(it) }.toTypedArray()

            // find the current language to maintain selection after rebuilding
            val currentLanguage = settingsManager.getCurrentLanguage()
            val newSelectionIndex = availableLanguages.indexOf(currentLanguage)

            // rebuild ComboBox itemsggVG
            languageComboBox.removeAllItems()
            displayNames.forEach { displayName ->
                languageComboBox.addItem(displayName)
            }

            // restore correct selection
            if (newSelectionIndex >= 0 && newSelectionIndex < displayNames.size) {
                languageComboBox.selectedIndex = newSelectionIndex
            }
        } finally {
            // always reset the flag
            updatingComboBox = false
        }
        revalidate()
        repaint()
    }
}