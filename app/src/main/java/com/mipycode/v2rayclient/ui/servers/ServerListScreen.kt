package com.mipycode.v2rayclient.ui.servers

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mipycode.v2rayclient.MainActivity
import com.mipycode.v2rayclient.data.model.ServerConfig
import com.mipycode.v2rayclient.service.V2RayVpnService
import com.mipycode.v2rayclient.service.VpnStatus
import com.mipycode.v2rayclient.ui.theme.*

@Composable
fun ServerListScreen(
    activity: MainActivity,
    onAddServer: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ServerListViewModel = viewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val status by viewModel.vpnStatus.collectAsState()
    val selectedId by viewModel.selectedServerId.collectAsState()

    val isConnected = status.state == VpnStatus.State.CONNECTED
    val isConnecting = status.state == VpnStatus.State.CONNECTING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgSurface)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            // ---- هدر ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("کلاینت V2Ray", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Row {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "آمار ترافیک", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- کارت وضعیت اتصال ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val statusColor = when (status.state) {
                        VpnStatus.State.CONNECTED -> AccentSuccess
                        VpnStatus.State.CONNECTING -> AccentWarning
                        VpnStatus.State.ERROR -> AccentError
                        VpnStatus.State.DISCONNECTED -> TextSecondary
                    }
                    val statusText = when (status.state) {
                        VpnStatus.State.CONNECTED -> "متصل"
                        VpnStatus.State.CONNECTING -> "در حال اتصال…"
                        VpnStatus.State.ERROR -> "خطا در اتصال"
                        VpnStatus.State.DISCONNECTED -> "قطع"
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(statusColor.copy(alpha = 0.35f), statusColor.copy(alpha = 0.05f))))
                            .clickable(enabled = selectedId != null && !isConnecting) {
                                val server = servers.find { it.id == selectedId } ?: return@clickable
                                if (isConnected) {
                                    activity.stopService(Intent(activity, V2RayVpnService::class.java).apply {
                                        action = V2RayVpnService.ACTION_DISCONNECT
                                    })
                                } else {
                                    connectToServer(activity, server)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                            contentDescription = statusText,
                            tint = statusColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(statusText, style = MaterialTheme.typography.titleMedium, color = statusColor)
                    status.serverRemark?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سرورها (${servers.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Row {
                    TextButton(onClick = { viewModel.pingAll() }) {
                        Text("تست همه", color = AccentSecondary)
                    }
                    TextButton(onClick = onAddServer) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AccentPrimary)
                        Text(" افزودن", color = AccentPrimary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (servers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "هنوز سروری اضافه نکردی.\nاز دکمه‌ی «افزودن» یا اسکن QR شروع کن.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            isSelected = server.id == selectedId,
                            onSelect = { viewModel.selectServer(server.id) },
                            onFavorite = { viewModel.toggleFavorite(server) },
                            onPing = { viewModel.pingServer(server) },
                            onDelete = { viewModel.deleteServer(server) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = AccentPrimary, unselectedColor = TextSecondary)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(server.remark, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
                Text(
                    "${server.protocol.name} · ${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                val pingText = when {
                    server.lastPingMs < 0 -> "تست نشده"
                    else -> "${server.lastPingMs} ms"
                }
                val pingColor = when {
                    server.lastPingMs < 0 -> TextSecondary
                    server.lastPingMs < 300 -> AccentSuccess
                    server.lastPingMs < 800 -> AccentWarning
                    else -> AccentError
                }
                Text(pingText, style = MaterialTheme.typography.bodyMedium, color = pingColor)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (server.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "علاقه‌مندی",
                    tint = if (server.isFavorite) AccentWarning else TextSecondary
                )
            }
            IconButton(onClick = onPing) {
                Icon(Icons.Default.NetworkPing, contentDescription = "تست پینگ", tint = AccentSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = AccentError)
            }
        }
    }
}

private fun connectToServer(activity: MainActivity, server: ServerConfig) {
    activity.requestVpnPermission { granted ->
        if (!granted) return@requestVpnPermission
        VpnStatus.setState(VpnStatus.State.CONNECTING, server.remark)
        val intent = Intent(activity, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_SERVER_ID, server.id)
        }
        activity.startService(intent)
    }
}
