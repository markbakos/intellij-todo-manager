package com.markbakos.todo.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.markbakos.todo.models.TagManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class TagSelectionPanel(
    private val project: Project,
    private val initialSelectedTags: List<String> = emptyList()
) : JPanel(BorderLayout()) {
    private val tagManager = TagManager.getInstance(project)
    private val selectedTags = mutableListOf<String>()
    private val tagCheckBoxList = CheckBoxList<String>()

    init {
        selectedTags.addAll(initialSelectedTags)
        setupUI()
    }

    private fun setupUI() {
        val mainPanel = JPanel(BorderLayout(5, 0))
        mainPanel.border = BorderFactory.createTitledBorder("Tags")

        refreshTagList()

        val scrollPane = JBScrollPane(tagCheckBoxList)
        scrollPane.preferredSize = Dimension(0, 150)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        val manageTagsButton = JButton("Manage Tags...")
        buttonPanel.add(manageTagsButton)

        manageTagsButton.addActionListener {
            TagManagementDialog(project, this).show()
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        add(mainPanel, BorderLayout.CENTER)
    }

    fun refreshTagList() {
        tagCheckBoxList.clear()

        tagManager.getAllTags().forEach { tag ->
            tagCheckBoxList.addItem(tag, tag, selectedTags.contains(tag))
        }
    }

    fun getSelectedTags(): List<String> {
        selectedTags.clear()

        for (i in 0 until tagCheckBoxList.itemsCount) {
            if (tagCheckBoxList.isItemSelected(i)) {
                tagCheckBoxList.getItemAt(i)?.let { selectedTags.add(it) }
            }
        }

        return selectedTags.toList()
    }
}