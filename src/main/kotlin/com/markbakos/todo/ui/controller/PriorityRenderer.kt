package com.markbakos.todo.ui.controller

import com.intellij.ui.JBColor
import com.markbakos.todo.models.Task
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class PriorityColorRenderer: DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        val isPriorityColumn = table?.convertColumnIndexToModel(column) == 1

        if (isPriorityColumn && value != null) {
            val priorityValue = value.toString()
            val textColor = when (priorityValue) {
                Task.Priority.LOW.toString() -> JBColor(Color(0, 128, 0), Color(0, 200, 0))
                Task.Priority.MEDIUM.toString() -> JBColor(Color(255, 165, 0), Color(255, 175, 0))
                Task.Priority.HIGH.toString() -> JBColor(Color(255, 0, 0), Color(255, 30, 30))
                Task.Priority.CRITICAL.toString() -> JBColor(Color(139, 0, 0), Color(200, 0, 0))
                else -> JBColor.BLACK
            }

            component.foreground = textColor
            font = font.deriveFont(Font.BOLD)
        } else {
            component.foreground = if (isSelected) table?.selectionForeground else table?.foreground
            font = font.deriveFont(Font.PLAIN)
        }

        return component
    }
}

val priorityComparator = Comparator<Any> { o1, o2 ->
    val p1 = o1.toString()
    val p2 = o2.toString()

    val priority1 = try { Task.Priority.valueOf(p1) } catch (_: Exception ) { Task.Priority.LOW }
    val priority2 = try { Task.Priority.valueOf(p2) } catch (_: Exception ) { Task.Priority.LOW }

    return@Comparator priority1.ordinal - priority2.ordinal
}