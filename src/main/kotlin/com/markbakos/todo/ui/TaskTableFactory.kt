package com.markbakos.todo.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.table.JBTable
import com.markbakos.todo.models.Task
import com.markbakos.todo.models.TagManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.*
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

object TaskTableFactory {

    private val priorityComparator = Comparator<Any> { o1, o2 ->
        val p1 = o1.toString()
        val p2 = o2.toString()

        val priority1 = try { Task.Priority.valueOf(p1) } catch (_: Exception ) { Task.Priority.LOW }
        val priority2 = try { Task.Priority.valueOf(p2) } catch (_: Exception ) { Task.Priority.LOW }

        return@Comparator priority1.ordinal - priority2.ordinal
    }

    fun createTaskPanel(
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        status: Task.TaskStatus,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit,
        sortState: TodoManagerPanel.SortState? = null
    ): JPanel {
        val panel = JPanel(BorderLayout())
        val tableModel = createTableModel()
        val table = JBTable(tableModel)

        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter

        sorter.setComparator(1, priorityComparator)

        for (i in 0 until tableModel.columnCount) {
            if (i != 1) {
                sorter.setSortable(i, false)
            }
        }

        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val tagManager = TagManager.getInstance(project)
        val allTags = tagManager.getAllTags()

        val filterTagsModel = DefaultComboBoxModel<String>().apply {
            addElement("All Tags")
            allTags.forEach { addElement(it) }
        }

        val tagFilterComboBox = ComboBox(filterTagsModel)
        val tagFilterLabel = JLabel("Filter by tag: ")
        filterPanel.add(tagFilterLabel)
        filterPanel.add(tagFilterComboBox)

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

        tagFilterComboBox.addActionListener {
            val selectedTag = tagFilterComboBox.selectedItem as String
            val tagFilter = if (selectedTag == "All Tags") null else selectedTag
            populateTable(table, tableModel, tasks, status, tagFilter)
        }

        val buttonPanel = createButtonPanel(table, tableModel, parent, project, tasks, saveTasks, refreshTabs)

        setupMouseListener(table, tableModel, parent, project, tasks, saveTasks, refreshTabs)
        setupContextMenu(table, tableModel, parent, project, tasks, saveTasks, refreshTabs)

        panel.add(filterPanel, BorderLayout.NORTH)
        panel.add(JScrollPane(table), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        // store reference to the table, used for sorting state retrieval
        panel.putClientProperty("taskTable", table)

        return panel
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

    private fun createTableModel(): DefaultTableModel {
        return object : DefaultTableModel(
            arrayOf("ID", "Priority", "Tags", "Description"), 0
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
        refreshTabs: () -> Unit
    ) {
        val popupMenu = JPopupMenu()

        val openLinkMenuItem = JMenuItem("Open Link")
        val openCommentLocation = JMenuItem("Open Comment Location")
        val editMenuItem = JMenuItem("Edit Task")
        val changeStatusMenuItem = JMenuItem("Change Status")
        val deleteMenuItem = JMenuItem("Delete Task")

        openLinkMenuItem.isEnabled = false
        openCommentLocation.isEnabled = false
        editMenuItem.isEnabled = false
        changeStatusMenuItem.isEnabled = false
        deleteMenuItem.isEnabled = false

        popupMenu.add(openLinkMenuItem)
        popupMenu.add(openCommentLocation)
        popupMenu.addSeparator()
        popupMenu.add(editMenuItem)
        popupMenu.add(changeStatusMenuItem)
        popupMenu.addSeparator()
        popupMenu.add(deleteMenuItem)

        openLinkMenuItem.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val task = tasks[taskIndex]
                task.link?.let { link ->
                    try {
                        java.awt.Desktop.getDesktop().browse(java.net.URI(link))
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            parent,
                            "Failed to open link: ${e.message}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        }

        openCommentLocation.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val task = tasks[taskIndex]

                // check if task is imported and has the file info
                if (task.isImported && !task.fileName.isNullOrEmpty()) {
                    try {
                        // find file in project using helper function
                        val virtualFile = findFileInProject(project, task.fileName!!)

                        if (virtualFile != null) {
                            // open the file in editor
                            val fileEditorManager = FileEditorManager.getInstance(project)
                            fileEditorManager.openFile(virtualFile, true)

                            // navigate to specific line if available
                            task.lineNumber?.let { lineNumber ->
                                ApplicationManager.getApplication().invokeLater {
                                    val editor = fileEditorManager.selectedTextEditor
                                    if (editor != null && lineNumber > 0) {
                                        val line = (lineNumber - 1).coerceAtLeast(0)
                                        val document = editor.document

                                        //ensure line number is withing document bounds
                                        if (line < document.lineCount) {
                                            // move caret to line
                                            val offset = document.getLineStartOffset(line)
                                            editor.caretModel.moveToOffset(offset)

                                            // scroll to make line visible
                                            val scrollingModel = editor.scrollingModel
                                            scrollingModel.scrollToCaret(ScrollType.CENTER)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            parent,
                            "Failed to open file: ${e.message}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
                else {
                    JOptionPane.showMessageDialog(
                        parent,
                        "THis task was not imported from a comment or has no file information",
                        "No File Information",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }

        editMenuItem.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], saveTasks, refreshTabs)
            }
        }

        changeStatusMenuItem.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val task = tasks[taskIndex]
                val options = Task.TaskStatus.values()
                val result = JOptionPane.showInputDialog(
                    parent,
                    "Select new status:",
                    "Change Task Status",
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

        deleteMenuItem.addActionListener {
            val taskIndex = getSelectedTaskIndex(table, tableModel, tasks)
            if (taskIndex != -1) {
                val confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "Are you sure you want to delete this task?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
                )

                if (confirm == JOptionPane.YES_OPTION) {
                    tasks.removeAt(taskIndex)
                    saveTasks()
                    refreshTabs()
                }
            }
        }

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
                    val hasLink = taskIndex != -1 && tasks[taskIndex].link != null
                    val isImported = taskIndex != -1 && tasks[taskIndex].isImported && !tasks[taskIndex].fileName.isNullOrEmpty()

                    openLinkMenuItem.isEnabled = hasLink
                    openCommentLocation.isEnabled = isImported
                    editMenuItem.isEnabled = true
                    changeStatusMenuItem.isEnabled = true
                    deleteMenuItem.isEnabled = true

                    popupMenu.show(e.component, e.x, e.y)
                } else {
                    table.clearSelection()

                    openLinkMenuItem.isEnabled = false
                    openCommentLocation.isEnabled = false
                    editMenuItem.isEnabled = false
                    changeStatusMenuItem.isEnabled = false
                    deleteMenuItem.isEnabled = false
                }
            }
        })
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
        val buttonPanel = JPanel()
        val editButton = JButton("Edit")
        val deleteButton = JButton("Delete")
        val moveButton = JButton("Change Status")

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
                    "Are you sure you want to delete this task?",
                    "Confirm Delete",
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
                    "Select new status:",
                    "Change Task Status",
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
                TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], saveTasks, refreshTabs)
            }
        }

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
                        TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], saveTasks, refreshTabs)
                    }
                }
            }
        })
    }

    private class PriorityColorRenderer: DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val isPriorityColumn = table?.convertColumnIndexToModel(column) == 1

            if (isPriorityColumn && value != null) {
                val priorityValue = value.toString()
                val textColor = when (priorityValue) {
                    Task.Priority.LOW.toString() -> JBColor(Color(0, 128, 0), Color(0, 200, 0))
                    Task.Priority.MEDIUM.toString() -> JBColor(Color(255, 165, 0), Color(255, 175, 0))
                    Task.Priority.HIGH.toString() -> JBColor(Color(255, 0, 0), Color(255, 30, 30))
                    Task.Priority.CRITICAL.toString() -> JBColor(Color(139, 0, 0), Color(200, 0, 0))
                    else -> JBColor.BLACK
                }

                component.foreground = textColor
                font = font.deriveFont(java.awt.Font.BOLD)
            } else {
                component.foreground = if (isSelected) table?.selectionForeground else table?.foreground
                font = font.deriveFont(java.awt.Font.PLAIN)
            }

            return component
        }
    }

    private fun findFileInProject(project: Project, fileName: String): VirtualFile? {
        // Helper function to find file in project
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)

        val files = FilenameIndex.getFilesByName(project, fileName, projectScope)

        return when {
            files.isEmpty() -> null
            files.size == 1 -> files.first().virtualFile
            else -> {
                // TODO: currently only returns first file found, in future add show dialog to let user choose
                files.first().virtualFile
            }
        }
    }
}