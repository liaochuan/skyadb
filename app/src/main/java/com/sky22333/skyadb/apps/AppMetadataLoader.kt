package com.sky22333.skyadb.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppMetadata(
    val label: String,
    val iconPath: String,
)

class AppMetadataLoader(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val cacheDir = File(appContext.cacheDir, "remote-app-metadata")

    fun tempApkFile(packageName: String, sourcePath: String): File {
        val safePackage = packageName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        return File(cacheDir, "$safePackage-${sourcePath.hashCode()}.tmp.apk")
    }

    suspend fun cached(packageName: String, sourcePath: String): AppMetadata? = withContext(Dispatchers.IO) {
        val iconFile = iconFile(packageName, sourcePath)
        val labelFile = labelFile(packageName, sourcePath)
        val label = labelFile.takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() }
        if (label != null && iconFile.isFile) {
            AppMetadata(label = label, iconPath = iconFile.absolutePath)
        } else {
            null
        }
    }

    suspend fun load(packageName: String, sourcePath: String, apkFile: File): AppMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val packageInfo = archivePackageInfo(apkFile) ?: return@withContext null
            val applicationInfo = packageInfo.applicationInfo ?: return@withContext null
            applicationInfo.sourceDir = apkFile.absolutePath
            applicationInfo.publicSourceDir = apkFile.absolutePath

            val label = applicationInfo.loadLabel(packageManager).toString().ifBlank { packageName }
            val iconFile = iconFile(packageName, sourcePath)
            if (!iconFile.isFile) {
                applicationInfo.loadIcon(packageManager).toBitmap().saveAsPng(iconFile)
            }
            labelFile(packageName, sourcePath).writeText(label)
            cleanupCache()
            AppMetadata(label = label, iconPath = iconFile.absolutePath)
        }.getOrNull()
    }

    private fun archivePackageInfo(apkFile: File) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
    }

    private fun iconFile(packageName: String, sourcePath: String): File {
        return File(cacheDir, "${cacheKey(packageName, sourcePath)}.png")
    }

    private fun labelFile(packageName: String, sourcePath: String): File {
        return File(cacheDir, "${cacheKey(packageName, sourcePath)}.label")
    }

    private fun cacheKey(packageName: String, sourcePath: String): String {
        return "${packageName.replace('.', '_')}-${sourcePath.hashCode()}"
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return Bitmap.createScaledBitmap(bitmap, DefaultIconSize, DefaultIconSize, true)
        }
        return Bitmap.createBitmap(DefaultIconSize, DefaultIconSize, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }

    private fun Bitmap.saveAsPng(file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun cleanupCache() {
        cacheDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MaxCacheFiles)
            ?.forEach { file -> runCatching { file.delete() } }
    }

    private companion object {
        const val DefaultIconSize = 96
        const val MaxCacheFiles = 160
    }
}
