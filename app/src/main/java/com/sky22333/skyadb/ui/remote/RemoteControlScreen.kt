package com.sky22333.skyadb.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.sky22333.skyadb.ui.components.AppTopBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens

@Composable
fun RemoteControlScreen(
    bottomPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    viewModel: RemoteControlViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    RemoteControlContent(
        bottomPadding = bottomPadding,
        uiState = uiState,
        onBackClick = onBackClick,
        onKeyClick = viewModel::sendKey,
    )
}

@Composable
private fun RemoteControlContent(
    bottomPadding: Dp = 0.dp,
    uiState: RemoteControlUiState,
    onBackClick: () -> Unit,
    onKeyClick: (RemoteKey) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("虚拟遥控器")
                    Text(
                        "发送物理按键到目标设备",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.ScreenPadding,
                top = AppDimens.ScreenPadding,
                end = AppDimens.ScreenPadding,
                bottom = AppDimens.ScreenPadding + bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionGap),
        ) {
            item { RemoteStatus(status = uiState.status) }
            item {
                SectionHeader(title = "方向键", description = "适合电视、盒子和无触控设备")
            }
            item { DpadCard(onKeyClick = onKeyClick) }
            item { SectionHeader(title = "常用按键") }
            item {
                KeyGrid(
                    keys = listOf(
                        KeyAction(RemoteKey.Back, Icons.AutoMirrored.Outlined.ArrowBack),
                        KeyAction(RemoteKey.Home, Icons.Outlined.Home),
                        KeyAction(RemoteKey.Menu, Icons.Outlined.PlayArrow),
                        KeyAction(RemoteKey.Power, Icons.Outlined.PowerSettingsNew),
                    ),
                    onKeyClick = onKeyClick,
                )
            }
            item { SectionHeader(title = "音量与媒体") }
            item {
                KeyGrid(
                    keys = listOf(
                        KeyAction(RemoteKey.VolumeDown, Icons.Outlined.VolumeDown),
                        KeyAction(RemoteKey.VolumeUp, Icons.Outlined.VolumeUp),
                        KeyAction(RemoteKey.Mute, Icons.Outlined.VolumeOff),
                        KeyAction(RemoteKey.PlayPause, Icons.Outlined.PlayArrow),
                        KeyAction(RemoteKey.Previous, Icons.AutoMirrored.Outlined.KeyboardArrowLeft),
                        KeyAction(RemoteKey.Next, Icons.AutoMirrored.Outlined.KeyboardArrowRight),
                    ),
                    onKeyClick = onKeyClick,
                )
            }
            item { SectionHeader(title = "电源") }
            item {
                KeyGrid(
                    keys = listOf(
                        KeyAction(RemoteKey.Wakeup, Icons.Outlined.PowerSettingsNew),
                        KeyAction(RemoteKey.Sleep, Icons.Outlined.PowerSettingsNew),
                    ),
                    onKeyClick = onKeyClick,
                )
            }
        }
    }
}

@Composable
private fun DpadCard(onKeyClick: (RemoteKey) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RemoteIconButton(RemoteKey.Up, Icons.Outlined.KeyboardArrowUp, onKeyClick)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteIconButton(RemoteKey.Left, Icons.AutoMirrored.Outlined.KeyboardArrowLeft, onKeyClick)
                Button(onClick = { onKeyClick(RemoteKey.Center) }, modifier = Modifier.size(76.dp)) {
                    Text(RemoteKey.Center.label)
                }
                RemoteIconButton(RemoteKey.Right, Icons.AutoMirrored.Outlined.KeyboardArrowRight, onKeyClick)
            }
            RemoteIconButton(RemoteKey.Down, Icons.Outlined.KeyboardArrowDown, onKeyClick)
        }
    }
}

@Composable
private fun KeyGrid(keys: List<KeyAction>, onKeyClick: (RemoteKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { action ->
                    OutlinedButton(
                        onClick = { onKeyClick(action.key) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(action.key.label)
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RemoteIconButton(key: RemoteKey, icon: ImageVector, onKeyClick: (RemoteKey) -> Unit) {
    OutlinedButton(onClick = { onKeyClick(key) }, modifier = Modifier.size(76.dp)) {
        Icon(icon, contentDescription = key.label)
    }
}

@Composable
private fun RemoteStatus(status: OperationStatus) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> Text(status.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        is OperationStatus.Success -> Text(status.text, color = MaterialTheme.colorScheme.primary)
        is OperationStatus.Failed -> Text("${status.text}：${status.suggestion}", color = MaterialTheme.colorScheme.error)
    }
}

private data class KeyAction(val key: RemoteKey, val icon: ImageVector)

@Preview(name = "虚拟遥控器", showBackground = true, widthDp = 390)
@Composable
private fun RemoteControlContentPreview() {
    AdbManagerTheme(dynamicColor = false) {
        RemoteControlContent(
            uiState = RemoteControlUiState(),
            onBackClick = {},
            onKeyClick = {},
        )
    }
}
