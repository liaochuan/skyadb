package com.sky22333.skyadb.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky22333.skyadb.AppServices
import com.sky22333.skyadb.model.AdbOperationResult
import com.sky22333.skyadb.model.AppInfo
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.repository.AdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppsUiState(
    val query: String = "",
    val filter: AppFilter = AppFilter.All,
    val apps: List<AppInfo> = emptyList(),
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
                        it.label.contains(query, ignoreCase = true)
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
}

class AppsViewModel(
    private val adbRepository: AdbRepository = AppServices.adbRepository,
) : ViewModel() {
    private val state = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = state.asStateFlow()

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

    fun launchApp(packageName: String) {
        runAppAction("正在启动 $packageName") { adbRepository.launchApp(packageName) }
    }

    fun forceStopApp(packageName: String) {
        runAppAction("正在停止 $packageName") { adbRepository.forceStopApp(packageName) }
    }

    fun uninstallApp(packageName: String) {
        state.value = state.value.copy(pendingAction = AppPendingAction.Uninstall(packageName))
    }

    fun cancelPendingAction() {
        state.value = state.value.copy(pendingAction = null)
    }

    fun confirmPendingAction() {
        val action = state.value.pendingAction ?: return
        state.value = state.value.copy(pendingAction = null)
        runAppAction(
            runningText = "正在卸载 ${action.packageName}",
            refreshAfterSuccess = true,
        ) {
            adbRepository.uninstall(action.packageName)
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
}
