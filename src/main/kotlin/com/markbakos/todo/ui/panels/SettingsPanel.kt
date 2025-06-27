package com.markbakos.todo.ui.panels

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class SettingsPanel(private val project: Project): JPanel(BorderLayout()) {
    init {
        setupUI()
    }

    // Setup UI components for Settings Panel
    private fun setupUI() {
        val border = BorderFactory.createEmptyBorder(16, 16, 16, 16)

        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // title
        val titleLabel = JLabel("Settings")
        titleLabel.font = titleLabel.font.deriveFont(18f)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insetsBottom(20)
        mainPanel.add(titleLabel, gbc)
    }
}