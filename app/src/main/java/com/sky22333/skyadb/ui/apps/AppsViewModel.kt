package com.sky22333.skyadb.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.apps.AppMetadata
import com.sky22333.skyadb.apps.AppMetadataLoader
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.AppInfo
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.repository.AdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class AppsUiState(
    val query: String = "",
    val filter: AppFilter = AppFilter.All,
    val apps: List<AppInfo> = emptyList(),
    val appMetadata: Map<String, AppMetadata> = emptyMap(),
    val pendingAction: AppPendingAction? = null,
    val operationStatus: OperationStatus = OperationStatus.Idle,
    val loading: Boolean = false,
) {
    val filteredApps: List<AppInfo>
        get() {
            val typedApps = when (filter) {
                AppFilter.All -> apps
                AppFilter.User -> apps.filterNot { it.isSystem }
                AppFilter.System -> apps.filter { it.isSystem }
            }
            return if (query.isBlank()) {
                typedApps
            } else {
                typedApps.filter {
                    it.packageName.contains(query, ignoreCase = true) ||
                        it.label.contains(query, ignoreCase = true) ||
                        appMetadata[it.packageName]?.label?.contains(query, ignoreCase = true) == true
                }
            }
        }
}

enum class AppFilter(val label: String) {
    All("全部"),
    User("用户应用"),
    System("系统应用"),
}

sealed interface AppPendingAction {
    val packageName: String

    data class Uninstall(override val packageName: String) : AppPendingAction
    data class SetEnabled(
        override val packageName: String,
        val enabled: Boolean,
        val isSystem: Boolean,
    ) : AppPendingAction
}

class AppsViewModel(
    private val adbRepository: AdbRepository = AppServices.adbRepository,
    private val metadataLoader: AppMetadataLoader = AppServices.appMetadataLoader,
) : ViewModel() {
    private val state = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = state.asStateFlow()
    private val metadataJobs = mutableMapOf<String, Job>()
    private val metadataSemaphore = Semaphore(MetadataConcurrency)

    fun onQueryChanged(value: String) {
        state.value = state.value.copy(query = value)
    }

    fun onFilterChanged(filter: AppFilter) {
        state.value = state.value.copy(filter = filter)
    }

    fun loadApps(force: Boolean = false) {
        if (!force && state.value.apps.isNotEmpty()) return
        state.value = state.value.copy(
            loading = true,
            operationStatus = OperationStatus.Running("正在读取应用列表"),
        )
        viewModelScope.launch {
            when (val result = adbRepository.listApps()) {
                is AdbOperationResult.Success -> {
                    state.value = state.value.copy(
                        apps = result.data,
                        loading = false,
                        operationStatus = OperationStatus.Success("已读取 ${result.data.size} 个应用"),
                    )
                }
                is AdbOperationResult.Failure -> {
                    state.value = state.value.copy(
                        loading = false,
                        operationStatus = OperationStatus.Failed(result.message, result.suggestion),
                    )
                }
            }
        }
    }

    fun loadAppMetadata(app: AppInfo) {
        if (app.sourcePath.isBlank()) return
        if (state.value.appMetadata.containsKey(app.packageName)) return
        if (metadataJobs.containsKey(app.packageName)) return

        metadataJobs[app.packageName] = viewModelScope.launch {
            try {
                val metadata = metadataSemaphore.withPermit {
                    metadataLoader.cached(app.packageName, app.sourcePath)?.let { return@withPermit it }
                    val apkFile = metadataLoader.tempApkFile(app.packageName, app.sourcePath)
                    apkFile.parentFile?.mkdirs()
                    try {
                        when (adbRepository.pull(app.sourcePath, apkFile)) {
                            is AdbOperationResult.Success -> metadataLoader.load(app.packageName, app.sourcePath, apkFile)
                            is AdbOperationResult.Failure -> null
                        }
                    } finally {
                        apkFile.delete()
                    }
                }
                if (metadata != null) {
                    state.value = state.value.copy(
                        appMetadata = state.value.appMetadata + (app.packageName to metadata),
                    )
                }
            } finally {
                metadataJobs.remove(app.packageName)
            }
        }
    }

    fun cancelAppMetadataLoad(packageName: String) {
        if (state.value.appMetadata.containsKey(packageName)) return
        metadataJobs.remove(packageName)?.cancel()
    }

    fun launchApp(packageName: String) {
        runAppAction("正在启动 $packageName") { adbRepository.launchApp(packageName) }
    }

    fun forceStopApp(packageName: String) {
        runAppAction("正在停止 $packageName") { adbRepository.forceStopApp(packageName) }
    }

    fun uninstallApp(packageName: String) {
        state.value = state.value.copy(pendingAction = AppPendingAction.Uninstall(packageName))
    }

    fun setAppEnabled(app: AppInfo, enabled: Boolean) {
        state.value = state.value.copy(
            pendingAction = AppPendingAction.SetEnabled(
                packageName = app.packageName,
                enabled = enabled,
                isSystem = app.isSystem,
            ),
        )
    }

    fun cancelPendingAction() {
        state.value = state.value.copy(pendingAction = null)
    }

    fun confirmPendingAction() {
        val action = state.value.pendingAction ?: return
        state.value = state.value.copy(pendingAction = null)
        when (action) {
            is AppPendingAction.Uninstall -> runAppAction(
                runningText = "正在卸载 ${action.packageName}",
                refreshAfterSuccess = true,
            ) {
                adbRepository.uninstall(action.packageName)
            }
            is AppPendingAction.SetEnabled -> runAppAction(
                runningText = if (action.enabled) {
                    "正在启用 ${action.packageName}"
                } else {
                    "正在冻结 ${action.packageName}"
                },
                refreshAfterSuccess = true,
            ) {
                adbRepository.setAppEnabled(action.packageName, action.enabled)
            }
        }
    }

    private fun runAppAction(
        runningText: String,
        refreshAfterSuccess: Boolean = false,
        action: suspend () -> AdbOperationResult<Unit>,
    ) {
        state.value = state.value.copy(operationStatus = OperationStatus.Running(runningText))
        viewModelScope.launch {
            when (val result = action()) {
                is AdbOperationResult.Success -> {
                    state.value = state.value.copy(operationStatus = OperationStatus.Success("操作完成"))
                    if (refreshAfterSuccess) {
                        loadApps(force = true)
                    }
                }
                is AdbOperationResult.Failure -> {
                    state.value = state.value.copy(
                        operationStatus = OperationStatus.Failed(result.message, result.suggestion),
                    )
                }
            }
        }
    }

    private companion object {
        const val MetadataConcurrency = 3
    }
}
