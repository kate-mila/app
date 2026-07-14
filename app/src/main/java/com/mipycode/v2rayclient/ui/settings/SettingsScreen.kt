package com.mipycode.v2rayclient.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mipycode.v2rayclient.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val prefs by viewModel.prefs.collectAsState()

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
                Text("تنظیمات", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Spacer(Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingSwitchRow(
                        title = "اتصال خودکار هنگام باز شدن اپ",
                        checked = prefs.autoConnect,
                        onCheckedChange = viewModel::setAutoConnect
                    )
                    Divider(color = GlassBorder)
                    SettingSwitchRow(
                        title = "بای‌پس ترافیک شبکه‌ی محلی (LAN)",
                        checked = prefs.bypassLan,
                        onCheckedChange = viewModel::setBypassLan
                    )
                    Divider(color = GlassBorder)
                    SettingSwitchRow(
                        title = "حالت فقط پراکسی برنامه‌های انتخابی",
                        checked = prefs.perAppProxy,
                        onCheckedChange = viewModel::setPerAppProxy
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("DNS اولیه", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = prefs.primaryDns,
                        onValueChange = viewModel::setPrimaryDns,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = GlassBorder
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "نسخه‌ی اپ: 1.0.0",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary, checkedTrackColor = AccentPrimary.copy(alpha = 0.5f))
        )
    }
}
