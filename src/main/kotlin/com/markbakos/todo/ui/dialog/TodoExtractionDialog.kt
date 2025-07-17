package com.markbakos.todo.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.markbakos.todo.models.Task
import com.markbakos.todo.ui.controller.I18nManager
import com.markbakos.todo.ui.controller.TodoItem
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.DefaultTableModel

class TodoExtractionDialog(
    private val project: Project,
    private val todoItems: List<TodoItem>,
    private val onAddToTasks: (List<TodoItem>) -> Unit
) : DialogWrapper(project) {
    
    private val i18nManager = I18nManager.getInstance(project)
    private lateinit var table: JBTable
    private lateinit var tableModel: DefaultTableModel
    
    init {
        title = i18nManager.getString("dialog.extractTodos")
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // create table
        tableModel = object : DefaultTableModel(
            arrayOf("Type", "File", "Line", "Description"), 0
        ) {
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when (columnIndex) {
                    2 -> Int::class.java // line column
                    else -> String::class.java
                }
            }
            
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false // no cells are editable
            }
        }
        
        table = JBTable(tableModel)
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        table.rowSelectionAllowed = true
        table.columnSelectionAllowed = false
        
        // configure column widths
        table.columnModel.getColumn(0).preferredWidth = 80  // Type
        table.columnModel.getColumn(1).preferredWidth = 200 // File
        table.columnModel.getColumn(2).preferredWidth = 60  // Line
        table.columnModel.getColumn(3).preferredWidth = 400 // Description
        
        // populate table
        todoItems.forEach { todoItem ->
            tableModel.addRow(arrayOf(
                todoItem.keyword,
                todoItem.fileName,
                todoItem.lineNumber,
                todoItem.text
            ))
        }
        
        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(800, 400)
        
        // Add header panel with selection buttons
        val headerPanel = JPanel()
        val selectAllButton = JButton(i18nManager.getString("button.selectAll"))
        val clearSelectionButton = JButton(i18nManager.getString("button.clearSelection"))
        
        selectAllButton.addActionListener {
            table.selectAll()
        }
        
        clearSelectionButton.addActionListener {
            table.clearSelection()
        }
        
        headerPanel.add(selectAllButton)
        headerPanel.add(clearSelectionButton)
        
        // Create a combined north panel with info and buttons
        val northPanel = JPanel(BorderLayout())
        val infoLabel = JLabel("${todoItems.size} ${i18nManager.getString("dialog.extractTodosTitle")}. ${i18nManager.getString("dialog.extractTodosInfo")}")
        infoLabel.border = JBUI.Borders.empty(10)
        northPanel.add(infoLabel, BorderLayout.NORTH)
        northPanel.add(headerPanel, BorderLayout.SOUTH)
        
        panel.add(northPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    override fun createActions(): Array<Action> {
        val extractToFileAction = object : AbstractAction(i18nManager.getString("button.extractToFile")) {
            override fun actionPerformed(e: ActionEvent) {
                extractToFile()
            }
        }
        
        val addToTasksAction = object : AbstractAction(i18nManager.getString("button.addToTasks")) {
            override fun actionPerformed(e: ActionEvent) {
                addToTasks()
            }
        }
        
        return arrayOf(extractToFileAction, addToTasksAction, cancelAction)
    }
    
    private fun getSelectedTodoItems(): List<TodoItem> {
        val selectedItems = mutableListOf<TodoItem>()
        val selectedRows = table.selectedRows
        for (rowIndex in selectedRows) {
            if (rowIndex >= 0 && rowIndex < todoItems.size) {
                selectedItems.add(todoItems[rowIndex])
            }
        }
        return selectedItems
    }
    
    private fun extractToFile() {
        val selectedItems = getSelectedTodoItems()
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                i18nManager.getString("message.selectTodosToProcess"),
                i18nManager.getString("error.validation"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        val descriptor = FileSaverDescriptor(
            i18nManager.getString("dialog.extractToFile"),
            i18nManager.getString("dialog.extractToFileInfo"),
            "txt", "md"
        )
        
        val fileChooser = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = fileChooser.save(null as VirtualFile?, "todo_comments_${getCurrentTimestamp()}.txt")
        
        result?.let { wrapper ->
            try {
                val file: File = wrapper.file
                FileWriter(file).use { writer ->
                    writer.write("TODO Comments Extracted from Project\n")
                    writer.write("Generated on: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}\n")
                    writer.write("=".repeat(50) + "\n\n")
                    
                    selectedItems.groupBy { it.fileName }.forEach { (fileName, items) ->
                        writer.write("File: $fileName\n")
                        writer.write("-".repeat(30) + "\n")
                        items.forEach { item ->
                            writer.write("Line ${item.lineNumber}: [${item.keyword}] ${item.text}\n")
                        }
                        writer.write("\n")
                    }
                }
                
                JOptionPane.showMessageDialog(
                    contentPanel,
                    i18nManager.getString("message.todosExtracted", file.absolutePath),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                )
                
                close(OK_EXIT_CODE)
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "Error saving file: ${e.message}",
                    i18nManager.getString("error.error"),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    private fun addToTasks() {
        val selectedItems = getSelectedTodoItems()
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                i18nManager.getString("message.selectTodosToProcess"),
                i18nManager.getString("error.validation"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        onAddToTasks(selectedItems)
        
        JOptionPane.showMessageDialog(
            contentPanel,
            i18nManager.getString("message.todosAddedToTasks", selectedItems.size),
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        )
        
        close(OK_EXIT_CODE)
    }
    
    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }
    
    override fun getPreferredSize(): Dimension {
        return Dimension(900, 500)
    }
}
