package com.sky22333.skyadb.ui.files

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.files.LocalFileManager
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.repository.AdbRepository
import com.sky22333.skyadb.validation.DevicePathValidator
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FileTransferMode(val label: String) {
    Push("发送到设备"),
    Pull("从设备拉取"),
}

data class FileTransferUiState(
    val mode: FileTransferMode = FileTransferMode.Push,
    val selectedLocalName: String? = null,
    val remotePath: String = "/sdcard/Download/",
    val remotePathError: String? = null,
    val actionEnabled: Boolean = false,
    val operationStatus: OperationStatus = OperationStatus.Idle,
)

class FileTransferViewModel(
    private val fileManager: LocalFileManager = AppServices.localFileManager,
    private val adbRepository: AdbRepository = AppServices.adbRepository,
) : ViewModel() {
    private val state = MutableStateFlow(FileTransferUiState())
    val uiState: StateFlow<FileTransferUiState> = state.asStateFlow()

    private var selectedLocalUri: Uri? = null

    fun onModeChanged(mode: FileTransferMode) {
        state.value = state.value.copy(
            mode = mode,
            actionEnabled = canExecute(mode, state.value.remotePath, selectedLocalUri),
            operationStatus = OperationStatus.Idle,
        )
    }

    fun onLocalFileSelected(uri: Uri?) {
        if (uri == null) return
        selectedLocalUri = uri
        state.value = state.value.copy(
            selectedLocalName = fileManager.displayName(uri),
            actionEnabled = canExecute(state.value.mode, state.value.remotePath, uri),
            operationStatus = OperationStatus.Idle,
        )
    }

    fun onRemotePathChanged(value: String) {
        val trimmed = value.trim()
        val error = remotePathError(trimmed)
        state.value = state.value.copy(
            remotePath = trimmed,
            remotePathError = error,
            actionEnabled = canExecute(state.value.mode, trimmed, selectedLocalUri) && error == null,
            operationStatus = OperationStatus.Idle,
        )
    }

    fun pushSelectedFile() {
        val uri = selectedLocalUri
        val current = state.value
        if (uri == null || current.remotePathError != null || current.remotePath.isBlank()) {
            state.value = current.copy(
                actionEnabled = false,
                operationStatus = OperationStatus.Failed("无法发送文件", "请先选择本地文件，并填写设备目标路径。"),
            )
            return
        }

        state.value = current.copy(actionEnabled = false, operationStatus = OperationStatus.Running("正在准备本地文件"))
        viewModelScope.launch {
            runCatching {
                val localFile = fileManager.copyToCache(uri)
                val remotePath = buildRemotePath(current.remotePath, localFile.name)
                localFile to remotePath
            }.fold(
                onSuccess = { (localFile, remotePath) ->
                    state.value = state.value.copy(operationStatus = OperationStatus.Running("正在发送到 $remotePath"))
                    when (val result = adbRepository.push(localFile, remotePath)) {
                        is AdbOperationResult.Success -> {
                            state.value = state.value.copy(
                                actionEnabled = true,
                                operationStatus = OperationStatus.Success("文件已发送到 $remotePath"),
                            )
                        }
                        is AdbOperationResult.Failure -> {
                            state.value = state.value.copy(
                                actionEnabled = true,
                                operationStatus = OperationStatus.Failed(result.message, result.suggestion),
                            )
                        }
                    }
                },
                onFailure = { error ->
                    state.value = state.value.copy(
                        actionEnabled = true,
                        operationStatus = OperationStatus.Failed(
                            text = "读取本地文件失败",
                            suggestion = error.message ?: "请确认文件存在，并允许 App 读取该文件。",
                        ),
                    )
                },
            )
        }
    }

    fun pullToUri(context: Context, destinationUri: Uri?) {
        val current = state.value
        if (destinationUri == null || current.remotePathError != null || current.remotePath.isBlank()) {
            state.value = current.copy(
                actionEnabled = false,
                operationStatus = OperationStatus.Failed("无法拉取文件", "请填写设备文件路径，并选择保存位置。"),
            )
            return
        }

        state.value = current.copy(actionEnabled = false, operationStatus = OperationStatus.Running("正在从设备拉取文件"))
        viewModelScope.launch {
            val tempFile = File(context.cacheDir, "pull/${current.remotePath.substringAfterLast('/').ifBlank { "pulled-file" }}")
            tempFile.parentFile?.mkdirs()
            when (val result = adbRepository.pull(current.remotePath, tempFile)) {
                is AdbOperationResult.Success -> {
                    runCatching {
                        context.contentResolver.openOutputStream(destinationUri).use { output ->
                            requireNotNull(output) { "无法打开保存位置" }
                            tempFile.inputStream().use { input -> input.copyTo(output) }
                        }
                    }.fold(
                        onSuccess = {
                            state.value = state.value.copy(
                                actionEnabled = true,
                                operationStatus = OperationStatus.Success("文件已保存到选择的位置"),
                            )
                        },
                        onFailure = { error ->
                            state.value = state.value.copy(
                                actionEnabled = true,
                                operationStatus = OperationStatus.Failed(
                                    text = "保存文件失败",
                                    suggestion = error.message ?: "请确认保存位置可写。",
                                ),
                            )
                        },
                    )
                }
                is AdbOperationResult.Failure -> {
                    state.value = state.value.copy(
                        actionEnabled = true,
                        operationStatus = OperationStatus.Failed(result.message, result.suggestion),
                    )
                }
            }
        }
    }

    private fun canExecute(mode: FileTransferMode, remotePath: String, localUri: Uri?): Boolean {
        return when (mode) {
            FileTransferMode.Push -> localUri != null && remotePath.isNotBlank() && remotePathError(remotePath) == null
            FileTransferMode.Pull -> remotePath.isNotBlank() && remotePathError(remotePath) == null
        }
    }

    private fun remotePathError(value: String): String? {
        return DevicePathValidator.pathError(value)
    }

    private fun buildRemotePath(input: String, fileName: String): String {
        return if (input.endsWith("/")) input + fileName else input
    }
}
