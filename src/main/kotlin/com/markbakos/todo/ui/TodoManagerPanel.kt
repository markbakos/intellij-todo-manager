package com.markbakos.todo.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.markbakos.todo.models.Task
import com.markbakos.todo.saving.TaskSavingService
import java.awt.BorderLayout
import javax.swing.*

class TodoManagerPanel(private val project: Project): JPanel(BorderLayout()) {

    private val savingService = TaskSavingService.getInstance(project)
    private val tasks = mutableListOf<Task>()
    private val tabbedPane = JBTabbedPane()

    private val tabSortStates = mutableMapOf<Int, SortState>()

    data class SortState(
        val sortedColumn: Int = -1,
        val sortOrder: SortOrder = SortOrder.UNSORTED
    )

    init {
        loadTasks()
        createTabs()
        setupAddTaskButton()
    }

    private fun loadTasks() {
        tasks.clear()
        tasks.addAll(savingService.loadTasks())
    }

    private fun saveTasks() {
        savingService.saveTasks(tasks)
    }

    private fun createTabs() {
        val todoPanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.TODO,
            ::saveTasks, ::refreshTabs, tabSortStates[0])
        val inProgressPanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.IN_PROGRESS,
            ::saveTasks, ::refreshTabs, tabSortStates[1])
        val donePanel = TaskTableFactory.createTaskPanel(this, project, tasks, Task.TaskStatus.DONE,
            ::saveTasks, ::refreshTabs, tabSortStates[2])

        tabbedPane.addTab("TO-DO", todoPanel)
        tabbedPane.addTab("In Progress", inProgressPanel)
        tabbedPane.addTab("Done", donePanel)

        add(tabbedPane, BorderLayout.CENTER)
    }

    private fun setupAddTaskButton() {
        val addButton = JButton("Add New Task")
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
}