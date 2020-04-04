package ru.hse.spb.sharkova.printfile.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.writeChild
import org.gradle.tooling.GradleConnector
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


class PrintFileAction : AnAction() {
    private class PrintFileDialog(project: Project) : DialogWrapper(project) {
        init {
            init()
            title = "Print File"
        }

        override fun createCenterPanel(): JComponent? {
            val dialogPanel = JPanel(BorderLayout())

            val label = JLabel("PrintFileDialog")
            label.preferredSize = Dimension(400, 400)
            dialogPanel.add(label, BorderLayout.CENTER)

            return dialogPanel
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val currentProject: Project = e.project ?: return
        val file = FileChooser.chooseFile(FileChooserDescriptor(true, false, false, false, false, false), currentProject, null)
        if (file != null) {
            val filepath = file.path
            val vFiles = ProjectRootManager.getInstance(currentProject).contentSourceRoots
            val outputLines = mutableListOf<String>()
            if (vFiles.isNotEmpty()) {
                val sourceRoot = vFiles[0]
                ApplicationManager.getApplication().runWriteAction {
                    // create the necessary directory in a random source directory;
                    // it is very unlikely such a directory already exists
                    // directory is removed when execution is finished
                    val dir = sourceRoot.createChildDirectory(null, "additionalGradleFilesForPrintFile")
                    val buildFile = dir.writeChild("build.gradle",
                        """
                            plugins {
                                id 'ru.hse.spb.sharkova.printfile' version '0.0.1'
                            }
    
                            printfile {
                                enabled = true
                                filename = "$filepath"
                            }                        
                        """.trimIndent())
                    val settingsFile = dir.writeChild("settings.gradle",
                        """
                            pluginManagement {
                                repositories {
                                    gradlePluginPortal()
                                    mavenLocal()
                                }
                            }
                        
                        """.trimIndent())


                    GradleConnector.newConnector().useGradleVersion("5.1.1")
                        .forProjectDirectory(File(dir.path))
                        .connect().use { connection ->
                            val baos = ByteArrayOutputStream()
                            val utf8: String = StandardCharsets.UTF_8.name()
                            // this task will always exist because it's specified in build.gradle
                            PrintStream(baos, true, utf8).use { ps ->
                                connection.newBuild().forTasks("printfile").setStandardOutput(ps).run() }
                            val data = baos.toString(utf8)
                            val lines = data.split("\n")
                            val startIndex = lines.indexOf("> Task :printfile")
                            val endIndex = lines.indexOf("1 actionable task: 1 executed")
                            if (startIndex != -1 && endIndex != -1) {
                                outputLines.addAll(lines.subList(startIndex + 1, endIndex - 1))
                            } else {
                                outputLines.add("Gradle task invocation did not finish successfully.")
                            }
                        }

                    if (settingsFile.exists()) {
                        settingsFile.delete(null)
                    }
                    if (buildFile.exists()) {
                        buildFile.delete(null)
                    }
                    if (dir.exists()) {
                        dir.delete(null)
                    }
                }
            }

            Messages.showMessageDialog(currentProject, outputLines.joinToString("\n"), "File Content", Messages.getInformationIcon())
        }
    }
}