package com.sky22333.skyadb.scrcpy

import android.view.KeyEvent
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object ScrcpyProtocol {
    const val CodecH264 = 0x68323634
    const val CodecH265 = 0x68323635
    const val CodecAv1 = 0x00617631

    const val DeviceNameLength = 64
    const val PacketHeaderLength = 12
    const val PacketFlagSession = 1L shl 63
    const val PacketFlagConfig = 1L shl 62
    const val PacketFlagKeyFrame = 1L shl 61
    const val PacketPtsMask = (1L shl 61) - 1
    const val PointerMouse = -1L

    private const val TypeInjectKeycode = 0
    private const val TypeInjectText = 1
    private const val TypeInjectTouchEvent = 2
    private const val TypeInjectScrollEvent = 3
    private const val TypeBackOrScreenOn = 4

    fun keyEvent(action: Int, keyCode: Int, repeat: Int = 0, metaState: Int = 0): ByteArray {
        return ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN)
            .put(TypeInjectKeycode.toByte())
            .put(action.toByte())
            .putInt(keyCode)
            .putInt(repeat)
            .putInt(metaState)
            .array()
    }

    fun text(text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(5 + bytes.size).order(ByteOrder.BIG_ENDIAN)
            .put(TypeInjectText.toByte())
            .putInt(bytes.size)
            .put(bytes)
            .array()
    }

    fun touch(
        action: Int,
        pointerId: Long,
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        pressure: Float,
        actionButton: Int = 0,
        buttons: Int = 0,
    ): ByteArray {
        return ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
            .put(TypeInjectTouchEvent.toByte())
            .put(action.toByte())
            .putLong(pointerId)
            .putInt(x)
            .putInt(y)
            .putShort(screenWidth.toShort())
            .putShort(screenHeight.toShort())
            .putShort(unsignedFixedPoint16(pressure))
            .putInt(actionButton)
            .putInt(buttons)
            .array()
    }

    fun scroll(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        horizontal: Float,
        vertical: Float,
        buttons: Int = 0,
    ): ByteArray {
        return ByteBuffer.allocate(21).order(ByteOrder.BIG_ENDIAN)
            .put(TypeInjectScrollEvent.toByte())
            .putInt(x)
            .putInt(y)
            .putShort(screenWidth.toShort())
            .putShort(screenHeight.toShort())
            .putShort(signedFixedPoint16(horizontal / 16f))
            .putShort(signedFixedPoint16(vertical / 16f))
            .putInt(buttons)
            .array()
    }

    fun backOrScreenOn(action: Int = KeyEvent.ACTION_DOWN): ByteArray {
        return byteArrayOf(TypeBackOrScreenOn.toByte(), action.toByte())
    }

    fun motionAction(action: Int): Int? = when (action) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 1
        MotionEvent.ACTION_MOVE -> 2
        else -> null
    }

    private fun unsignedFixedPoint16(value: Float): Short {
        val clamped = value.coerceIn(0f, 1f)
        return if (clamped >= 1f) {
            0xffff.toShort()
        } else {
            (clamped * 65536f).roundToInt().coerceIn(0, 0xfffe).toShort()
        }
    }

    private fun signedFixedPoint16(value: Float): Short {
        val clamped = value.coerceIn(-1f, 1f)
        return when {
            clamped >= 1f -> 0x7fff.toShort()
            clamped <= -1f -> (-0x8000).toShort()
            else -> (clamped * 32768f).roundToInt().coerceIn(-0x8000, 0x7ffe).toShort()
        }
    }
}
