package com.mipycode.v2rayclient.ui.addserver

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mipycode.v2rayclient.data.model.ProtocolType
import com.mipycode.v2rayclient.data.model.ServerConfig
import com.mipycode.v2rayclient.data.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddServerResult {
    object Idle : AddServerResult()
    object Success : AddServerResult()
    data class Error(val message: String) : AddServerResult()
}

class AddServerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServerRepository(application)

    private val _result = MutableStateFlow<AddServerResult>(AddServerResult.Idle)
    val result: StateFlow<AddServerResult> = _result.asStateFlow()

    fun addFromLink(link: String) {
        viewModelScope.launch {
            try {
                repository.addFromLink(link)
                _result.value = AddServerResult.Success
            } catch (e: Exception) {
                _result.value = AddServerResult.Error(e.message ?: "خطای نامشخص در پردازش لینک")
            }
        }
    }

    fun addManual(
        remark: String,
        protocol: ProtocolType,
        address: String,
        port: Int,
        userId: String,
        network: String,
        path: String,
        host: String,
        tls: String,
        sni: String
    ) {
        viewModelScope.launch {
            try {
                if (remark.isBlank() || address.isBlank() || userId.isBlank()) {
                    _result.value = AddServerResult.Error("لطفاً فیلدهای الزامی را پر کن")
                    return@launch
                }
                repository.addManual(
                    ServerConfig(
                        remark = remark,
                        protocol = protocol,
                        address = address,
                        port = port,
                        userId = userId,
                        network = network,
                        path = path,
                        host = host,
                        tls = tls,
                        sni = sni
                    )
                )
                _result.value = AddServerResult.Success
            } catch (e: Exception) {
                _result.value = AddServerResult.Error(e.message ?: "خطای نامشخص")
            }
        }
    }

    fun resetResult() {
        _result.value = AddServerResult.Idle
    }
}
