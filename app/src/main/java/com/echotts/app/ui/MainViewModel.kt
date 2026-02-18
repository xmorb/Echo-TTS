package com.echotts.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echotts.app.api.AlexaRepository
import com.echotts.app.api.ApiResult
import com.echotts.app.api.models.Device
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(private val repository: AlexaRepository) : ViewModel() {

    private val _devices = MutableLiveData<List<Device>>(emptyList())
    val devices: LiveData<List<Device>> = _devices

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getDevices()) {
                is ApiResult.Success -> {
                    _devices.value = result.data
                    _uiState.value = if (result.data.isEmpty()) {
                        UiState.Error("No Echo devices found in your account.")
                    } else {
                        UiState.Success("Found ${result.data.size} device(s).")
                    }
                }
                is ApiResult.Error -> {
                    val hint = if (result.code == 401 || result.code == 403) {
                        " Please log in again."
                    } else ""
                    _uiState.value = UiState.Error(result.message + hint)
                }
            }
        }
    }

    fun speakOnDevice(text: String, device: Device) {
        if (text.isBlank()) {
            _uiState.value = UiState.Error("Please enter text to speak.")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = repository.speakOnDevice(text, device)) {
                is ApiResult.Success -> UiState.Success("Speaking on ${device.displayName}.")
                is ApiResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun speakOnAllDevices(text: String) {
        if (text.isBlank()) {
            _uiState.value = UiState.Error("Please enter text to speak.")
            return
        }
        val deviceList = _devices.value ?: emptyList()
        if (deviceList.isEmpty()) {
            _uiState.value = UiState.Error("No devices loaded. Pull down to refresh.")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = repository.speakOnAllDevices(text, deviceList)) {
                is ApiResult.Success -> UiState.Success("Speaking on all ${deviceList.size} device(s).")
                is ApiResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
