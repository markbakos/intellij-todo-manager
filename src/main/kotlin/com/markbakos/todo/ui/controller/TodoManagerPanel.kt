package com.markbakos.todo.ui.controller

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.markbakos.todo.models.Task
import com.markbakos.todo.saving.TaskSavingService
import com.markbakos.todo.ui.dialog.TaskDialogManager
import com.markbakos.todo.ui.panels.SettingsPanel
import com.markbakos.todo.ui.table.TaskTableFactory
import java.awt.BorderLayout
import java.util.Locale
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SortOrder
import javax.swing.SwingUtilities

class TodoManagerPanel(private val project: Project): JPanel(BorderLayout()), I18nManager.LanguageChangeListener {

    private val savingService = TaskSavingService.Companion.getInstance(project)
    private val tasks = mutableListOf<Task>()
    private val tabbedPane = JBTabbedPane()
    private val i18nManager = I18nManager.getInstance(project)

    private lateinit var addButton: JButton

    private val tabSortStates = mutableMapOf<Int, SortState>()

    data class SortState(
        val sortedColumn: Int = -1,
        val sortOrder: SortOrder = SortOrder.UNSORTED
    )

    init {
        i18nManager.addLanguageChangeListener(this)

        initializeLanguage()

        loadTasks()
        createTabs()
        setupAddTaskButton()
    }

    private fun initializeLanguage() {
        val settingsManager = SettingsManager.getInstance(project)
        settingsManager.loadSettings()
    }

    private fun getString(key: String): String = i18nManager.getString(key)

    private fun loadTasks() {
        tasks.clear()
        tasks.addAll(savingService.loadTasks())
    }

    private fun saveTasks() {
        savingService.saveTasks(tasks)
    }

    private fun createTabs() {
        val todoPanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.TODO,
            ::saveTasks, ::refreshTabs, ::navigateToPrerequisiteTask ,tabSortStates[0])
        val inProgressPanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.IN_PROGRESS,
            ::saveTasks, ::refreshTabs, ::navigateToPrerequisiteTask, tabSortStates[1])
        val donePanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.DONE,
            ::saveTasks, ::refreshTabs, ::navigateToPrerequisiteTask, tabSortStates[2])

        tabbedPane.addTab(getString("status.todo"), todoPanel)
        tabbedPane.addTab(getString("status.inProgress"), inProgressPanel)
        tabbedPane.addTab(getString("status.done"), donePanel)

        // create and add settings tab
        val settingsPanel = SettingsPanel(project)
        tabbedPane.addTab("", AllIcons.General.Settings, settingsPanel, getString("label.settings"))

        add(tabbedPane, BorderLayout.CENTER)
    }

    private fun setupAddTaskButton() {
        addButton = JButton(getString("button.addTask"))
        addButton.addActionListener {
            SwingUtilities.invokeLater {
                TaskDialogManager.showAddTaskDialog(this, project, tasks, ::saveTasks, ::refreshTabs)
            }
        }
        add(addButton, BorderLayout.SOUTH)
    }

    private fun refreshTabs() {
        val currentTabIndex = tabbedPane.selectedIndex

        saveSortState() // save the current sort state before refreshing

        tabbedPane.removeAll()
        createTabs()
        if (currentTabIndex >= 0 && currentTabIndex < tabbedPane.tabCount) {
            tabbedPane.selectedIndex = currentTabIndex
        }
        tabbedPane.revalidate()
        tabbedPane.repaint()
    }

    private fun saveSortState() {
        for (i in 0 until tabbedPane.tabCount) {
            val panel = tabbedPane.getComponentAt(i) as? JPanel
            panel?.let {
                val sortState = TaskTableFactory.getSortState(it)
                if (sortState != null) {
                    tabSortStates[i] = sortState
                }
            }
        }
    }

    private fun navigateToPrerequisiteTask(prerequisiteTaskId: String) {
        val prerequisiteTask = tasks.find { it.id == prerequisiteTaskId }

        if (prerequisiteTask == null ) {
            JOptionPane.showMessageDialog(
                this,
                getString("message.prerequisiteNotFound"),
                getString("error.taskNotFound"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val targetTabIndex = when (prerequisiteTask.status) {
            Task.TaskStatus.TODO -> 0
            Task.TaskStatus.IN_PROGRESS -> 1
            Task.TaskStatus.DONE -> 2
        }

        tabbedPane.selectedIndex = targetTabIndex

        val targetPanel = tabbedPane.getComponentAt(targetTabIndex) as? JPanel

        if (targetPanel == null) {
            JOptionPane.showMessageDialog(
                this,
                getString("message.panelNotFound"),
                getString("error.navigationError"),
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        SwingUtilities.invokeLater {
            val success = TaskTableFactory.selectTaskById(targetPanel, prerequisiteTaskId)

            if (!success) {
                JOptionPane.showMessageDialog(
                    this,
                    getString("message.prerequisiteNotLocated"),
                    getString("error.navigationError"),
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
    }

    // called when language changes
    override fun onLanguageChanged(newLocale: Locale) {
        updateTexts()
        refreshTabs()
    }

    // update all translatable text elements
    private fun updateTexts() {
        addButton.text = getString("button.addTask")

        if (tabbedPane.tabCount >= 4) {
            tabbedPane.setTitleAt(0, getString("todo.status.todo"))
            tabbedPane.setTitleAt(1, getString("todo.status.in_progress"))
            tabbedPane.setTitleAt(2, getString("todo.status.done"))
            tabbedPane.setToolTipTextAt(3, getString("settings.title"))
        }

        revalidate()
        repaint()
    }
}