package com.mipycode.v2rayclient.ui.settings

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Application.dataStore by preferencesDataStore(name = "settings")

data class AppPrefs(
    val autoConnect: Boolean = false,
    val bypassLan: Boolean = true,
    val perAppProxy: Boolean = false,
    val primaryDns: String = "1.1.1.1"
)

private object Keys {
    val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
    val PER_APP_PROXY = booleanPreferencesKey("per_app_proxy")
    val PRIMARY_DNS = stringPreferencesKey("primary_dns")
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    private val _prefs = MutableStateFlow(AppPrefs())
    val prefs: StateFlow<AppPrefs> = _prefs.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { p ->
                AppPrefs(
                    autoConnect = p[Keys.AUTO_CONNECT] ?: false,
                    bypassLan = p[Keys.BYPASS_LAN] ?: true,
                    perAppProxy = p[Keys.PER_APP_PROXY] ?: false,
                    primaryDns = p[Keys.PRIMARY_DNS] ?: "1.1.1.1"
                )
            }.collect { _prefs.value = it }
        }
    }

    fun setAutoConnect(value: Boolean) = update { it[Keys.AUTO_CONNECT] = value }
    fun setBypassLan(value: Boolean) = update { it[Keys.BYPASS_LAN] = value }
    fun setPerAppProxy(value: Boolean) = update { it[Keys.PER_APP_PROXY] = value }
    fun setPrimaryDns(value: String) = update { it[Keys.PRIMARY_DNS] = value }

    private fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
