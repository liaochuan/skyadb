package com.sky22333.skyadb.scrcpy

import android.content.Context
import com.flyfishxu.kadb.Kadb
import okio.source

class ScrcpyServerManager(
    private val context: Context,
) {
    fun pushServer(kadb: Kadb) {
        context.assets.open(ScrcpyConstants.ServerAssetPath).use { input ->
            kadb.push(
                input.source(),
                ScrcpyConstants.RemoteServerPath,
                420,
                System.currentTimeMillis(),
            )
        }
    }

    fun buildStartCommand(scid: UInt, options: ScrcpyOptions): String {
        val socketId = scid.toString(16).padStart(8, '0')
        return listOf(
            "CLASSPATH=${ScrcpyConstants.RemoteServerPath}",
            "app_process",
            "/",
            "com.genymobile.scrcpy.Server",
            ScrcpyConstants.ServerVersion,
            "scid=$socketId",
            "log_level=info",
            "video=true",
            "audio=false",
            "control=true",
            "tunnel_forward=true",
            "max_size=${options.maxSize}",
            "max_fps=${options.maxFps}",
            "video_bit_rate=${options.videoBitRate}",
        ).joinToString(" ")
    }
}
