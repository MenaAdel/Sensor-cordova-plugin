package com.t2.sensorreader.domain.datasource.storage

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

fun Context.writeToFile(data: String? ,name: String): String? {
    return try {
        val rootFolder = externalCacheDir
        val file = File(rootFolder ,name)
        val writer = FileWriter(file)
        writer.write(data)
        writer.close()
        Log.e("filePath", file.absolutePath)
        file.absolutePath
    } catch (e: IOException) {
        Log.e("Exception", "File write failed: $e")
        null
    }
}

fun Context.writeToFileOnDisk(data: String? ,name: String) {
    try {
        val rootFolder = getExternalFilesDir(null)
        val file = File(rootFolder ,name)
        val writer = FileWriter(file)
        writer.write(data)
        writer.close()
        Log.e("filePath", file.absolutePath)
    } catch (e: IOException) {
        Log.e("Exception", "File write failed: $e")

    }
}

