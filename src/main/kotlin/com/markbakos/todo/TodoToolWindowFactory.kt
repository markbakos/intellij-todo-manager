package com.markbakos.todo

import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent


class TodoToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(@NotNull project: Project, @NotNull toolWindow: ToolWindow) {
        val todoManagerPanel: JComponent = TodoManagerPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(todoManagerPanel, "", false)

        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }
}