package com.sky22333.skyadb.ui.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteKeyTest {
    @Test
    fun keyCodes_matchAndroidInputKeyEvents() {
        assertEquals("KEYCODE_BACK", RemoteKey.Back.keyCode)
        assertEquals("KEYCODE_DPAD_CENTER", RemoteKey.Center.keyCode)
        assertEquals("KEYCODE_MEDIA_PLAY_PAUSE", RemoteKey.PlayPause.keyCode)
    }
}
