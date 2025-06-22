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
    val lineNumber: Int,
    val fullCommentText: String,
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
        val commentStartLine = document.getLineNumber(startOffset)
        val commentText = processCommentText(comment.text)

        val lines = commentText.split("\n")
        var i = 0
        while (i < lines.size) {
            val trimmedLine = lines[i].trim()
            if (trimmedLine.isNotEmpty()) {
                val match = todoPattern.find(trimmedLine)
                if (match != null) {
                    val keyword = match.groupValues[1].uppercase()
                    var todoText = match.groupValues[2].trim()

                    // for multi line todos, continue collecting lines
                    var j = i + 1
                    while (j < lines.size) {
                        val nextLine = lines[j].trim()
                        if (nextLine.isNotEmpty() && !todoKeywords.any { nextLine.uppercase().startsWith(it) }) {
                            todoText += " " + nextLine
                            j++
                        } else {
                            break
                        }
                    }

                    if (todoText.isNotEmpty()) {
                        todoComments.add(
                            TodoItem(
                                keyword = keyword,
                                text = todoText.trim(),
                                fileName = virtualFile.name,
                                lineNumber = commentStartLine + i + 1, // +1 for 1-based line numbering
                                fullCommentText = commentText.trim().replace("\n", " "),
                            )
                        )
                    }
                    i = j // skip the lines already processed
                } else {
                    i++
                }
            } else {
                i++
            }
        }
    }

    return todoComments
}

public fun processCommentText(commentText: String): String {
    return when {
        // handling single line comments
        commentText.startsWith("//") -> commentText.substring(2).trim()
        commentText.startsWith("#") -> commentText.substring(1).trim()
        commentText.startsWith("--") -> commentText.substring(2).trim() // SQL
        commentText.startsWith(";") -> commentText.substring(1).trim() // Assembly

        // handle block comments
        /* style */
        commentText.startsWith("/*") && commentText.endsWith("*/") -> {
            extractFromBlockComment(commentText, 2, 2)
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

private fun extractFromBlockComment(commentText: String, startChars: Int, endChars: Int): String {
    // Helper function for extracting multi line block comments
    val content = commentText.substring(startChars, commentText.length - endChars)
    val lines = content.split('\n').mapIndexed { index, line ->
        val trimmedLine = line.trim()
        val cleanedLine = if (trimmedLine.startsWith("*")) {
            trimmedLine.substring(1).trim()
        } else {
            trimmedLine
        }
        cleanedLine
    }.filter { it.isNotEmpty() } // remove empty lines

    return lines.joinToString("\n")
}