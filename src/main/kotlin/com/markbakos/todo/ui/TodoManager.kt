package com.markbakos.todo.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

data class TodoItem(
    val keyword: String,
    val text: String,
    val fileName: String,
    val lineNumber: Int
)

fun findTodoComments(project: Project): List<TodoItem> {
    val todoComments = mutableListOf<TodoItem>()

    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return emptyList()
    val virtualFile = editor.virtualFile ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()
    val document = editor.document

    // keywords to search for
    val todoKeywords = listOf(
        "TODO", "FIXME", "HACK", "BUG", "NOTE", "REVIEW",
        "OPTIMIZE", "WARNING", "DEPRECATED", "REFACTOR"
    )

    // regex pattern for all keywords
    val todoPattern = Regex("\\b(${todoKeywords.joinToString("|")})\\s*:?\\s*(.+)", RegexOption.IGNORE_CASE)
    val comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)

    comments.forEach { comment ->
        val startOffset = comment.textRange.startOffset
        val lineNumber = document.getLineNumber(startOffset) + 1
        val commentText = processCommentText(comment.text)

        commentText.split("\n").forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                val match = todoPattern.find(trimmedLine)
                if (match != null) {
                    val keyword = match.groupValues[1].uppercase()
                    val todoText = match.groupValues[2].trim()
                    if (todoText.isNotEmpty()) {
                        todoComments.add(
                            TodoItem(
                                keyword = keyword,
                                text = todoText,
                                fileName = virtualFile.name,
                                lineNumber = lineNumber + index
                            )
                        )
                    }
                }
            }
        }
    }

    return todoComments
}

private fun processCommentText(commentText: String): String {
    return when {
        // handling single line comments
        commentText.startsWith("//") -> commentText.substring(2).trim()
        commentText.startsWith("#") -> commentText.substring(1).trim()
        commentText.startsWith("--") -> commentText.substring(2).trim() // SQL
        commentText.startsWith(";") -> commentText.substring(1).trim() // Assembly

        // handle block comments
        /* style */
        commentText.startsWith("/*") && commentText.endsWith("*/") -> {
            extractFromBlockComment(commentText, 2)
        }

        // HTML / XML comments (<!-- style -->)
        commentText.startsWith("<!--") && commentText.endsWith("-->") -> {
            commentText.substring(4, commentText.length - 3).trim()
        }

        // Python docstrings and triple quoted strings
        (commentText.startsWith("\"\"\"") && commentText.endsWith("\"\"\"")) ||
                (commentText.startsWith("'''") && commentText.endsWith("'''")) -> {
            commentText.substring(3, commentText.length - 3).trim()
        }

        else -> commentText.trim()
    }
}

private fun extractFromBlockComment(commentText: String, characters: Int): String {
    // Helper function for extracting multi line block comments
    val lines = commentText.substring(characters, commentText.length - characters)
        .split('\n')
        .map { line ->
            line.trim()
                .removePrefix("*")
                .removePrefix("//")
                .trim()
        }
        .filter { it.isNotEmpty() }
    return lines.joinToString("\n")
}
