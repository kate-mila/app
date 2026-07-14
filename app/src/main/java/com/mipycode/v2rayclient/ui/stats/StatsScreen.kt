package com.mipycode.v2rayclient.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mipycode.v2rayclient.service.VpnStatus
import com.mipycode.v2rayclient.ui.theme.*
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val status by VpnStatus.status.collectAsState()
    val logs = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        VpnStatus.logs.collect { logs.add(0, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgSurface)))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                }
                Text("آمار ترافیک", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "آپلود", value = formatBytes(status.uplinkBytes), color = AccentSecondary, modifier = Modifier.weight(1f))
                StatCard(title = "دانلود", value = formatBytes(status.downlinkBytes), color = AccentPrimary, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("مدت اتصال", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    val durationText = if (status.state == VpnStatus.State.CONNECTED && status.connectedSinceMs > 0) {
                        formatDuration(System.currentTimeMillis() - status.connectedSinceMs)
                    } else "متصل نیست"
                    Text(durationText, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("گزارش هسته", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (logs.isEmpty()) {
                    Text("هنوز رویدادی ثبت نشده", color = TextSecondary)
                } else {
                    LazyColumnLogs(logs)
                }
            }
        }
    }
}

@Composable
private fun LazyColumnLogs(logs: List<String>) {
    LazyColumn {
        items(logs) { line ->
            Text(line, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column {
            Text(title, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
    val value = bytes / 1024.0.pow(exp.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[exp - 1])
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
