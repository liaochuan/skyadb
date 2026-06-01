package com.sky22333.skyadb.scrcpy

object ScrcpyConstants {
    const val ServerVersion = "4.0"
    const val ServerAssetPath = "scrcpy/scrcpy-server-v4.0"
    const val RemoteServerPath = "/data/local/tmp/skyadb-scrcpy-server-v4.0.jar"
    const val DefaultMaxSize = 1280
    const val DefaultMaxFps = 30
    const val DefaultVideoBitRate = 4_000_000
    const val ConnectRetryCount = 80
    const val ConnectRetryDelayMillis = 100L
}

data class ScrcpyOptions(
    val maxSize: Int = ScrcpyConstants.DefaultMaxSize,
    val maxFps: Int = ScrcpyConstants.DefaultMaxFps,
    val videoBitRate: Int = ScrcpyConstants.DefaultVideoBitRate,
)

data class ScrcpyDeviceInfo(
    val name: String,
    val codecId: Int,
)
