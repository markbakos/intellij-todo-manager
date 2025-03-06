package com.markbakos.todo.ui

import com.intellij.ui.table.JBTable
import com.markbakos.todo.models.Task
import javax.swing.*
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.table.DefaultTableModel

object TaskTableFactory {

    fun createTaskPanel(
        parent: JPanel,
        tasks: MutableList<Task>,
        status: Task.TaskStatus,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ): JPanel {
        val panel = JPanel(BorderLayout())

        val tableModel = object : DefaultTableModel(
            arrayOf("ID", "Title", "Description", "Tags", "Priority"), 0
        ) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        val table = JBTable(tableModel)

        val filteredTasks = tasks.filter { it.status == status }
        filteredTasks.forEach { task ->
            tableModel.addRow(arrayOf(
                task.id,
                task.title,
                task.description,
                task.tags.joinToString(", "),
                task.priority
            ))
        }

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
                val taskId = table.getValueAt(selectedRow, 0).toString()
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
                val taskId = table.getValueAt(selectedRow, 0).toString()
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
                val taskId = tableModel.getValueAt(selectedRow, 0).toString()
                val taskIndex = tasks.indexOfFirst { it.id == taskId }
                if (taskIndex != -1) {
                    TaskDialogManager.showEditTaskDialog(parent, tasks[taskIndex], saveTasks, refreshTabs)
                }
            }
        }

        table.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedRow = table.selectedRow
                    if (selectedRow != -1) {
                        val taskId = tableModel.getValueAt(selectedRow, 0).toString()
                        val taskIndex = tasks.indexOfFirst { it.id == taskId }
                        if (taskIndex != -1) {
                            TaskDialogManager.showEditTaskDialog(parent, tasks[taskIndex], saveTasks, refreshTabs)
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
}