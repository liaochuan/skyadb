package com.sky22333.skyadb.ui.mirror

import android.view.MotionEvent
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.scrcpy.ScrcpyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class MirrorUiState(
    val status: OperationStatus = OperationStatus.Idle,
    val deviceName: String = "屏幕镜像",
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
)

class MirrorViewModel(
    private val repository: ScrcpyRepository = AppServices.scrcpyRepository,
) : ViewModel() {
    private val state = MutableStateFlow(MirrorUiState())
    val uiState: StateFlow<MirrorUiState> = state.asStateFlow()

    private val controlScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start(surface: Surface) {
        if (started) return
        started = true
        state.value = state.value.copy(status = OperationStatus.Running("正在启动屏幕镜像"))
        viewModelScope.launch {
            when (
                val result = repository.start(
                    surface = surface,
                    onVideoSize = { width, height ->
                        state.value = state.value.copy(videoWidth = width, videoHeight = height)
                    },
                    onStreamError = { error ->
                        started = false
                        state.value = state.value.copy(
                            status = OperationStatus.Failed(
                                text = "屏幕镜像已断开",
                                suggestion = error.message ?: "请重新进入屏幕镜像。",
                            ),
                        )
                    },
                )
            ) {
                is AdbOperationResult.Success -> {
                    state.value = state.value.copy(
                        status = OperationStatus.Success("正在镜像"),
                        deviceName = result.data.name,
                    )
                }
                is AdbOperationResult.Failure -> {
                    started = false
                    state.value = state.value.copy(
                        status = OperationStatus.Failed(result.message, result.suggestion),
                    )
                }
            }
        }
    }

    fun sendTouch(event: MotionEvent, width: Int, height: Int) {
        val eventCopy = MotionEvent.obtain(event)
        controlScope.launch {
            try {
                repository.sendTouch(eventCopy, width, height)
            } finally {
                eventCopy.recycle()
            }
        }
    }

    fun sendKey(keyCode: Int) {
        controlScope.launch {
            repository.sendKey(keyCode)
        }
    }

    fun sendText(text: String) {
        controlScope.launch {
            repository.sendText(text)
        }
    }

    fun stop() {
        started = false
        state.value = MirrorUiState()
        controlScope.launch {
            repository.stop()
        }
    }

    override fun onCleared() {
        controlScope.cancel()
        runBlocking(Dispatchers.IO) {
            repository.stop()
        }
        super.onCleared()
    }
}
