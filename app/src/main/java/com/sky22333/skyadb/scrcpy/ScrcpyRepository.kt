package com.sky22333.skyadb.scrcpy

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import com.sky22333.skyadb.adb.KadbManager
import com.sky22333.skyadb.diagnostics.DiagnosticLogger
import com.sky22333.skyadb.diagnostics.DiagnosticModule
import com.sky22333.skyadb.model.AdbOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScrcpyRepository(
    private val context: Context,
    private val kadbManager: KadbManager,
) {
    private var session: ScrcpySession? = null

    suspend fun start(
        surface: Surface,
        options: ScrcpyOptions = ScrcpyOptions(),
        onVideoSize: (Int, Int) -> Unit,
        onStreamError: (Throwable) -> Unit = {},
    ): AdbOperationResult<ScrcpyDeviceInfo> = withContext(Dispatchers.IO) {
        stop()
        val kadb = kadbManager.createStreamingClient()
            ?: return@withContext AdbOperationResult.Failure(
                message = "未连接设备",
                suggestion = "请先连接设备，再启动屏幕镜像。",
            )

        runCatching {
            ScrcpySession.start(
                context = context,
                kadb = kadb,
                surface = surface,
                options = options,
                onVideoSize = onVideoSize,
                onError = { error ->
                    DiagnosticLogger.record(
                        module = DiagnosticModule.Mirror,
                        operation = "视频流",
                        target = kadbManager.currentEndpoint(),
                        message = "屏幕镜像视频流异常",
                        suggestion = "请重新进入屏幕镜像；如果持续失败，请降低画质或确认设备支持当前编码。",
                        cause = error,
                    )
                    onStreamError(error)
                },
            ).also { session = it }
        }.fold(
            onSuccess = { AdbOperationResult.Success(it.deviceInfo) },
            onFailure = { error ->
                DiagnosticLogger.record(
                    module = DiagnosticModule.Mirror,
                    operation = "启动镜像",
                    target = kadbManager.currentEndpoint(),
                    message = "屏幕镜像启动失败",
                    suggestion = "请确认设备已授权 ADB、scrcpy-server-v4.0 已放入 assets，并查看详细错误。",
                    cause = error,
                )
                AdbOperationResult.Failure(
                    message = "屏幕镜像启动失败",
                    suggestion = error.message ?: "请查看设置里的诊断日志。",
                    cause = error,
                )
            },
        )
    }

    fun sendTouch(event: MotionEvent, surfaceWidth: Int, surfaceHeight: Int) {
        runCatching {
            session?.controlClient?.sendTouch(event, surfaceWidth, surfaceHeight)
        }.onFailure { error ->
            DiagnosticLogger.record(
                module = DiagnosticModule.Mirror,
                operation = "发送触摸",
                message = "远程触摸发送失败",
                suggestion = "镜像连接可能已断开，请重新进入屏幕镜像。",
                cause = error,
            )
        }
    }

    fun sendKey(keyCode: Int) {
        runCatching {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                session?.controlClient?.sendBackOrScreenOn()
            } else {
                session?.controlClient?.sendKey(keyCode)
            }
        }.onFailure { error ->
            DiagnosticLogger.record(
                module = DiagnosticModule.Mirror,
                operation = "发送按键",
                message = "远程按键发送失败",
                suggestion = "镜像连接可能已断开，请重新进入屏幕镜像。",
                cause = error,
            )
        }
    }

    fun sendText(text: String) {
        runCatching { session?.controlClient?.sendText(text) }
    }

    fun stop() {
        runCatching { session?.stop() }
            .onFailure { error ->
                DiagnosticLogger.record(
                    module = DiagnosticModule.Mirror,
                    operation = "停止镜像",
                    message = "释放屏幕镜像资源失败",
                    suggestion = "如果再次启动异常，请重新连接设备。",
                    cause = error,
                )
            }
        session = null
    }
}
