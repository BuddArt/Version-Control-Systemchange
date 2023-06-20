package svcs

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

val separator = File.separator
val path = System.getProperty("user.dir")

fun copyFile(src: String, dest: String) {
    val file = File(src)
    if (file.exists()) {
        val sourcePath = Paths.get(src)
        val targetPath = Paths.get(dest)
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
    }
}

fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.toHex()
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

fun commitFun(args: Array<String>) {
    val configFile = File("${path}${separator}vcs${separator}config.txt")
    val indexFile = File("${path}\\vcs\\index.txt")
    val indexPath = "${path}\\vcs\\index.txt"
    val hashPath = "${path}\\vcs\\hash.txt"
    if (!configFile.exists()) {
        configFile.createNewFile()
    }
    if (args.size == 1 && args[0] == "commit") println("Message was not passed.")
    else if (args.size >= 2 && args[0] == "commit") {
        val hashCommitFile = File("${path}\\vcs\\hash.txt")
        var isFirstCommit = false
        val commitsFolder = File("${path}\\vcs\\commits")
        if (!commitsFolder.exists()) {
            commitsFolder.mkdirs()
            isFirstCommit = true
        }
        val fromFile = File(path)
        if (hashCommitFile.exists()) hashCommitFile.writeText("")
        fromFile.walkTopDown().maxDepth(1).forEach {
            if ((it.isFile) && (it.extension == "txt")) {
                var indexFileName = it.name
                var textFileFrom = ""
                textFileFrom = it.readText()
                indexFile.forEachLine {
                    if (indexFileName == it) {
                        if (hashCommitFile.exists()) {
                            hashCommitFile.appendText("${indexFileName}\n${textFileFrom.toMD5()}\n")
                        } else {
                            hashCommitFile.createNewFile()
                            hashCommitFile.writeText("${indexFileName}\n${textFileFrom.toMD5()}\n")
                        }
                    }
                }
            }
        }
        val hashCommitFolderName = hashCommitFile.readText().toMD5()
        File("${path}\\vcs\\commits\\$hashCommitFolderName").mkdirs()

        if ((hashCommitFile.readText().toMD5() == indexFile.readText().toMD5()) && !isFirstCommit) {
            println("Nothing to commit.")
        } else {
            fromFile.walkTopDown().maxDepth(1).forEach {// create files in folder commits
                if ((it.isFile) && (it.extension == "txt")) {
                    val fileThat = File("${path}\\vcs\\commits\\$hashCommitFolderName\\${it.name}")
                    var indexFileName = it.name
                    var textFileFrom = ""
                    textFileFrom = it.readText()
                    indexFile.forEachLine {
                        if (indexFileName == it) {
                            if (fileThat.exists()) {
                                fileThat.writeText(textFileFrom)
                            } else {
                                fileThat.createNewFile()
                                fileThat.writeText(textFileFrom)
                            }
                        }
                    }
                }
            }
            val logFile = File("${path}\\vcs\\log.txt")
            val logFileCopy = File("${path}\\vcs\\log_copy.txt")
            if (logFile.exists()) {
                logFileCopy.writeText("${logFile.readText()}\n")
                logFile.writeText("commit ${hashCommitFolderName}\nAuthor: ${configFile.readText()}\n${if (args.size > 1) "${args[1]}\n" else ""}")
                logFile.appendText("${logFileCopy.readText()}\n")
            } else {
                logFile.createNewFile()
                logFile.writeText("commit ${hashCommitFolderName}\nAuthor: ${configFile.readText()}\n${if (args.size > 1) "${args[1]}\n" else ""}")
            }
            logFileCopy.delete()
            println("Changes are committed.")
            copyFile(hashPath, indexPath)
        }
    }
}

fun logFun(args: Array<String>) {
    val logFile = File("${path}\\vcs\\log.txt")
    if (args.isNotEmpty() && args[0] == "log") {
        if (logFile.exists()) {
            println("${logFile.readText()}")
        } else {
            println("No commits yet.")
        }
    }
}

fun configFun(args: Array<String>) {
    val configFile = File("${path}${separator}vcs${separator}config.txt")
    if(!configFile.exists()) {
        configFile.createNewFile()
    }

    if (args[0] == "config" && 1 in args.indices) {
        configFile.writeText(args[1])
        println("The username is ${args[1]}.")
    } else if (args[0] == "config" && 1 !in args.indices) {
        if (configFile.readText().isBlank()) {
            println("Please, tell me who you are.")
        } else println("The username is ${configFile.readText()}.")
    }
}

fun addFun(args: Array<String>) {
    val indexFile = File("${path}${separator}vcs${separator}index.txt")
    if(!indexFile.exists()) {
        indexFile.createNewFile()
        indexFile.writeText("Tracked files:")
    }

    if (args[0] == "add" && 1 in args.indices && File("${path}${separator}${args[1]}").exists()) {
        indexFile.appendText("\n${args[1]}")
        println("The file '${args[1]}' is tracked.")
    } else if (args[0] == "add" && 1 in args.indices && !File("${path}${separator}${args[1]}").exists()) {
        println("Can't find '${args[1]}'.")
    } else if (args[0] == "add" && 1 !in args.indices) {
        val lines = indexFile.readLines()
        if(lines.size > 1) println(indexFile.readText())
        else if (lines.size == 1) println("Add a file to the index.")
    }
}

fun checkoutFun(args: Array<String>) {
    if (args.size == 1 && args[0] == "checkout") {
        println("Commit id was not passed.")
    } else if (args.size >= 2 && args[0] == "checkout") {
        val dirPath = "${path}"
        var isExistsCommitsID = false
        val folderOfCommits = File("${path}\\vcs\\commits")
        var choosingCommitFile = File("")
        if (args[1] != "") {
            folderOfCommits.walkTopDown().maxDepth(1).forEach {
                if (args[1] == it.name) {
                    choosingCommitFile = it
                    isExistsCommitsID = true
                }
            }
            if (isExistsCommitsID) {
                choosingCommitFile.walkTopDown().maxDepth(1).forEach {
                    if ((it.isFile) && (it.extension == "txt")) {
                        it.copyTo(File("${path}\\${it.name}"), overwrite = true)
                    }
                }
                println("Switched to commit ${choosingCommitFile.name}.")
            } else println("Commit does not exist.")
        } else println("Commit id was not passed.")
    }
}


fun main(args: Array<String>) {
    val vcsDir = File("${path}${separator}vcs")
    if(!vcsDir.exists()) vcsDir.mkdir()

    val mapCommands = mapOf<String, String>(
        "config" to "Please, tell me who you are.",
        "add" to "Add a file to the index.",
        "log" to "Show commit logs.",
        "commit" to "Save changes.",
        "checkout" to "Restore a file."
    )

    if (args.firstOrNull() == null || args.firstOrNull() == "--help") {
        println(
            """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file.""".trimIndent()
        )
    } else if (mapCommands.containsKey(args.first())) {
        when(args[0]) {
            "config" -> configFun(args)
            "add" -> addFun(args)
            "commit" -> commitFun(args)
            "log" -> logFun(args)
            "checkout" -> checkoutFun(args)
        }
    } else {
        println("\'${args.first()}\' is not a SVCS command.")
    }
}