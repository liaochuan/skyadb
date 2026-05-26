package com.sky22333.skyadb.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.discovery.AdbProbeState
import com.sky22333.skyadb.discovery.AdbScanResult
import com.sky22333.skyadb.discovery.LocalNetwork
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.ui.components.EmptyState
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens

@Composable
fun DeviceDiscoveryScreen(
    onBackClick: () -> Unit,
    onUseEndpoint: (String, Int) -> Unit,
    viewModel: DeviceDiscoveryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    DeviceDiscoveryContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefreshNetworkClick = viewModel::refreshNetwork,
        onStartScanClick = viewModel::startScan,
        onStopScanClick = viewModel::stopScan,
        onUseEndpoint = onUseEndpoint,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDiscoveryContent(
    uiState: DeviceDiscoveryUiState,
    onBackClick: () -> Unit,
    onRefreshNetworkClick: () -> Unit,
    onStartScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onUseEndpoint: (String, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("局域网扫描")
                    Text(
                        text = scanRangeSummary(uiState.networks),
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
            actions = {
                IconButton(onClick = onRefreshNetworkClick, enabled = !uiState.scanning) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新网络")
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionGap),
        ) {
            item {
                ScanControlCard(
                    uiState = uiState,
                    onStartScanClick = onStartScanClick,
                    onStopScanClick = onStopScanClick,
                )
            }
            item {
                SectionHeader(
                    title = "发现结果",
                    description = "显示 ADB 握手确认或目标端口开放的设备",
                )
            }
            if (uiState.results.isEmpty()) {
                item {
                    EmptyState(
                        title = if (uiState.scanning) "正在查找设备" else "暂无发现",
                        message = if (uiState.scanning) {
                            "发现可响应的 ADB 设备后会立即显示。"
                        } else {
                            "确认目标设备已开启无线调试，并与本机处于同一局域网。"
                        },
                    )
                }
            } else {
                items(uiState.results, key = { it.endpoint }) { result ->
                    ScanResultCard(result = result, onUseEndpoint = onUseEndpoint)
                }
            }
        }
    }
}

@Composable
private fun ScanControlCard(
    uiState: DeviceDiscoveryUiState,
    onStartScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = "扫描范围",
                description = scanRangeDescription(uiState),
            )
            if (uiState.scanning) {
                LinearProgressIndicator(
                    progress = {
                        if (uiState.totalCount == 0) 0f else uiState.scannedCount.toFloat() / uiState.totalCount
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "已扫描 ${uiState.scannedCount} / ${uiState.totalCount}，发现 ${uiState.results.size} 个",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DiscoveryStatus(status = uiState.status)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartScanClick,
                    enabled = uiState.network != null && !uiState.scanning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("开始扫描")
                }
                OutlinedButton(
                    onClick = onStopScanClick,
                    enabled = uiState.scanning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun DiscoveryStatus(status: OperationStatus) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> Text(status.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        is OperationStatus.Success -> Text(status.text, color = MaterialTheme.colorScheme.primary)
        is OperationStatus.Failed -> Text(
            text = "${status.text}：${status.suggestion}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ScanResultCard(
    result: AdbScanResult,
    onUseEndpoint: (String, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(result.endpoint, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${result.state.label} · ${result.latencyMs}ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = result.state.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { onUseEndpoint(result.host, result.port) }) {
                Text("使用")
            }
        }
    }
}

private fun scanRangeSummary(networks: List<LocalNetwork>): String {
    return when {
        networks.isEmpty() -> "未检测到局域网"
        networks.size == 1 -> networks.first().subnetLabel
        else -> "${networks.size} 个候选网段"
    }
}

private fun scanRangeDescription(uiState: DeviceDiscoveryUiState): String {
    if (uiState.networks.isEmpty()) return "请先连接 WiFi 或局域网"
    val ranges = uiState.networks.joinToString("、") { "${it.subnetLabel}（${it.sourceLabel}）" }
    return "$ranges，端口 ${uiState.ports.joinToString(" / ")}"
}

@Preview(name = "局域网扫描", showBackground = true, widthDp = 390)
@Composable
private fun DeviceDiscoveryContentPreview() {
    AdbManagerTheme(dynamicColor = false) {
        DeviceDiscoveryContent(
            uiState = DeviceDiscoveryUiState(
                networks = listOf(
                    LocalNetwork(
                        deviceIp = "10.71.180.42",
                        subnetLabel = "10.71.180.0/24",
                        hosts = emptyList(),
                    ),
                    LocalNetwork(
                        deviceIp = "10.43.180.147",
                        subnetLabel = "10.43.180.0/24",
                        hosts = emptyList(),
                        sourceLabel = "最近设备",
                    ),
                ),
                scanning = true,
                scannedCount = 88,
                totalCount = 253,
                results = listOf(
                    AdbScanResult("192.168.1.86", 5555, AdbProbeState.AdbUnauthorized, 32),
                    AdbScanResult("192.168.1.98", 5555, AdbProbeState.AdbAvailable, 18),
                ),
                status = OperationStatus.Running("正在扫描 192.168.1.0/24"),
            ),
            onBackClick = {},
            onRefreshNetworkClick = {},
            onStartScanClick = {},
            onStopScanClick = {},
            onUseEndpoint = { _, _ -> },
        )
    }
}
