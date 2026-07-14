package com.mipycode.v2rayclient.ui.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mipycode.v2rayclient.data.model.ServerConfig
import com.mipycode.v2rayclient.data.repository.ServerRepository
import com.mipycode.v2rayclient.service.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServerListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServerRepository(application)

    private val _servers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    val vpnStatus = VpnStatus.status

    private val _selectedServerId = MutableStateFlow<Long?>(null)
    val selectedServerId: StateFlow<Long?> = _selectedServerId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeServers().collect { list ->
                _servers.value = list
                if (_selectedServerId.value == null) {
                    _selectedServerId.value = list.firstOrNull { it.isFavorite }?.id ?: list.firstOrNull()?.id
                }
            }
        }
    }

    fun selectServer(id: Long) {
        _selectedServerId.value = id
    }

    fun deleteServer(server: ServerConfig) = viewModelScope.launch {
        repository.delete(server)
    }

    fun toggleFavorite(server: ServerConfig) = viewModelScope.launch {
        repository.toggleFavorite(server.id, !server.isFavorite)
    }

    fun pingServer(server: ServerConfig) = viewModelScope.launch {
        repository.pingServer(server)
    }

    fun pingAll() = viewModelScope.launch {
        _servers.value.forEach { repository.pingServer(it) }
    }
}
