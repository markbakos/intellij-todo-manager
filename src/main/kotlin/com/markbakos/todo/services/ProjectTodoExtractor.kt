package com.markbakos.todo.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.markbakos.todo.ui.controller.TodoItem
import com.markbakos.todo.ui.controller.processCommentText
import com.markbakos.todo.ui.controller.I18nManager
import java.util.concurrent.CompletableFuture

class ProjectTodoExtractor(private val project: Project) {

    private val i18nManager = I18nManager.getInstance(project)
    
    private val todoKeywords = listOf(
        "TODO", "FIXME", "HACK", "BUG", "NOTE", "REVIEW",
        "OPTIMIZE", "WARNING", "DEPRECATED", "REFACTOR"
    )
    
    private val todoPattern = Regex("\\b(${todoKeywords.joinToString("|")})\\s*:?\\s*(.+)", RegexOption.IGNORE_CASE)
    
    fun extractAllTodosAsync(onComplete: (List<TodoItem>) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, i18nManager.getString("dialog.extractingTodos"), true) {
            override fun run(indicator: ProgressIndicator) {
                val todos = extractAllTodos(indicator)
                onComplete(todos)
            }
        })
    }
    
    private fun extractAllTodos(indicator: ProgressIndicator): List<TodoItem> {
        val todoComments = mutableListOf<TodoItem>()
        val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        
        indicator.text = i18nManager.getString("dialog.scanningProjectFiles")
        indicator.isIndeterminate = false
        
        val allFiles = mutableListOf<VirtualFile>()
        
        // collect all files in the project
        projectFileIndex.iterateContent { file ->
            if (!file.isDirectory && isProcessableFile(file)) {
                allFiles.add(file)
            }
            true
        }
        
        indicator.text = "${i18nManager.getString("dialog.processing")} ${allFiles.size} ${i18nManager.getString("dialog.files")}..."
        
        allFiles.forEachIndexed { index, file ->
            if (indicator.isCanceled) return todoComments
            
            indicator.fraction = index.toDouble() / allFiles.size
            indicator.text2 = i18nManager.getString("dialog.processing") + ": ${file.name}"
            
            ReadAction.run<RuntimeException> {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    val fileTodos = extractTodosFromFile(file, psiFile)
                    todoComments.addAll(fileTodos)
                }
            }
        }
        
        return todoComments
    }
    
    private fun extractTodosFromFile(virtualFile: VirtualFile, psiFile: com.intellij.psi.PsiFile): List<TodoItem> {
        val todoComments = mutableListOf<TodoItem>()
        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: return emptyList()
        
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
                                    fullCommentText = commentText.trim(),
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
    
    private fun isProcessableFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase()
        return extension in setOf(
            "kt", "java", "js", "ts", "py", "cpp", "c", "h", "hpp", "cs", "php",
            "rb", "go", "rs", "swift", "scala", "groovy", "xml", "html", "css",
            "scss", "less", "sql", "sh", "bat", "ps1", "yaml", "yml", "json",
            "properties", "gradle", "md", "txt"
        )
    }
}
