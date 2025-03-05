package com.markbakos.todo

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import javax.swing.*
import java.awt.BorderLayout
import javax.swing.table.DefaultTableModel

class TodoManagerPanel(private val project: Project): JPanel(BorderLayout()) {

    private val tasks = mutableListOf<Task>()
    private val tabbedPane = JBTabbedPane()

    init {
        createTabs()
        setupAddTaskButton()
    }

    private fun createTabs() {
        val todoPanel = createTaskPanel(Task.TaskStatus.TODO)
        val inProgressPanel = createTaskPanel(Task.TaskStatus.IN_PROGRESS)
        val donePanel = createTaskPanel(Task.TaskStatus.DONE)

        tabbedPane.addTab("To-Do", todoPanel)
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

        panel.add(JScrollPane(table), BorderLayout.CENTER)
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
                val newTask = Task(
                    title = titleField.text,
                    description = descriptionArea.text,
                    tags = tagsField.text.split(",").map { it.trim() }.toMutableList(),
                    priority = priorityCombo.selectedItem as Task.Priority
                )
                tasks.add(newTask)
                refreshTabs()
                dialog.dispose()
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

    private fun refreshTabs() {
        tabbedPane.removeAll()
        createTabs()
        tabbedPane.revalidate()
        tabbedPane.repaint()
    }
}