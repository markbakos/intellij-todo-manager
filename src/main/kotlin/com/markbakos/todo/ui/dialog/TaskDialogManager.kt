package com.markbakos.todo.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.markbakos.todo.models.Task
import com.markbakos.todo.ui.panels.TagSelectionPanel
import com.markbakos.todo.ui.controller.TodoItem
import com.markbakos.todo.ui.controller.findTodoComments
import com.markbakos.todo.ui.controller.I18nManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dialog
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel

object TaskDialogManager {

    private const val DIALOG_WIDTH = 520
    private const val DIALOG_HEIGHT_ADD = 750
    private const val DIALOG_HEIGHT_EDIT = 750
    private const val DESCRIPTION_ROWS = 8
    private const val TEXT_FIELD_COLUMNS = 25
    private const val VERTICAL_GAP = 5
    private const val HORIZONTAL_GAP = 10
    private const val DESCRIPTION_HEIGHT = 200
    private const val TAG_SECTION_HEIGHT = 180
    private const val TODO_COMMENT_HEIGHT = 150

    fun showAddTaskDialog(
        parent: JPanel,
        project: Project,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        val i18nManager = I18nManager.getInstance(project)

        try {
            val dialog = JDialog()
            dialog.isModal = true
            dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
            dialog.isAlwaysOnTop = true
            dialog.title = i18nManager.getString("dialog.addTask")
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

            val descriptionArea = addFormField(panel, i18nManager.getString("label.description.dialog"), gbc) { constraints ->
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

            // track selected TodoItem
            var selectedTodoItem: TodoItem? = null

            val importButton = JButton(i18nManager.getString("button.importTodo"))
            importButton.addActionListener {
                val todoComments = findTodoComments(project)
                if (todoComments.isNotEmpty()) {
                    showTodoCommentsDialog(parent, project, todoComments) { selectedComment ->
                        descriptionArea.text = "${selectedComment.keyword}: ${selectedComment.text}"
                        selectedTodoItem = selectedComment
                    }
                } else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        i18nManager.getString("message.noTodos"),
                        i18nManager.getString("error.noTodos"),
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
            panel.add(importButton, gbc)

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

            val priorityCombo = addFormField(panel, i18nManager.getString("label.priority.dialog"), gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                panel.add(combo, constraints)
                combo
            }

            var selectedPrerequisiteTask: Task? = null
            val prerequisiteButton = addFormField(panel, i18nManager.getString("label.prerequisite.dialog"), gbc) { constraints ->
                val button = JButton(i18nManager.getString("button.addPrerequisite"))
                button.addActionListener {
                    showPrerequisiteSelectionDialog(dialog, project, tasks) { task: Task? ->
                        selectedPrerequisiteTask = task
                        button.text = if (task != null) {
                            "${i18nManager.getString("label.prerequisite")}: ${task.description.take(30)}${if (task.description.length > 30) "..." else ""}"
                        } else {
                            i18nManager.getString("button.addPrerequisite")
                        }
                    }
                }
                panel.add(button, constraints)
                button
            }

            val linkField = addFormField(panel, i18nManager.getString("label.link.dialog"), gbc) { constraints ->
                val field = JTextField()
                panel.add(field, constraints)
                field
            }

            val buttonPanel = JPanel()
            buttonPanel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)

            val saveButton = JButton(i18nManager.getString("button.save"))
            saveButton.addActionListener {
                if (descriptionArea.text.isNotEmpty()) {
                    val selectedTags = tagSelectionPanel.getSelectedTags()

                    val newTask = Task(
                        description = descriptionArea.text,
                        tags = selectedTags.toMutableList(),
                        priority = priorityCombo.selectedItem as Task.Priority,
                        link = linkField.text.takeIf { it.isNotBlank() },
                        isImported = selectedTodoItem != null,
                        fileName = selectedTodoItem?.fileName,
                        lineNumber = selectedTodoItem?.lineNumber,
                        fullCommentText = selectedTodoItem?.fullCommentText,
                        prerequisiteTaskId = selectedPrerequisiteTask?.id
                    )
                    tasks.add(newTask)
                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                }
                else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        i18nManager.getString("message.emptyDescription"),
                        i18nManager.getString("error.validation"),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            val cancelButton = JButton(i18nManager.getString("button.cancel"))
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
                "${i18nManager.getString("message.creatingDialog")}: ${e.message}\n${e.stackTraceToString()}",
                i18nManager.getString("error.error"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun showEditTaskDialog(
        parent: JPanel,
        project: Project,
        task: Task,
        tasks: MutableList<Task>,
        saveTasks: () -> Unit,
        refreshTabs: () -> Unit
    ) {
        val i18nManager = I18nManager.getInstance(project)

        try {
            val dialog = JDialog()
            dialog.isModal = true
            dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
            dialog.isAlwaysOnTop = true
            dialog.title = "${i18nManager.getString("dialog.editTask")} #${task.id.substringAfter("TASK_")}"
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

            val descriptionArea = addFormField(panel, i18nManager.getString("label.description.dialog"), gbc) { constraints ->
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

            val priorityCombo = addFormField(panel, i18nManager.getString("label.priority.dialog"), gbc) { constraints ->
                val combo = ComboBox(Task.Priority.values())
                combo.selectedItem = task.priority
                panel.add(combo, constraints)
                combo
            }

            var selectedPrerequisiteTask: Task? = tasks.find { it.id == task.prerequisiteTaskId }
            val prerequisiteButton = addFormField(panel, i18nManager.getString("label.prerequisite.dialog"), gbc) { constraints ->
                val button = JButton()

                fun updateButtonText() {
                    button.text = if (selectedPrerequisiteTask != null) {
                        "${i18nManager.getString("label.prerequisite")}: ${selectedPrerequisiteTask!!.description.take(30)}${if (selectedPrerequisiteTask!!.description.length > 30) "..." else ""}"
                    } else {
                        i18nManager.getString("button.addPrerequisite")
                    }
                }

                updateButtonText()

                button.addActionListener {
                    showPrerequisiteSelectionDialog(dialog, project, tasks, selectedPrerequisiteTask) { newTask ->
                        selectedPrerequisiteTask = newTask
                        updateButtonText()
                    }
                }
                panel.add(button, constraints)
                button
            }

            val linkField = addFormField(panel, i18nManager.getString("label.link.dialog"), gbc) { constraints ->
                val field = JTextField(task.link ?: "")
                panel.add(field, constraints)
                field
            }

            val statusCombo = addFormField(panel, i18nManager.getString("label.status"), gbc) { constraints ->
                val combo = ComboBox(Task.TaskStatus.values())
                combo.selectedItem = task.status
                panel.add(combo, constraints)
                combo
            }

            // create formatter
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")

            var dateLabel: JLabel

            if (task.status == Task.TaskStatus.DONE && task.finishDate != null) {
                dateLabel = JLabel("${i18nManager.getString("label.finishedOn")} ${task.finishDate!!.format(formatter)}")
            } else {
                dateLabel = JLabel("${i18nManager.getString("label.createdOn")} ${task.date.format(formatter)}")
            }

            dateLabel.horizontalAlignment = JLabel.CENTER

            // create panel for datelabel for design
            val dateLabelPanel = JPanel(BorderLayout())
            dateLabelPanel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
            dateLabelPanel.add(dateLabel, BorderLayout.CENTER)

            val buttonPanel = JPanel()
            buttonPanel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)

            val saveButton = JButton(i18nManager.getString("button.update"))
            saveButton.addActionListener {
                if (descriptionArea.text.isNotEmpty()) {
                    task.description = descriptionArea.text
                    task.tags = tagSelectionPanel.getSelectedTags().toMutableList()
                    task.priority = priorityCombo.selectedItem as Task.Priority
                    task.status = statusCombo.selectedItem as Task.TaskStatus
                    task.link = linkField.text.takeIf { it.isNotBlank() }
                    task.prerequisiteTaskId = selectedPrerequisiteTask?.id

                    saveTasks()
                    refreshTabs()
                    dialog.dispose()
                } else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        i18nManager.getString("message.emptyDescription"),
                        i18nManager.getString("error.validation"),
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            val cancelButton = JButton(i18nManager.getString("button.cancel"))
            cancelButton.addActionListener { dialog.dispose() }

            buttonPanel.add(saveButton)
            buttonPanel.add(cancelButton)

            val bottomPanel = JPanel(BorderLayout())
            bottomPanel.add(dateLabelPanel, BorderLayout.NORTH)
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH)

            dialog.add(panel, BorderLayout.CENTER)
            dialog.add(bottomPanel, BorderLayout.SOUTH)

            dialog.minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_EDIT)
            dialog.preferredSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_EDIT)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                parent,
                "${i18nManager.getString("message.creatingDialog")}: ${e.message}\n${e.stackTraceToString()}!",
                i18nManager.getString("error.error"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showPrerequisiteSelectionDialog(
        parent: JDialog,
        project: Project,
        tasks: List<Task>,
        currentPrerequisite: Task? = null,
        onSelect: (Task?) -> Unit
    ) {
        val i18nManager = I18nManager.getInstance(project)

        val dialog = JDialog(parent)
        dialog.isModal = true
        dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
        dialog.title = i18nManager.getString("dialog.selectPrerequisite")
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.layout = BorderLayout()

        val mainPanel = JPanel(BorderLayout())
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val filterPanel = JPanel()
        filterPanel.add(JLabel(i18nManager.getString("label.filterByStatus")))
        val statusFilter = ComboBox<String>()
        statusFilter.addItem(i18nManager.getString("label.all"))
        Task.TaskStatus.values().forEach { status ->
            statusFilter.addItem(status.name)
        }
        filterPanel.add(statusFilter)

        val listModel = DefaultListModel<TaskDisplayItem>()
        val taskList = JBList(listModel)
        taskList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        // custom renderer for task display
        taskList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is TaskDisplayItem) {
                    text = value.displayText
                }
                return this
            }
        }

        fun updateTaskList() {
            listModel.clear()
            val selectedStatus = statusFilter.selectedItem as String
            val filteredTasks = if (selectedStatus == i18nManager.getString("label.all")) {
                tasks
            } else {
                tasks.filter { it.status.name == selectedStatus }
            }

            filteredTasks.forEach { task ->
                val displayText = "${task.description.take(50)}${if (task.description.length > 50) "..." else ""}"
                listModel.addElement(TaskDisplayItem(task, displayText))
            }

            currentPrerequisite?.let { current ->
                for (i in 0 until listModel.size()) {
                    if (listModel.getElementAt(i).task.id == current.id) {
                        taskList.selectedIndex = i
                        break
                    }
                }
            }
        }

        statusFilter.addActionListener { updateTaskList() }
        updateTaskList()

        val scrollPane = JBScrollPane(taskList)
        scrollPane.preferredSize = Dimension(540, 280)

        // button panel
        val buttonPanel = JPanel()
        val selectButton = JButton(i18nManager.getString("button.select"))
        val removeButton = JButton(i18nManager.getString("button.removePrerequisite"))
        val cancelButton = JButton(i18nManager.getString("button.cancel"))

        selectButton.addActionListener {
            val selectedIndex = taskList.selectedIndex
            if (selectedIndex >= 0) {
                val selectedTask = listModel.getElementAt(selectedIndex).task
                onSelect(selectedTask)
                dialog.dispose()
            } else {
                JOptionPane.showMessageDialog(
                    dialog,
                    i18nManager.getString("message.selectTaskFromList"),
                    i18nManager.getString("error.noTaskSelected"),
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }

        removeButton.addActionListener {
            onSelect(null) // clear prerequisite
            dialog.dispose()
        }

        cancelButton.addActionListener { dialog.dispose() }

        buttonPanel.add(selectButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(cancelButton)

        mainPanel.add(filterPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        dialog.add(mainPanel, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.size = Dimension(600, 400)
        dialog.setLocationRelativeTo(parent)
        dialog.isVisible = true
    }

    private data class TaskDisplayItem(
        val task: Task,
        val displayText: String
    )

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

    private fun showTodoCommentsDialog(
        parent: JPanel,
        project: Project,
        comments: List<TodoItem>,
        onSelect: (TodoItem) -> Unit
    ) {
        val i18nManager = I18nManager.getInstance(project)

        val dialog = JDialog()
        dialog.isModal = true
        dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
        dialog.isAlwaysOnTop = true
        dialog.title = i18nManager.getString("dialog.selectTodoComment")
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.layout = BorderLayout()

        val listModel = DefaultListModel<String>()
        comments.forEach { todoItem ->
            listModel.addElement("${todoItem.keyword}: ${todoItem.text} (${todoItem.fileName}:${todoItem.lineNumber})")
        }

        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val scrollPane = JBScrollPane(list)
        scrollPane.preferredSize = Dimension(400, TODO_COMMENT_HEIGHT)

        val buttonPanel = JPanel()
        val selectButton = JButton(i18nManager.getString("button.select"))
        val cancelButton = JButton(i18nManager.getString("button.cancel"))

        selectButton.addActionListener {
            val selectedIndex = list.selectedIndex
            if (selectedIndex >= 0) {
                onSelect(comments[selectedIndex])
                dialog.dispose()
            }
        }

        cancelButton.addActionListener { dialog.dispose() }

        buttonPanel.add(selectButton)
        buttonPanel.add(cancelButton)

        dialog.add(scrollPane, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(parent)
        dialog.isVisible = true
    }
}