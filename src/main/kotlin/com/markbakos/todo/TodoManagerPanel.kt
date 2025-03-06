package com.markbakos.todo

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.table.DefaultTableModel

class TodoManagerPanel(private val project: Project): JPanel(BorderLayout()) {

    private val savingService = TaskSavingService.getInstance(project)
    private val tasks = mutableListOf<Task>()
    private val tabbedPane = JBTabbedPane()

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
        val todoPanel = createTaskPanel(Task.TaskStatus.TODO)
        val inProgressPanel = createTaskPanel(Task.TaskStatus.IN_PROGRESS)
        val donePanel = createTaskPanel(Task.TaskStatus.DONE)

        tabbedPane.addTab("TO-DO", todoPanel)
        tabbedPane.addTab("In Progress", inProgressPanel)
        tabbedPane.addTab("Done", donePanel)

        add(tabbedPane, BorderLayout.CENTER)
    }

    private fun createTaskPanel(status: Task.TaskStatus): JPanel {
        val panel = JPanel(BorderLayout())
        val tableModel = DefaultTableModel(
            arrayOf("ID", "Title", "Description", "Tags", "Priority"), 0
        )
        val table = JBTable(tableModel)

        val filteredTasks = tasks.filter { it.Status == status }
        filteredTasks.forEach { task ->
            tableModel.addRow(arrayOf(
                task.id,
                task.title,
                task.description,
                task.tags.joinToString(", "),
                task.priority
            ))
        }

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
                    tasks.removeAt(taskIndex)
                    saveTasks()
                    refreshTabs()
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
                        this,
                        "Select new status:",
                        "Change Task Status",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                    )

                    if (result != null) {
                        task.Status = result as Task.TaskStatus
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
                if (taskIndex != -1 ) {
                    showEditTaskDialog(tasks[taskIndex])
                }
            }
        }

        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(moveButton)

        panel.add(JScrollPane(table), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun setupAddTaskButton() {
        val addButton = JButton("Add New Task")
        addButton.addActionListener {
            SwingUtilities.invokeLater { showAddTaskDialog() }
        }
        val buttonPanel = JPanel()
        buttonPanel.add(addButton)
        add(addButton, BorderLayout.SOUTH)
    }

    private fun showAddTaskDialog() {
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
                this,
                "Error creating dialog: ${e.message}\n${e.stackTraceToString()}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showEditTaskDialog(task: Task) {
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
            statusCombo.selectedItem = task.Status

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
                    task.Status = statusCombo.selectedItem as Task.TaskStatus

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
                this,
                "Error creating dialog: ${e.message}\n${e.stackTraceToString()}!",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun refreshTabs() {
        tabbedPane.removeAll()
        createTabs()
        tabbedPane.revalidate()
        tabbedPane.repaint()
    }
}