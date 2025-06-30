package com.markbakos.todo.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.markbakos.todo.models.TagManager
import com.markbakos.todo.ui.controller.I18nManager
import com.markbakos.todo.ui.panels.TagSelectionPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class TagManagementDialog(
    private val project: Project,
    private val parentPanel: TagSelectionPanel
) : DialogWrapper(project, true), I18nManager.LanguageChangeListener {

    private val tagManager = TagManager.Companion.getInstance(project)
    private val i18nManager = I18nManager.getInstance(project)
    private val listModel = DefaultListModel<String>()
    private val tagList = JBList(listModel)
    private val addTagField = JTextField(20)

    // UI components that need text updates
    private lateinit var addPanel: JPanel
    private lateinit var listPanel: JPanel
    private lateinit var addButton: JButton
    private lateinit var editButton: JButton
    private lateinit var removeButton: JButton

    init {
        i18nManager.addLanguageChangeListener(this)
        title = getString("dialog.manageTags")
        init()
    }

    private fun getString(key: String): String = i18nManager.getString(key)

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(350, 400)

        addPanel = JPanel(BorderLayout(5, 0))
        addPanel.border = BorderFactory.createTitledBorder(getString("label.addNewTag"))
        addPanel.add(addTagField, BorderLayout.CENTER)

        addButton = JButton(getString("button.add"))
        addButton.addActionListener {
            addNewTag()
        }
        addPanel.add(addButton, BorderLayout.EAST)

        listPanel = JPanel(BorderLayout())
        listPanel.border = BorderFactory.createTitledBorder(getString("label.availableTags"))

        refreshTagList()
        tagList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val scrollPane = JBScrollPane(tagList)
        scrollPane.preferredSize = Dimension(0, 250)
        listPanel.add(scrollPane, BorderLayout.CENTER)

        val buttonsPanel = JPanel(GridLayout(1, 2, 5, 0))

        editButton = JButton(getString("button.editSelected"))
        editButton.addActionListener {
            editSelectedTag()
        }

        removeButton = JButton(getString("button.removeSelected"))
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
                getString("message.tagNameEmpty"),
                getString("error.invalidTag"),
                JOptionPane.WARNING_MESSAGE
            )
        } else {
            JOptionPane.showMessageDialog(
                contentPanel,
                getString("message.tagAlreadyExists"),
                getString("error.duplicateTag"),
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun editSelectedTag() {
        val selectedTag = tagList.selectedValue ?: return
        val index = tagList.selectedIndex

        val newTagName = JOptionPane.showInputDialog(
            contentPanel,
            getString("dialog.editTagPrompt"),
            getString("dialog.editTag"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            selectedTag
        ) as? String ?: return

        if (newTagName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                getString("message.tagNameEmpty"),
                getString("error.invalidTag"),
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        if (newTagName != selectedTag && tagManager.getAllTags().contains(newTagName)) {
            JOptionPane.showMessageDialog(
                contentPanel,
                getString("message.tagAlreadyExists"),
                getString("error.duplicateTag"),
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
            getString("dialog.removeTagConfirmation") + "\n" +
                    getString("dialog.removeTagWarning"),
            getString("dialog.removeTag"),
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

    // language change listener implementation
    override fun onLanguageChanged(newLocale: Locale) {
        updateTexts()
    }

    private fun updateTexts() {
        // update dialog title
        title = getString("dialog.manageTags")

        // update panel borders
        addPanel.border = BorderFactory.createTitledBorder(getString("label.addNewTag"))
        listPanel.border = BorderFactory.createTitledBorder(getString("label.availableTags"))

        // update button texts
        addButton.text = getString("button.add")
        editButton.text = getString("button.editSelected")
        removeButton.text = getString("button.removeSelected")

        // refresh the dialog
        contentPanel.revalidate()
        contentPanel.repaint()
    }
}