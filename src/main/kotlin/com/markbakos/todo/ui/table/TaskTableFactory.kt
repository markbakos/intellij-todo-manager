package com.markbakos.todo.ui.table

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.table.JBTable
import com.markbakos.todo.models.TagManager
import com.markbakos.todo.models.Task
import com.markbakos.todo.ui.controller.I18nManager
import com.markbakos.todo.ui.controller.PriorityColorRenderer
import com.markbakos.todo.ui.controller.priorityComparator
import com.markbakos.todo.ui.dialog.TaskDialogManager
import com.markbakos.todo.ui.controller.TodoManagerPanel
import com.markbakos.todo.ui.navigation.TaskNavigationService
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.Locale
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

object TaskTableFactory {

    fun createTaskPanel(
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        status: Task.TaskStatus,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit,
        navigateToPrerequisite: ((String) -> Unit)? = null,
        sortState: TodoManagerPanel.SortState? = null
    ): JPanel {
        val panel = TaskTablePanel(project)
        val i18nManager = I18nManager.getInstance(project)
        val tableModel = createTableModel(i18nManager)
        val table = JBTable(tableModel)

        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter

        sorter.setComparator(1, priorityComparator)

        for (i in 0 until tableModel.columnCount) {
            if (i != 1) {
                sorter.setSortable(i, false)
            }
        }

        val filterPanel = createFilterPanel(project, table, tableModel, tasks, status)

        table.setDefaultRenderer(Task.Priority::class.java, PriorityColorRenderer())
        table.setDefaultRenderer(String::class.java, PriorityColorRenderer())

        populateTable(table, tableModel, tasks, status, null)

        table.columnModel.getColumn(1).preferredWidth = 60
        table.columnModel.getColumn(2).preferredWidth = 100
        table.columnModel.getColumn(3).preferredWidth = 300

        table.removeColumn(table.columnModel.getColumn(0))

        // restore sort state if available
        sortState?.let { state ->
            if (state.sortedColumn >= 0 && state.sortOrder != SortOrder.UNSORTED) {
                val viewColumn = if (state.sortedColumn > 0) state.sortedColumn - 1 else 0
                if (viewColumn < table.columnCount) {
                    val sortKeys = listOf(RowSorter.SortKey(viewColumn, state.sortOrder))
                    sorter.sortKeys = sortKeys
                }
            }
        }

        val buttonPanel = createButtonPanel(table, tableModel, parent, project, tasks, saveTasks, refreshTabs)

        setupMouseListener(table, tableModel, parent, project, tasks, saveTasks, refreshTabs)
        setupContextMenu(table, tableModel, parent, project, tasks, saveTasks, refreshTabs, navigateToPrerequisite)

        panel.add(filterPanel, BorderLayout.NORTH)
        panel.add(JScrollPane(table), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        // store references for language updates
        panel.putClientProperty("taskTable", table)
        panel.putClientProperty("filterPanel", filterPanel)
        panel.putClientProperty("buttonPanel", buttonPanel)

        return panel
    }

    private fun createFilterPanel(
        project: Project,
        table: JBTable,
        tableModel: DefaultTableModel,
        tasks: MutableList<Task>,
        status: Task.TaskStatus
    ): JPanel {
        val i18nManager = I18nManager.getInstance(project)
        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val tagManager = TagManager.getInstance(project)
        val allTags = tagManager.getAllTags()

        val filterTagsModel = DefaultComboBoxModel<String>().apply {
            addElement(i18nManager.getString("label.allTags"))
            allTags.forEach { addElement(it) }
        }

        val tagFilterComboBox = ComboBox(filterTagsModel)
        val tagFilterLabel = JLabel(i18nManager.getString("label.filterByTag"))

        tagFilterComboBox.addActionListener {
            val selectedTag = tagFilterComboBox.selectedItem as String
            val tagFilter = if (selectedTag == i18nManager.getString("label.allTags")) null else selectedTag
            populateTable(table, tableModel, tasks, status, tagFilter)
        }

        filterPanel.add(tagFilterLabel)
        filterPanel.add(tagFilterComboBox)

        // store references for language updates
        filterPanel.putClientProperty("tagFilterLabel", tagFilterLabel)
        filterPanel.putClientProperty("tagFilterComboBox", tagFilterComboBox)

        return filterPanel
    }

    fun getSortState(panel: JPanel): TodoManagerPanel.SortState? {
        val table = panel.getClientProperty("taskTable") as? JBTable
        return table?.let {
            val sorter = it.rowSorter as? TableRowSorter<*>
            sorter?.let { s ->
                val sortKeys = s.sortKeys
                if (sortKeys.isNotEmpty()) {
                    val key = sortKeys[0]
                    val modelColumn = if (key.column >= 0) key.column + 1 else key.column
                    TodoManagerPanel.SortState(modelColumn, key.sortOrder)
                } else {
                    TodoManagerPanel.SortState()
                }
            }
        }
    }

    fun selectTaskById(panel: JPanel, taskId: String): Boolean {
        val table = panel.getClientProperty("taskTable") as? JBTable ?: return false
        val tableModel = table.model as? DefaultTableModel ?: return false

        for (i in 0 until tableModel.rowCount) {
            val rowTaskId = tableModel.getValueAt(i, 0).toString()
            if (rowTaskId == taskId) {
                val viewRow = table.convertRowIndexToView(i)
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow)
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true))
                    return true
                }
            }
        }
        return false
    }

    private fun createTableModel(i18nManager: I18nManager): DefaultTableModel {
        return object : DefaultTableModel(
            arrayOf(
                "ID",
                i18nManager.getString("label.priority"),
                i18nManager.getString("label.tags"),
                i18nManager.getString("label.description")
            ), 0
        ) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }

            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when (columnIndex) {
                    1 -> Task.Priority::class.java
                    else -> String::class.java
                }
            }
        }
    }

    private fun populateTable(
        table: JBTable,
        tableModel: DefaultTableModel,
        tasks: List<Task>,
        status: Task.TaskStatus,
        tagFilter: String?
    ) {
        tableModel.rowCount = 0

        val filteredTasks = tasks.filter { task ->
            val matchesStatus = task.status == status
            val matchesTag = tagFilter == null || task.tags.contains(tagFilter)
            matchesStatus && matchesTag
        }

        filteredTasks.forEach { task ->
            tableModel.addRow(arrayOf(
                task.id,
                task.priority,
                task.tags.joinToString(", "),
                task.description,
            ))
        }
    }

    private fun setupContextMenu(
        table: JBTable,
        tableModel: DefaultTableModel,
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit,
        navigateToPrerequisite: ((String) -> Unit)? = null
    ) {
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    handlePopup(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    handlePopup(e)
                }
            }

            private fun handlePopup(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                if (row >= 0 && row < table.rowCount) {
                    table.setRowSelectionInterval(row, row)

                    val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
                    if (taskIndex != -1) {
                        val task = tasks[taskIndex]
                        val popupMenu = createDynamicPopupMenu(task, parent, project, tasks, taskIndex, saveTasks, refreshTabs, navigateToPrerequisite)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                } else {
                    table.clearSelection()
                }
            }
        })
    }

    private fun createDynamicPopupMenu(
        task: Task,
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        taskIndex: Int,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit,
        navigateToPrerequisite: ((String) -> Unit)? = null
    ): JPopupMenu {
        val i18nManager = I18nManager.getInstance(project)
        val popupMenu = JPopupMenu()
        var hasSeparatorBefore = false
        val navigationService = TaskNavigationService()

        // only add "View Prerequisite Task" if task has a prerequisite
        if (task.prerequisiteTaskId != null) {
            val viewPrerequisiteMenuItem = JMenuItem(i18nManager.getString("menu.viewPrerequisite"))
            viewPrerequisiteMenuItem.addActionListener {
                navigateToPrerequisite?.invoke(task.prerequisiteTaskId!!) ?: run {
                    JOptionPane.showMessageDialog(
                        parent,
                        i18nManager.getString("message.cannotNavigatePrerequisite"),
                        i18nManager.getString("error.navigationError"),
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            }
            popupMenu.add(viewPrerequisiteMenuItem)
            hasSeparatorBefore = true
        }

        // only add "Open Link" if task has a link
        if (task.link != null) {
            val link = task.link
            val openLinkMenuItem = JMenuItem(i18nManager.getString("menu.openLink"))
            openLinkMenuItem.addActionListener {
                try {
                    Desktop.getDesktop().browse(URI(task.link))
                } catch (e: Exception) {
                    try {
                        if (link?.startsWith("https://") != true && link?.startsWith("http://") != true) {
                            Desktop.getDesktop().browse(URI("https://" + link))
                        }
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            parent,
                            i18nManager.getString("message.failedToOpenLink") + ": ${e.message}",
                            i18nManager.getString("error.error"),
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            popupMenu.add(openLinkMenuItem)
            hasSeparatorBefore = true
        }

        // only add "Open Comment Location" if task is imported and has file info
        if (task.isImported && !task.fileName.isNullOrEmpty()) {
            val openCommentLocation = JMenuItem(i18nManager.getString("menu.openCommentLocation"))
            openCommentLocation.addActionListener {
                val success = navigationService.navigateToTodoComment(project, task)
                if (!success) {
                    JOptionPane.showMessageDialog(
                        parent,
                        i18nManager.getString("message.couldNotLocateTodo"),
                        i18nManager.getString("error.navigationError"),
                        JOptionPane.WARNING_MESSAGE
                    )
                }
            }
            popupMenu.add(openCommentLocation)
            hasSeparatorBefore = true
        }

        // add separator if we added any items from the first section
        if (hasSeparatorBefore) {
            popupMenu.addSeparator()
        }

        // always add the standard task management items
        val editMenuItem = JMenuItem(i18nManager.getString("menu.editTask"))
        editMenuItem.addActionListener {
            TaskDialogManager.showEditTaskDialog(parent, project, task, tasks, saveTasks, refreshTabs)
        }
        popupMenu.add(editMenuItem)

        val changeStatusMenuItem = JMenuItem(i18nManager.getString("menu.changeStatus"))
        changeStatusMenuItem.addActionListener {
            val options = Task.TaskStatus.values()
            val result = JOptionPane.showInputDialog(
                parent,
                i18nManager.getString("dialog.selectNewStatus"),
                i18nManager.getString("dialog.changeTaskStatus"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[task.status.ordinal]
            )

            if (result != null) {
                task.status = result as Task.TaskStatus
                saveTasks()
                refreshTabs()
            }
        }
        popupMenu.add(changeStatusMenuItem)

        popupMenu.addSeparator()

        val deleteMenuItem = JMenuItem(i18nManager.getString("menu.deleteTask"))
        deleteMenuItem.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                parent,
                i18nManager.getString("dialog.confirmDeleteTask"),
                i18nManager.getString("dialog.confirmDelete"),
                JOptionPane.YES_NO_OPTION
            )

            if (confirm == JOptionPane.YES_OPTION) {
                tasks.removeAt(taskIndex)
                saveTasks()
                refreshTabs()
            }
        }
        popupMenu.add(deleteMenuItem)

        return popupMenu
    }

    private fun getSelectedTaskIndex(
        table: JBTable,
        tableModel: DefaultTableModel,
        tasks: MutableList<Task>
    ): Int {
        val selectedRow = table.selectedRow
        if (selectedRow != -1) {
            val modelRow = table.convertRowIndexToModel(selectedRow)
            val taskId = tableModel.getValueAt(modelRow, 0).toString()
            return tasks.indexOfFirst { it.id == taskId }
        }
        return -1
    }

    private fun createButtonPanel(
        table: JBTable,
        tableModel: DefaultTableModel,
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ): JPanel {
        val i18nManager = I18nManager.getInstance(project)
        val buttonPanel = JPanel()
        val editButton = JButton(i18nManager.getString("button.edit"))
        val deleteButton = JButton(i18nManager.getString("button.delete"))
        val moveButton = JButton(i18nManager.getString("button.changeStatus"))

        editButton.isEnabled = false
        deleteButton.isEnabled = false
        moveButton.isEnabled = false

        table.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting && table.selectedRow != -1) {
                editButton.isEnabled = true
                deleteButton.isEnabled = true
                moveButton.isEnabled = true
            } else {
                editButton.isEnabled = false
                deleteButton.isEnabled = false
                moveButton.isEnabled = false
            }
        }

        deleteButton.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val confirm = JOptionPane.showConfirmDialog(
                    parent,
                    i18nManager.getString("dialog.confirmDeleteTask"),
                    i18nManager.getString("dialog.confirmDelete"),
                    JOptionPane.YES_NO_OPTION
                )

                if (confirm == JOptionPane.YES_OPTION) {
                    tasks.removeAt(taskIndex)
                    saveTasks()
                    refreshTabs()
                }
            }
        }

        moveButton.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val task = tasks[taskIndex]
                val options = Task.TaskStatus.values()
                val result = JOptionPane.showInputDialog(
                    parent,
                    i18nManager.getString("dialog.selectNewStatus"),
                    i18nManager.getString("dialog.changeTaskStatus"),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[task.status.ordinal]
                )

                if (result != null) {
                    task.status = result as Task.TaskStatus
                    saveTasks()
                    refreshTabs()
                }
            }
        }

        editButton.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], tasks, saveTasks, refreshTabs)
            }
        }

        // store references for language updates
        buttonPanel.putClientProperty("editButton", editButton)
        buttonPanel.putClientProperty("deleteButton", deleteButton)
        buttonPanel.putClientProperty("moveButton", moveButton)

        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(moveButton)

        return buttonPanel
    }

    private fun setupMouseListener(
        table: JBTable,
        tableModel: DefaultTableModel,
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        table.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
                    if (taskIndex != -1) {
                        TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], tasks, saveTasks, refreshTabs)
                    }
                }
            }
        })
    }

    // custom JPanel that implements language change listener
    private class TaskTablePanel(private val project: Project) : JPanel(BorderLayout()), I18nManager.LanguageChangeListener {
        private val i18nManager = I18nManager.getInstance(project)

        init {
            i18nManager.addLanguageChangeListener(this)
        }

        override fun onLanguageChanged(newLocale: Locale) {
            updateTexts()
        }

        private fun updateTexts() {
            // Update filter panel
            val filterPanel = getClientProperty("filterPanel") as? JPanel
            filterPanel?.let { panel ->
                val tagFilterLabel = panel.getClientProperty("tagFilterLabel") as? JLabel
                val tagFilterComboBox = panel.getClientProperty("tagFilterComboBox") as? ComboBox<String>

                tagFilterLabel?.text = i18nManager.getString("filter.byTag")

                tagFilterComboBox?.let { comboBox ->
                    val selectedIndex = comboBox.selectedIndex
                    val model = comboBox.model as DefaultComboBoxModel<String>
                    model.removeAllElements()
                    model.addElement(i18nManager.getString("filter.allTags"))

                    val tagManager = TagManager.getInstance(project)
                    tagManager.getAllTags().forEach { model.addElement(it) }

                    if (selectedIndex >= 0 && selectedIndex < model.size) {
                        comboBox.selectedIndex = selectedIndex
                    }
                }
            }

            // Update button panel
            val buttonPanel = getClientProperty("buttonPanel") as? JPanel
            buttonPanel?.let { panel ->
                val editButton = panel.getClientProperty("editButton") as? JButton
                val deleteButton = panel.getClientProperty("deleteButton") as? JButton
                val moveButton = panel.getClientProperty("moveButton") as? JButton

                editButton?.text = i18nManager.getString("button.edit")
                deleteButton?.text = i18nManager.getString("button.delete")
                moveButton?.text = i18nManager.getString("button.changeStatus")
            }

            // Update table headers
            val table = getClientProperty("taskTable") as? JBTable
            table?.let {
                val tableModel = it.model as? DefaultTableModel
                tableModel?.let { model ->
                    model.setColumnIdentifiers(arrayOf(
                        "ID",
                        i18nManager.getString("label.priority"),
                        i18nManager.getString("label.tags"),
                        i18nManager.getString("label.description")
                    ))
                }
            }

            revalidate()
            repaint()
        }
    }
}