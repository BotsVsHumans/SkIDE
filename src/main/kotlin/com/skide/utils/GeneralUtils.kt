package com.skide.utils

import com.skide.Info
import com.skide.gui.GUIManager
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import java.awt.im.InputContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


fun unzip(zipFile: String, outputFolder: String) {

    val buffer = ByteArray(1024)
    try {

        val folder = File(outputFolder)
        if (!folder.exists()) folder.mkdir()
        val zis = ZipInputStream(FileInputStream(zipFile))
        var ze: ZipEntry? = zis.nextEntry
        while (ze != null) {

            val fileName = ze.name
            val newFile = File(outputFolder + File.separator + fileName)

            File(newFile.parent).mkdirs()

            val fos = FileOutputStream(newFile)

            var len: Int
            while (true) {
                len = zis.read(buffer)
                if (len <= 0) break
                fos.write(buffer, 0, len)
            }

            fos.close()
            ze = zis.nextEntry
        }

        zis.closeEntry()
        zis.close()


    } catch (ex: IOException) {
        ex.printStackTrace()
    }

}

fun KeyEvent.verifyKeyCombo(): Boolean {
    if (this.isShiftDown) return false
    val os = getOS()
    return when (os) {
        OperatingSystemType.WINDOWS, OperatingSystemType.LINUX -> this.isControlDown
        OperatingSystemType.MAC_OS -> this.isMetaDown && this.isShortcutDown
        else -> false
    }
}

fun extensionToLang(ex: String): String {
    return when (ex) {
        "sk" -> "skript"
        "json" -> "json"
        "yaml", "yml" -> "yaml"
        "java" -> "java"
        else -> "text/plain"
    }
}

fun adjustVersion(value: String): String {

    var str = value.replace("-dev", ".")
    var fails = 0
    while (true) {
        try {
            Integer.parseInt(str.replace(".", ""))
            break
        } catch (e: Exception) {
            str = str.substring(0, str.length - 1)
            fails++
        }
    }
    if (str.length == 3) str += ".0"
    return if (fails == 0) str else "$str$fails"
}

fun restart() {
    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val command = ArrayList<String>()
    if (getOS() == OperatingSystemType.WINDOWS) {
        command.add(File(File(File(".").canonicalPath), "SkIDE.exe").absolutePath)
    } else {
        command.add(javaBin)
        if (Info.prodMode) command.add("-Dskide.mode=prod")
        command.add("-jar")
        command.add(File(File(File(".").canonicalPath), "Installer.jar").absolutePath)
    }

    val builder = ProcessBuilder(command)

    Thread {
        builder.start()
    }.start()
    System.exit(0)
}

fun getLocale(): String {

    val context = InputContext.getInstance()
    return context.locale.toString()
}

fun Button.setIcon(name: String, replaceAble: Boolean = true) {
    var lnk = "/images/$name"
    if (replaceAble) lnk += if (GUIManager.settings.get("theme") == "Dark") "_white" else "_black"
    lnk += ".png"
    val image = Image(GUIManager::javaClass.javaClass.getResource(lnk).toExternalForm())
    this.text = ""
    this.style = "-fx-background-color: rgba(0,0,0,0.0);"
    this.setPrefSize(image.width, image.height)
    val view = ImageView(image)
    this.graphic = view
}

fun Button.setIcon(name: String, w: Double, h: Double, replaceAble: Boolean = true) {
    var lnk = "/images/$name"
    if (replaceAble) lnk += if (GUIManager.settings.get("theme") == "Dark") "_white" else "_black"
    lnk += ".png"
    val image = Image(GUIManager::javaClass.javaClass.getResource(lnk).toExternalForm())
    this.text = ""
    this.style = "-fx-background-color: rgba(0,0,0,0.0);"
    this.setPrefSize(image.width, image.height)
    val view = ImageView(image)
    view.fitWidth = w
    view.fitHeight = h
    this.graphic = view
}