# printFileIdea
An IntelliJ IDEA plugin which adds an action that launches a custom Gradle plugin to print the content of a selected file with specified lines skipped.

How it works: there's a `Print File Action` option added as the last option in the Tools menu. Select a file in the file chooser dialog window, and wait for a pop-up window with file content to appear.

Upon file choice a Gradle `printfile` plugin task is executed; the result printed out in the pop-up window is a part of the Gradle plugin output.

In order for the Gradle plugin to work, it needs to be published to Maven local repository (see [GitHub](https://github.com/sharkovadarya/print_file_gradle) page for the Gradle plugin). 
 
