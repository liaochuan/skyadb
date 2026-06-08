package com.sky22333.skyadb.adb

import com.flyfishxu.kadb.Kadb

data class MirrorConnections(
    val control: Kadb,
    val video: Kadb,
) : AutoCloseable {
    override fun close() {
        runCatching { control.close() }
        runCatching { video.close() }
    }
}
