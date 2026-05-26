package com.sky22333.skyadb.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

class LocalFileManager(
    private val context: Context,
) {
    fun displayName(uri: Uri): String {
        val fromCursor = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }

        return fromCursor
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "selected-${System.currentTimeMillis()}"
    }

    fun copyToCache(uri: Uri, preferredName: String = displayName(uri)): File {
        val safeName = preferredName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val target = File(context.cacheDir, "picked/$safeName")
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取选择的文件" }
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return target
    }
}
