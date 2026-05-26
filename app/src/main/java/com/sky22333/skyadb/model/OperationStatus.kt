package com.sky22333.skyadb.model

sealed interface OperationStatus {
    data object Idle : OperationStatus
    data class Running(val text: String) : OperationStatus
    data class Success(val text: String) : OperationStatus
    data class Failed(val text: String, val suggestion: String) : OperationStatus
}
