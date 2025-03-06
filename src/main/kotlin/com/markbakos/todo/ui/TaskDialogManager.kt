package com.markbakos.todo.ui

import com.intellij.openapi.ui.ComboBox
import com.markbakos.todo.models.Task
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

object TaskDialogManager {

    fun showAddTaskDialog(
        parent: JPanel,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        try {
            val dialog = JDialog()
            dialog.title = "Add New Task"
            dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            dialog.layout = BorderLayout()
            dialog.isResizable = false

            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

            val titleField = JTextField(20)
            val descriptionArea = JTextArea(4, 20)
            val tagsField = JTextField(20)
            val priorityCombo = ComboBox(Task.Priority.values())

            panel.add(JLabel("Title:"))
            panel.add(titleField)
            panel.add(JLabel("Description:"))
            panel.add(JScrollPane(descriptionArea))
            panel.add(JLabel("Tags (separate with commas):"))
            panel.add(tagsField)
            panel.add(JLabel("Priority:"))
            panel.add(priorityCombo)

            val buttonPanel = JPanel()
            val saveButton = JButton("Save Task")
            saveButton.addActionListener {
                if (titleField.text.isNotEmpty()) {
                    val newTask = Task(
                        title = titleField.text,
                        description = descriptionArea.text,
                        tags = tagsField.text.split(",")
                            .filter { it.isNotBlank() }
                            .map { it.trim() }
                            .toMutableList(),
                        priority = priorityCombo.selectedItem as Task.Priority
                    )
                    tasks.add(newTask)
                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                }
                else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Title cannot be empty!",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            val cancelButton = JButton("Cancel")
            cancelButton.addActionListener { dialog.dispose() }

            buttonPanel.add(saveButton)
            buttonPanel.add(cancelButton)

            dialog.add(panel, BorderLayout.CENTER)
            dialog.add(buttonPanel, BorderLayout.SOUTH)

            dialog.pack()
            dialog.isVisible = true
        }
        catch (e: Exception) {
            JOptionPane.showMessageDialog(
                parent,
                "Error creating dialog: ${e.message}\n${e.stackTraceToString()}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun showEditTaskDialog(
        parent: JPanel,
        task: Task,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        try {
            val dialog = JDialog()
            dialog.title = "Edit Task"
            dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            dialog.layout = BorderLayout()
            dialog.isResizable = false

            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            val titleField = JTextField(task.title, 20)

            val descriptionArea = JTextArea(task.description, 8, 20)
            descriptionArea.lineWrap = true

            val tagsField = JTextField(task.tags.joinToString(", "), 20)
            val priorityCombo = ComboBox(Task.Priority.values())
            priorityCombo.selectedItem = task.priority
            val statusCombo = ComboBox(Task.TaskStatus.values())
            statusCombo.selectedItem = task.status

            val formPanel = JPanel(java.awt.GridLayout(0, 1, 5, 5))
            formPanel.add(JLabel("Title:"))
            formPanel.add(titleField)
            formPanel.add(JLabel("Description:"))
            formPanel.add(JScrollPane(descriptionArea))
            formPanel.add(JLabel("Tags (comma-separated):"))
            formPanel.add(tagsField)
            formPanel.add(JLabel("Priority:"))
            formPanel.add(priorityCombo)
            formPanel.add(JLabel("Status:"))
            formPanel.add(statusCombo)

            panel.add(formPanel)

            val buttonPanel = JPanel()
            val saveButton = JButton("Update Task")
            saveButton.addActionListener {
                if (titleField.text.isNotEmpty()) {
                    task.title = titleField.text
                    task.description = descriptionArea.text
                    task.tags = tagsField.text.split(",")
                        .filter { it.isNotBlank() }
                        .map { it.trim() }
                        .toMutableList()
                    task.priority = priorityCombo.selectedItem as Task.Priority
                    task.status = statusCombo.selectedItem as Task.TaskStatus

                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                } else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Title cannot be empty!",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            val cancelButton = JButton("Cancel")
            cancelButton.addActionListener { dialog.dispose() }

            buttonPanel.add(saveButton)
            buttonPanel.add(cancelButton)

            dialog.add(panel, BorderLayout.CENTER)
            dialog.add(buttonPanel, BorderLayout.SOUTH)

            dialog.preferredSize = Dimension(400, 500)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                parent,
                "Error creating dialog: ${e.message}\n${e.stackTraceToString()}!",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}