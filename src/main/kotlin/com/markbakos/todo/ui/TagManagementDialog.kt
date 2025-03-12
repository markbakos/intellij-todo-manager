package com.markbakos.todo.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.markbakos.todo.models.TagManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

class TagManagementDialog(
    private val project: Project,
    private val parentPanel: TagSelectionPanel
) : DialogWrapper(project, true) {

    private val tagManager = TagManager.getInstance(project)
    private val listModel = DefaultListModel<String>()
    private val tagList = JBList(listModel)
    private val addTagField = JTextField(20)

    init {
        title = "Manage Tags"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(350, 400)

        val addPanel = JPanel(BorderLayout(5, 0))
        addPanel.border = BorderFactory.createTitledBorder("Add New Tag")
        addPanel.add(addTagField, BorderLayout.CENTER)

        val addButton = JButton("Add")
        addButton.addActionListener {
            addNewTag()
        }
        addPanel.add(addButton, BorderLayout.EAST)

        val listPanel = JPanel(BorderLayout())
        listPanel.border = BorderFactory.createTitledBorder("Available Tags")

        refreshTagList()
        tagList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val scrollPane = JBScrollPane(tagList)
        scrollPane.preferredSize = Dimension(0, 250)
        listPanel.add(scrollPane, BorderLayout.CENTER)

        val buttonsPanel = JPanel(GridLayout(1, 2, 5, 0))

        val editButton = JButton("Edit Selected")
        editButton.addActionListener {
            editSelectedTag()
        }

        val removeButton = JButton("Remove Selected")
        removeButton.addActionListener {
            removeSelectedTag()
        }

        buttonsPanel.add(editButton)
        buttonsPanel.add(removeButton)
        listPanel.add(buttonsPanel, BorderLayout.SOUTH)

        panel.add(addPanel, BorderLayout.NORTH)
        panel.add(listPanel, BorderLayout.CENTER)

        return panel
    }

    private fun refreshTagList() {
        listModel.clear()
        tagManager.getAllTags().forEach { listModel.addElement(it) }
    }

    private fun addNewTag() {
        val newTag = addTagField.text.trim()
        if (newTag.isNotEmpty() && !tagManager.getAllTags().contains(newTag)) {
            tagManager.addTag(newTag)
            refreshTagList()
            addTagField.text = ""
            parentPanel.refreshTagList()
        } else if (newTag.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Tag name cannot be empty.",
                "Invalid Tag",
                JOptionPane.WARNING_MESSAGE
            )
        } else {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Tag already exists.",
                "Duplicate Tag",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun editSelectedTag() {
        val selectedTag = tagList.selectedValue ?: return
        val index = tagList.selectedIndex

        val newTagName = JOptionPane.showInputDialog(
            contentPanel,
            "Edit tag name:",
            "Edit Tag",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            selectedTag
        ) as? String ?: return

        if (newTagName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Tag name cannot be empty.",
                "Invalid Tag",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        if (newTagName != selectedTag && tagManager.getAllTags().contains(newTagName)) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Tag already exists.",
                "Duplicate Tag",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        tagManager.removeTag(selectedTag)
        tagManager.addTag(newTagName.trim())
        refreshTagList()
        parentPanel.refreshTagList()
    }

    private fun removeSelectedTag() {
        val selectedTag = tagList.selectedValue ?: return

        val confirm = JOptionPane.showConfirmDialog(
            contentPanel,
            "Are you sure you want to remove tag '$selectedTag'?\n" +
                    "This will not remove the tag from existing tasks.",
            "Remove Tag",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (confirm == JOptionPane.YES_OPTION) {
            tagManager.removeTag(selectedTag)
            refreshTagList()
            parentPanel.refreshTagList()
        }
    }

    override fun doValidate(): ValidationInfo? {
        return null
    }
}