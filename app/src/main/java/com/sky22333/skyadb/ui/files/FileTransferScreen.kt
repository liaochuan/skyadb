package com.sky22333.skyadb.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens

@Composable
fun FileTransferScreen(
    onBackClick: () -> Unit,
    viewModel: FileTransferViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onLocalFileSelected(uri)
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        viewModel.pullToUri(context, uri)
    }

    FileTransferContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onModeChanged = viewModel::onModeChanged,
        onRemotePathChanged = viewModel::onRemotePathChanged,
        onPickFileClick = { openFile.launch(arrayOf("*/*")) },
        onPushClick = viewModel::pushSelectedFile,
        onPullClick = {
            val fileName = uiState.remotePath.substringAfterLast('/').ifBlank { "pulled-file" }
            createFile.launch(fileName)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTransferContent(
    uiState: FileTransferUiState,
    onBackClick: () -> Unit,
    onModeChanged: (FileTransferMode) -> Unit,
    onRemotePathChanged: (String) -> Unit,
    onPickFileClick: () -> Unit,
    onPushClick: () -> Unit,
    onPullClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("文件传输")
                    Text(
                        "发送本机文件，或从设备拉取文件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionGap),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.CardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionHeader(title = "传输任务", description = "设备路径需要以 / 开头")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        FileTransferMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.mode == mode,
                                onClick = { onModeChanged(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, FileTransferMode.entries.size),
                            ) {
                                Text(mode.label)
                            }
                        }
                    }
                    if (uiState.mode == FileTransferMode.Push) {
                        Text(
                            text = uiState.selectedLocalName ?: "尚未选择本地文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onClick = onPickFileClick, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("选择本地文件")
                        }
                    }
                    OutlinedTextField(
                        value = uiState.remotePath,
                        onValueChange = onRemotePathChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (uiState.mode == FileTransferMode.Push) "设备目标路径" else "设备文件路径") },
                        singleLine = true,
                        isError = uiState.remotePathError != null,
                        supportingText = {
                            Text(uiState.remotePathError ?: "例如 /sdcard/Download/file.txt")
                        },
                    )
                    FileTransferStatus(status = uiState.operationStatus)
                    Button(
                        onClick = if (uiState.mode == FileTransferMode.Push) onPushClick else onPullClick,
                        enabled = uiState.actionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = if (uiState.mode == FileTransferMode.Push) {
                                Icons.Outlined.FileUpload
                            } else {
                                Icons.Outlined.FileDownload
                            },
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uiState.mode == FileTransferMode.Push) "发送文件" else "拉取文件")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTransferStatus(status: OperationStatus) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(status.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is OperationStatus.Success -> Text(status.text, color = MaterialTheme.colorScheme.primary)
        is OperationStatus.Failed -> Text(
            text = "${status.text}：${status.suggestion}",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(name = "文件传输 - 发送", showBackground = true, widthDp = 390)
@Composable
private fun FileTransferPushPreview() {
    AdbManagerTheme(dynamicColor = false) {
        FileTransferContent(
            uiState = FileTransferUiState(
                selectedLocalName = "config.json",
                actionEnabled = true,
            ),
            onBackClick = {},
            onModeChanged = {},
            onRemotePathChanged = {},
            onPickFileClick = {},
            onPushClick = {},
            onPullClick = {},
        )
    }
}

@Preview(name = "文件传输 - 拉取", showBackground = true, widthDp = 390)
@Composable
private fun FileTransferPullPreview() {
    AdbManagerTheme(dynamicColor = false) {
        FileTransferContent(
            uiState = FileTransferUiState(
                mode = FileTransferMode.Pull,
                remotePath = "/sdcard/Download/log.txt",
                actionEnabled = true,
            ),
            onBackClick = {},
            onModeChanged = {},
            onRemotePathChanged = {},
            onPickFileClick = {},
            onPushClick = {},
            onPullClick = {},
        )
    }
}
