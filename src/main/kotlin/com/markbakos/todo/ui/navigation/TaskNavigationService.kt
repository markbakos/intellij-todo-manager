package com.markbakos.todo.ui.navigation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.markbakos.todo.models.Task
import com.markbakos.todo.ui.processCommentText
import javax.swing.Timer

/*
*   Service for navigating to TODO comments in the project
*/
class TaskNavigationService {
    // Navigates to a TODO comment in the project
    fun navigateToTodoComment(project: Project, task: Task): Boolean {
        if (!task.isImported || task.fileName.isNullOrEmpty()) {
            return false
        }

        try {
            val virtualFile = findFileInProject(project, task.fileName!!) ?: return false
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return false
            val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return false

            // first attempt: search by text content
            val comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)

            for (comment in comments) {
                val processedText = processCommentText(comment.text)

                // check if this comment matches task description
                val taskFullComment = task.fullCommentText?.trim()

                // check if comment contains task comment or vice versa
                if (taskFullComment != null &&
                    (processedText.contains(taskFullComment, ignoreCase = true) ||
                            taskFullComment.contains(processedText.trim(), ignoreCase = true))) {

                    // found the matching comment
                    val startOffset = comment.textRange.startOffset
                    val lineNumber = document.getLineNumber(startOffset)

                    return navigateToLine(project, virtualFile, lineNumber, document)
                }
            }

            // fallback: line number navigation
            task.lineNumber?.let { originalLineNumber ->
                val line = (originalLineNumber - 1).coerceAtLeast(0)
                if (line < document.lineCount) {
                    return navigateToLine(project, virtualFile, line, document)
                }
            }

        } catch (e: Exception) {
            println("Error in text-based navigatio: ${e.message}")
        }

        return false
    }

    // Navigates to a specific line in a file
    fun navigateToLine(project: Project, virtualFile: VirtualFile, lineNumber: Int, document: Document): Boolean {
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFile(virtualFile, true)

        ApplicationManager.getApplication().invokeLater {
            val editor = fileEditorManager.selectedTextEditor
            if (editor != null) {
                val line = lineNumber.coerceAtLeast(0)
                if (line < document.lineCount) {
                    //move caret to line
                    val offset = document.getLineStartOffset(line)
                    editor.caretModel.moveToOffset(offset)

                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

                    highlightLine(editor, line)
                }
            }
        }
        return true
    }

    // Finds a file in the project by file name
    fun findFileInProject(project: Project, fileName: String): VirtualFile? {
        val projectScope = GlobalSearchScope.projectScope(project)

        val files = FilenameIndex.getFilesByName(project, fileName, projectScope)

        return when {
            files.isEmpty() -> null
            files.size == 1 -> files.first().virtualFile
            else -> {
                // TODO: currently only returns first file found, in future add show dialog to let user choose
                files.first().virtualFile
            }
        }
    }

    // Highlights a specific line in the editor for 2 seconds
    fun highlightLine(editor: Editor, line: Int) {
        val document = editor.document
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)

        val markupModel = editor.markupModel
        val highlightAttributes = TextAttributes().apply {
            backgroundColor = JBColor.CYAN
        }

        val rangeHighlighter = markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION - 1,
            highlightAttributes,
            HighlighterTargetArea.LINES_IN_RANGE
        )

        Timer(2000) {
            ApplicationManager.getApplication().invokeLater {
                markupModel.removeHighlighter(rangeHighlighter)
            }
        }.apply {
            isRepeats = false
            start()
        }
    }
}