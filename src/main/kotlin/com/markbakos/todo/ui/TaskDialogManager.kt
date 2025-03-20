package com.markbakos.todo.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.project.Project
import com.markbakos.todo.models.Task
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

object TaskDialogManager {

    private const val DIALOG_WIDTH = 520
    private const val DIALOG_HEIGHT_ADD = 500
    private const val DIALOG_HEIGHT_EDIT = 550
    private const val DESCRIPTION_ROWS = 8
    private const val TEXT_FIELD_COLUMNS = 25
    private const val VERTICAL_GAP = 5
    private const val HORIZONTAL_GAP = 10
    private const val DESCRIPTION_HEIGHT = 200
    private const val TAG_SECTION_HEIGHT = 180

    fun showAddTaskDialog(
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        try {
            val dialog = JDialog()
            dialog.isModal = true
            dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
            dialog.isAlwaysOnTop = true
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

            val tagSelectionPanel = addFormField(panel, "", gbc) { constraints ->
                constraints.fill = GridBagConstraints.BOTH
                constraints.weighty = 1.0

                val tagPanel = TagSelectionPanel(project)
                tagPanel.preferredSize = Dimension(0, TAG_SECTION_HEIGHT)
                tagPanel.minimumSize = Dimension(0, TAG_SECTION_HEIGHT)
                panel.add(tagPanel, constraints)

                constraints.weighty = 0.0
                constraints.fill = GridBagConstraints.HORIZONTAL

                tagPanel
            }

            val priorityCombo = addFormField(panel, "Priority:", gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                panel.add(combo, constraints)
                combo
            }

            val linkField = addFormField(panel, "Link (optional):", gbc) { constraints ->
                val field = JTextField()
                panel.add(field, constraints)
                field
            }

            val buttonPanel = JPanel()
            buttonPanel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)

            val saveButton = JButton("Save Task")
            saveButton.addActionListener {
                if (descriptionArea.text.isNotEmpty()) {
                    val selectedTags = tagSelectionPanel.getSelectedTags()

                    val newTask = Task(
                        description = descriptionArea.text,
                        tags = selectedTags.toMutableList(),
                        priority = priorityCombo.selectedItem as Task.Priority,
                        link = linkField.text.takeIf { it.isNotBlank() }
                    )
                    tasks.add(newTask)
                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                }
                else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Description cannot be empty!",
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
        project: Project,
        task: Task,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        try {
            val dialog = JDialog()
            dialog.isModal = true
            dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
            dialog.isAlwaysOnTop = true
            dialog.title = "Edit Task #${task.id.substringAfter("TASK_")}"
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

            val tagSelectionPanel = addFormField(panel, "", gbc) { constraints ->
                constraints.fill = GridBagConstraints.BOTH
                constraints.weighty = 1.0

                val tagPanel = TagSelectionPanel(project, task.tags)
                tagPanel.preferredSize = Dimension(0, TAG_SECTION_HEIGHT)
                tagPanel.minimumSize = Dimension(0, TAG_SECTION_HEIGHT)
                panel.add(tagPanel, constraints)

                constraints.weighty = 0.0
                constraints.fill = GridBagConstraints.HORIZONTAL

                tagPanel
            }

            val priorityCombo = addFormField(panel, "Priority:", gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                combo.selectedItem = task.priority
                panel.add(combo, constraints)
                combo
            }

            val linkField = addFormField(panel, "Link (optional):", gbc) { constraints ->
                val field = JTextField(task.link ?: "")
                panel.add(field, constraints)
                field
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
                if (descriptionArea.text.isNotEmpty()) {
                    task.description = descriptionArea.text
                    task.tags = tagSelectionPanel.getSelectedTags().toMutableList()
                    task.priority = priorityCombo.selectedItem as Task.Priority
                    task.status = statusCombo.selectedItem as Task.TaskStatus
                    task.link = linkField.text.takeIf { it.isNotBlank() }

                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                } else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Description cannot be empty!",
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
        if (labelText.isNotEmpty()) {
            val label = JLabel(labelText)
            panel.add(label, constraints)
        }

        return createComponent(constraints)
    }
}