package com.example.babyparenting.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.babyparenting.data.model.ProgressSummary
import com.example.babyparenting.data.model.*
import com.example.babyparenting.data.repository.MillionaireRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MillionaireViewModel @Inject constructor(
    private val repository: MillionaireRepository
) : ViewModel() {

    private val _strategiesState = MutableStateFlow<StrategiesUiState>(StrategiesUiState.Loading)
    val strategiesState: StateFlow<StrategiesUiState> = _strategiesState.asStateFlow()

    private val _activitiesState = MutableStateFlow<ActivitiesUiState>(ActivitiesUiState.Idle)
    val activitiesState: StateFlow<ActivitiesUiState> = _activitiesState.asStateFlow()

    private val _dailyActivityState = MutableStateFlow<DailyActivityUiState>(DailyActivityUiState.Loading)
    val dailyActivityState: StateFlow<DailyActivityUiState> = _dailyActivityState.asStateFlow()

    private val _progressState = MutableStateFlow<ProgressUiState>(ProgressUiState.Idle)
    val progressState: StateFlow<ProgressUiState> = _progressState.asStateFlow()

    private val _completionState = MutableStateFlow<CompletionUiState>(CompletionUiState.Idle)
    val completionState: StateFlow<CompletionUiState> = _completionState.asStateFlow()

    val completedActivities: Flow<Set<Int>> = repository.getCompletedActivities()
    val childAge: Flow<Int> = repository.getChildAge()
    val userId: Flow<String> = repository.getUserId()

    init {
        loadStrategies()
        loadDailyActivity()
        loadProgress()
    }

    fun loadStrategies() {
        viewModelScope.launch {
            _strategiesState.value = StrategiesUiState.Loading
            val result = repository.getStrategies()
            result.fold(
                onSuccess = { strategies ->
                    _strategiesState.value = StrategiesUiState.Success(strategies)
                },
                onFailure = { error ->
                    _strategiesState.value = StrategiesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadActivitiesForStrategy(strategyId: Int) {
        viewModelScope.launch {
            _activitiesState.value = ActivitiesUiState.Loading
            val result = repository.getActivities(strategyId)
            result.fold(
                onSuccess = { activities ->
                    _activitiesState.value = ActivitiesUiState.Success(activities)
                },
                onFailure = { error ->
                    _activitiesState.value = ActivitiesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun markActivityAsCompleted(activityId: Int) {
        viewModelScope.launch {
            _completionState.value = CompletionUiState.Loading
            val userId = repository.getUserId().firstOrNull() ?: "default_user"
            val result = repository.completeActivity(userId, activityId)
            result.fold(
                onSuccess = {
                    _completionState.value = CompletionUiState.Success("Activity marked as completed!")
                    loadProgress()
                },
                onFailure = { error ->
                    _completionState.value = CompletionUiState.Error(error.message ?: "Failed to complete activity")
                }
            )
        }
    }

    fun setChildAge(age: Int) {
        viewModelScope.launch {
            repository.setChildAge(age)
            loadStrategies()
            loadDailyActivity()
        }
    }

    fun setUserId(userId: String) {
        viewModelScope.launch {
            repository.setUserId(userId)
        }
    }

    fun loadDailyActivity() {
        viewModelScope.launch {
            _dailyActivityState.value = DailyActivityUiState.Loading
            val userId = repository.getUserId().firstOrNull() ?: "default_user"
            val age = repository.getChildAge().firstOrNull() ?: 3
            val result = repository.getDailyActivity(userId, age)
            result.fold(
                onSuccess = { activity ->
                    _dailyActivityState.value = DailyActivityUiState.Success(activity)
                },
                onFailure = { error ->
                    _dailyActivityState.value = DailyActivityUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadProgress() {
        viewModelScope.launch {
            _progressState.value = ProgressUiState.Loading
            val userId = repository.getUserId().firstOrNull() ?: "default_user"
            val result = repository.getProgressSummary(userId)
            result.fold(
                onSuccess = { summary ->
                    _progressState.value = ProgressUiState.Success(summary)
                },
                onFailure = { error ->
                    _progressState.value = ProgressUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun clearCompletion() {
        _completionState.value = CompletionUiState.Idle
    }
}

// ============ UI State Sealed Classes ============

sealed class StrategiesUiState {
    object Loading : StrategiesUiState()
    data class Success(val strategies: List<Strategy>) : StrategiesUiState()
    data class Error(val message: String) : StrategiesUiState()
}

sealed class ActivitiesUiState {
    object Idle : ActivitiesUiState()
    object Loading : ActivitiesUiState()
    data class Success(val activities: List<Activity>) : ActivitiesUiState()
    data class Error(val message: String) : ActivitiesUiState()
}

sealed class DailyActivityUiState {
    object Loading : DailyActivityUiState()
    data class Success(val activity: DailyActivityResponse) : DailyActivityUiState()
    data class Error(val message: String) : DailyActivityUiState()
}

sealed class ProgressUiState {
    object Idle : ProgressUiState()
    object Loading : ProgressUiState()
    data class Success(val progress: ProgressSummary) : ProgressUiState()
    data class Error(val message: String) : ProgressUiState()
}

sealed class CompletionUiState {
    object Idle : CompletionUiState()
    object Loading : CompletionUiState()
    data class Success(val message: String) : CompletionUiState()
    data class Error(val message: String) : CompletionUiState()
}