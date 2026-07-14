package com.mipycode.v2rayclient.ui.addserver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mipycode.v2rayclient.data.model.ProtocolType
import com.mipycode.v2rayclient.ui.theme.*

private enum class AddTab { LINK, QR, MANUAL }

@Composable
fun AddServerScreen(onDone: () -> Unit, viewModel: AddServerViewModel = viewModel()) {
    var tab by remember { mutableStateOf(AddTab.LINK) }
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(result) {
        if (result is AddServerResult.Success) {
            viewModel.resetResult()
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgSurface)))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                }
                Text("افزودن سرور", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Spacer(Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = BgSurface,
                contentColor = AccentPrimary
            ) {
                Tab(selected = tab == AddTab.LINK, onClick = { tab = AddTab.LINK }, text = { Text("لینک") })
                Tab(selected = tab == AddTab.QR, onClick = { tab = AddTab.QR }, text = { Text("اسکن QR") })
                Tab(selected = tab == AddTab.MANUAL, onClick = { tab = AddTab.MANUAL }, text = { Text("دستی") })
            }

            Spacer(Modifier.height(16.dp))

            if (result is AddServerResult.Error) {
                Text(
                    (result as AddServerResult.Error).message,
                    color = AccentError,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when (tab) {
                AddTab.LINK -> LinkTab(onSubmit = viewModel::addFromLink)
                AddTab.QR -> QrTab(onScanned = viewModel::addFromLink)
                AddTab.MANUAL -> ManualTab(onSubmit = viewModel::addManual)
            }
        }
    }
}

@Composable
private fun LinkTab(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("لینک اشتراک‌گذاری را جای‌گذاری کن", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("vless://... یا vmess://... یا trojan://... یا ss://...") },
                    minLines = 3,
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
        Button(
            onClick = { if (text.isNotBlank()) onSubmit(text.trim()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("افزودن سرور")
        }
    }
}

@Composable
private fun QrTab(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context.applicationContext, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
        if (hasPermission) {
            GlassCard(modifier = Modifier.fillMaxSize()) {
                QrScannerView(modifier = Modifier.fillMaxSize(), onScanned = onScanned)
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("برای اسکن QR به دسترسی دوربین نیاز داریم", color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("دادن دسترسی")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualTab(
    onSubmit: (remark: String, protocol: ProtocolType, address: String, port: Int, userId: String, network: String, path: String, host: String, tls: String, sni: String) -> Unit
) {
    var remark by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf(ProtocolType.VLESS) }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var userId by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("tcp") }
    var path by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var tls by remember { mutableStateOf("tls") }
    var sni by remember { mutableStateOf("") }
    var protocolMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                LabeledField("نام (Remark)", remark) { remark = it }
                Spacer(Modifier.height(10.dp))

                Text("پروتکل", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Box {
                    OutlinedButton(onClick = { protocolMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(protocol.name, color = TextPrimary)
                    }
                    DropdownMenu(expanded = protocolMenuExpanded, onDismissRequest = { protocolMenuExpanded = false }) {
                        ProtocolType.values().forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { protocol = p; protocolMenuExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                LabeledField("آدرس سرور", address) { address = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("پورت", port, keyboardType = KeyboardType.Number) { port = it }
                Spacer(Modifier.height(10.dp))
                LabeledField(if (protocol == ProtocolType.TROJAN || protocol == ProtocolType.SHADOWSOCKS) "پسورد" else "UUID", userId) { userId = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("Network (tcp/ws/grpc/http)", network) { network = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("Path", path) { path = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("Host", host) { host = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("TLS (none/tls/reality)", tls) { tls = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("SNI", sni) { sni = it }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSubmit(remark, protocol, address, port.toIntOrNull() ?: 443, userId, network, path, host, tls, sni)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text("افزودن سرور")
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = GlassBorder
            )
        )
    }
}
