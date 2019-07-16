package com.skide.core.management

import com.skide.CoreManager
import com.skide.include.Server
import com.skide.utils.OperatingSystemType
import com.skide.utils.getOS
import com.skide.utils.writeFile
import javafx.application.Platform
import javafx.scene.control.TextArea
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RunningServerManager(val server: Server, val coreManager: CoreManager) {

    lateinit var process: Process
    private lateinit var thread: Thread
    lateinit var area: TextArea

    fun start(readyCallback: (RunningServerManager) -> Unit) {
        if (server.running) return

        area = TextArea()
        thread = Thread {

            area.isEditable = false
            val builder = ProcessBuilder()
            val javaPath = {
                if(coreManager.configManager.get("jre_path") != null) {
                    coreManager.configManager.get("jre_path").toString()
                } else {
                    if(getOS() === OperatingSystemType.MAC_OS) {
                        "java"
                    } else{
                        File(File(System.getProperty("java.home"), "bin"), "java").absolutePath
                    }
                }

            }.invoke()
            val serverFile = File(server.configuration.folder, "server.jar").absolutePath
            val list = arrayListOf(javaPath)
            if (server.configuration.jvmArgs.isNotEmpty()) {
                server.configuration.jvmArgs.split(" ").forEach {
                    list += it
                }
            }
           if(!list.contains("-jar")) list.add("-jar")
            list.add(serverFile)
            if (server.configuration.startArgs.isNotEmpty()) {
                server.configuration.startArgs.split(" ").forEach {
                    list += it
                }
            }
            builder.command(list)
            builder.directory(server.configuration.folder)
            process = builder.start()
            server.running = true
            area.appendText("> ${list.joinToString(" ")}\nIn directory(WD): ${server.configuration.folder}\n\n")
            Thread {
                var msg: String?
                val reader = BufferedReader(InputStreamReader(process.inputStream, "UTF-8"))
                while (true) {
                    msg = reader.readLine()
                    if (!process.isAlive || msg == null) break
                    if (msg != "") {
                        if (msg.contains("]: Done")) {
                            readyCallback(this)
                        }
                        Thread {
                            Platform.runLater {
                                if (msg != null && msg!!.length > 1) area.appendText("$msg\n")
                                msg = ""
                            }
                        }.start()
                    }
                }
                server.running = false
                coreManager.serverManager.running.remove(server)
                Platform.runLater {
                    area.appendText("\n\nSERVER STOPPED")
                }
            }.start()
            Thread {
                var msg: String?
                val reader = BufferedReader(InputStreamReader(process.errorStream, "UTF-8"))
                while (true) {
                    msg = reader.readLine()
                    if (!process.isAlive || msg == null) break
                    if (msg != "") {
                        Thread {
                            Platform.runLater {
                                if (msg != null && msg!!.length > 1) area.appendText("ERR: $msg\n")
                                msg = ""
                            }
                        }.start()
                    }
                }
            }.start()
        }
        thread.name = "Server Thread for ${server.configuration.name}"
        thread.start()
    }

    fun cleanFiles() {
        val f = File(File(File(server.configuration.folder, "plugins"), "Skript"), "scripts")

        if (f.exists()) {
            f.listFiles().forEach {
                if (it.name.endsWith(".sk")) it.delete()
            }
        }
    }

    fun setSkriptFile(name: String, content: String) {
        val file = File(File(File(File(server.configuration.folder, "plugins"), "Skript"), "scripts"), name)
        if (!file.parentFile.exists()) return
        writeFile(content.toByteArray(), file, false, true)

        sendCommand("sk reload $name")
    }

    fun sendCommand(msg: String) {
        if (!server.running) return

        try {
            process.outputStream.write("$msg\n".toByteArray())
            process.outputStream.flush()
        } catch (e: Exception) {
            server.running = false
            coreManager.serverManager.running.remove(server)
            Platform.runLater {
                area.appendText("\n\nSERVER STOPPED")
            }
        }
    }

    fun kill() {
        if (!server.running) return

        process.destroyForcibly()
        server.running = false
        coreManager.serverManager.running.remove(server)

    }

    fun graceFulShutdown(callback: (Boolean) -> Unit) {
        if (!server.running) return
        sendCommand("stop")
        Thread {
            var counter = 0

            while (true) {
                Thread.sleep(250)
                counter++
                if (!process.isAlive) {
                    server.running = false
                    callback(true)
                    break
                }
                if (counter > 250) {
                    kill()
                    callback(false)
                    break
                }
            }
            coreManager.serverManager.running.remove(server)

        }.start()
    }

}