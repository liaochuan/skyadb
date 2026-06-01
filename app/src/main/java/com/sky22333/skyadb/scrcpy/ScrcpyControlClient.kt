package com.sky22333.skyadb.scrcpy

import android.view.KeyEvent
import android.view.MotionEvent
import com.flyfishxu.kadb.stream.AdbStream

class ScrcpyControlClient(
    private val stream: AdbStream,
) {
    private val lock = Any()

    @Volatile
    var videoWidth: Int = 0
        private set

    @Volatile
    var videoHeight: Int = 0
        private set

    fun updateVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
    }

    fun sendTouch(event: MotionEvent, surfaceWidth: Int, surfaceHeight: Int) {
        val width = videoWidth
        val height = videoHeight
        val action = ScrcpyProtocol.motionAction(event.actionMasked) ?: return
        val pointerIndex = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val point = MirrorCoordinateMapper.map(
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            videoWidth = width,
            videoHeight = height,
        ) ?: return

        val pointerId = if (event.getPointerId(pointerIndex) == 0) {
            ScrcpyProtocol.PointerMouse
        } else {
            event.getPointerId(pointerIndex).toLong()
        }
        send(
            ScrcpyProtocol.touch(
                action = action,
                pointerId = pointerId,
                x = point.x,
                y = point.y,
                screenWidth = point.screenWidth,
                screenHeight = point.screenHeight,
                pressure = event.getPressure(pointerIndex),
                actionButton = event.actionButton,
                buttons = event.buttonState,
            ),
        )
    }

    fun sendKey(keyCode: Int) {
        send(ScrcpyProtocol.keyEvent(KeyEvent.ACTION_DOWN, keyCode))
        send(ScrcpyProtocol.keyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun sendText(text: String) {
        if (text.isNotEmpty()) send(ScrcpyProtocol.text(text))
    }

    fun sendBackOrScreenOn() {
        send(ScrcpyProtocol.backOrScreenOn())
        send(ScrcpyProtocol.backOrScreenOn(KeyEvent.ACTION_UP))
    }

    private fun send(bytes: ByteArray) {
        synchronized(lock) {
            stream.sink.write(bytes)
            stream.sink.flush()
        }
    }
}
