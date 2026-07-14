package com.mipycode.v2rayclient.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * وضعیت اتصال VPN به‌صورت سراسری، تا صفحات مختلف UI (کارت وضعیت، صفحه‌ی آمار)
 * بتوانند بدون نیاز به bind کردن مستقیم به سرویس، تغییرات را observe کنند.
 */
object VpnStatus {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    data class Snapshot(
        val state: State = State.DISCONNECTED,
        val serverRemark: String? = null,
        val uplinkBytes: Long = 0,
        val downlinkBytes: Long = 0,
        val connectedSinceMs: Long = 0
    )

    private val _status = MutableStateFlow(Snapshot())
    val status = _status.asStateFlow()

    private val _logs = MutableSharedFlow<String>(replay = 50)
    val logs = _logs.asSharedFlow()

    fun setState(state: State, remark: String?) {
        _status.value = _status.value.copy(
            state = state,
            serverRemark = remark ?: _status.value.serverRemark,
            connectedSinceMs = if (state == State.CONNECTED) System.currentTimeMillis() else _status.value.connectedSinceMs
        )
    }

    fun updateTraffic(uplink: Long, downlink: Long) {
        _status.value = _status.value.copy(uplinkBytes = uplink, downlinkBytes = downlink)
    }

    fun appendLog(message: String) {
        _logs.tryEmit(message)
    }
}
