package com.markbakos.todo.ui

import com.intellij.openapi.ui.ComboBox
import com.markbakos.todo.models.Task
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

object TaskDialogManager {

    private const val DIALOG_WIDTH = 420
    private const val DIALOG_HEIGHT_ADD = 450
    private const val DIALOG_HEIGHT_EDIT = 500
    private const val DESCRIPTION_ROWS = 8
    private const val TEXT_FIELD_COLUMNS = 25
    private const val VERTICAL_GAP = 5
    private const val HORIZONTAL_GAP = 10
    private const val DESCRIPTION_HEIGHT = 200

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
            dialog.layout = BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP)
            dialog.isResizable = false

            val panel = JPanel(GridBagLayout())
            panel.border = BorderFactory.createEmptyBorder(15, 15, 5, 15)

            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(5, 2, 5, 2)
                weightx = 1.0
                gridwidth = GridBagConstraints.REMAINDER
            }

            addFormField(panel, "Title:", gbc) { constraints ->
                val titleField = JTextField(TEXT_FIELD_COLUMNS)
                panel.add(titleField, constraints)
                titleField
            }

            val descriptionArea = addFormField(panel, "Description:", gbc) { constraints ->
                constraints.fill = GridBagConstraints.BOTH
                constraints.weighty = 1.0

                val textArea = JTextArea(DESCRIPTION_ROWS, TEXT_FIELD_COLUMNS)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true

                val scrollPane = JScrollPane(textArea)
                scrollPane.preferredSize = Dimension(0, DESCRIPTION_HEIGHT)
                scrollPane.minimumSize = Dimension(0, DESCRIPTION_HEIGHT)
                panel.add(scrollPane, constraints)

                constraints.weighty = 0.0
                constraints.fill = GridBagConstraints.HORIZONTAL

                textArea
            }

            val tagsField = addFormField(panel, "Tags (separate with commas):", gbc) { constraints ->
                val field = JTextField(TEXT_FIELD_COLUMNS)
                panel.add(field, constraints)
                field
            }

            val priorityCombo = addFormField(panel, "Priority:", gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                panel.add(combo, constraints)
                combo
            }

            val buttonPanel = JPanel()
            buttonPanel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)

            val saveButton = JButton("Save Task")
            saveButton.addActionListener {
                val titleField = panel.components.filterIsInstance<JTextField>().first()

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

            dialog.minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_ADD)
            dialog.preferredSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_ADD)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
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
            dialog.layout = BorderLayout(HORIZONTAL_GAP, VERTICAL_GAP)
            dialog.isResizable = false

            val panel = JPanel(GridBagLayout())
            panel.border = BorderFactory.createEmptyBorder(15, 15, 5, 15)

            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(5, 2, 5, 2)
                weightx = 1.0
                gridwidth = GridBagConstraints.REMAINDER
            }

            val titleField = addFormField(panel, "Title:", gbc) { constraints ->
                val field = JTextField(task.title, TEXT_FIELD_COLUMNS)
                panel.add(field, constraints)
                field
            }

            val descriptionArea = addFormField(panel, "Description:", gbc) { constraints ->
                constraints.fill = GridBagConstraints.BOTH
                constraints.weighty = 1.0

                val textArea = JTextArea(task.description, DESCRIPTION_ROWS, TEXT_FIELD_COLUMNS)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true

                val scrollPane = JScrollPane(textArea)
                scrollPane.preferredSize = Dimension(0, DESCRIPTION_HEIGHT)
                scrollPane.minimumSize = Dimension(0, DESCRIPTION_HEIGHT)
                panel.add(scrollPane, constraints)

                constraints.weighty = 0.0
                constraints.fill = GridBagConstraints.HORIZONTAL

                textArea
            }

            val tagsField = addFormField(panel, "Tags (comma-separated):", gbc) { constraints ->
                val field = JTextField(task.tags.joinToString(", "), TEXT_FIELD_COLUMNS)
                panel.add(field, constraints)
                field
            }

            val priorityCombo = addFormField(panel, "Priority:", gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                combo.selectedItem = task.priority
                panel.add(combo, constraints)
                combo
            }

            val statusCombo = addFormField(panel, "Status:", gbc) { constraints ->
                val combo = ComboBox(Task.TaskStatus.values())
                combo.selectedItem = task.status
                panel.add(combo, constraints)
                combo
            }

            val buttonPanel = JPanel()
            buttonPanel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)

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

            dialog.minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_EDIT)
            dialog.preferredSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_EDIT)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
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

    private fun <T : JComponent> addFormField(
        panel: JPanel,
        labelText: String,
        constraints: GridBagConstraints,
        createComponent: (GridBagConstraints) -> T
    ): T {
        val label = JLabel(labelText)
        panel.add(label, constraints)

        return createComponent(constraints)
    }

}