package com.markbakos.todo.ui

import com.intellij.ui.table.JBTable
import com.markbakos.todo.models.Task
import com.intellij.openapi.project.Project
import java.awt.Color
import java.awt.Component
import javax.swing.*
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import java.util.Comparator

object TaskTableFactory {

    private val priorityComparator = Comparator<Any> { o1, o2 ->
        val p1 = o1.toString()
        val p2 = o2.toString()

        val priority1 = try { Task.Priority.valueOf(p1) } catch (e: Exception ) { Task.Priority.LOW }
        val priority2 = try { Task.Priority.valueOf(p2) } catch (e: Exception ) { Task.Priority.LOW }

        return@Comparator priority1.ordinal - priority2.ordinal
    }

    fun createTaskPanel(
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        status: Task.TaskStatus,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ): JPanel {
        val panel = JPanel(BorderLayout())

        val tableModel = object : DefaultTableModel(
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
        val table = JBTable(tableModel)

        val sorter = TableRowSorter<DefaultTableModel>(tableModel)
        table.rowSorter = sorter

        sorter.setComparator(1, priorityComparator)

        for (i in 0 until tableModel.columnCount) {
            if (i != 1) {
                sorter.setSortable(i, false)
            }
        }

        table.setDefaultRenderer(Task.Priority::class.java, PriorityColorRenderer() )
        table.setDefaultRenderer(String::class.java, PriorityColorRenderer())

        val filteredTasks = tasks.filter { it.status == status }
        filteredTasks.forEach { task ->
            tableModel.addRow(arrayOf(
                task.id,
                task.priority,
                task.tags.joinToString(", "),
                task.description,
            ))
        }

        table.columnModel.getColumn(1).preferredWidth = 60
        table.columnModel.getColumn(2).preferredWidth = 100
        table.columnModel.getColumn(3).preferredWidth = 300

        table.removeColumn(table.columnModel.getColumn(0))

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
            val selectedRow = table.selectedRow
            if (selectedRow != -1) {
                val modelRow = table.convertRowIndexToModel(selectedRow)
                val taskId = tableModel.getValueAt(modelRow, 0).toString()
                val taskIndex = tasks.indexOfFirst { it.id == taskId }
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
        }

        moveButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow != -1) {
                val modelRow = table.convertRowIndexToModel(selectedRow)
                val taskId = tableModel.getValueAt(modelRow, 0).toString()
                val taskIndex = tasks.indexOfFirst { it.id == taskId }
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
                        options[0]
                    )

                    if (result != null) {
                        task.status = result as Task.TaskStatus
                        saveTasks()
                        refreshTabs()
                    }
                }
            }
        }

        editButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow != -1) {
                val modelRow = table.convertRowIndexToModel(selectedRow)
                val taskId = tableModel.getValueAt(modelRow, 0).toString()
                val taskIndex = tasks.indexOfFirst { it.id == taskId }
                if (taskIndex != -1) {
                    TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], saveTasks, refreshTabs)
                }
            }
        }

        table.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedRow = table.selectedRow
                    if (selectedRow != -1) {
                        val modelRow = table.convertRowIndexToModel(selectedRow)
                        val taskId = tableModel.getValueAt(modelRow, 0).toString()
                        val taskIndex = tasks.indexOfFirst { it.id == taskId }
                        if (taskIndex != -1) {
                            TaskDialogManager.showEditTaskDialog(parent, project, tasks[taskIndex], saveTasks, refreshTabs)
                        }
                    }
                }
            }
        })

        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(moveButton)

        panel.add(JScrollPane(table), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
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
                    Task.Priority.LOW.toString() -> Color(0, 128, 0)
                    Task.Priority.MEDIUM.toString() -> Color(255, 165, 0)
                    Task.Priority.HIGH.toString() -> Color(255, 0, 0)
                    Task.Priority.CRITICAL.toString() -> Color(139, 0, 0)
                    else -> Color.BLACK
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

}
