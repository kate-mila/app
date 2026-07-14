package com.mipycode.v2rayclient

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mipycode.v2rayclient.ui.addserver.AddServerScreen
import com.mipycode.v2rayclient.ui.servers.ServerListScreen
import com.mipycode.v2rayclient.ui.settings.SettingsScreen
import com.mipycode.v2rayclient.ui.stats.StatsScreen
import com.mipycode.v2rayclient.ui.theme.V2RayClientTheme

class MainActivity : ComponentActivity() {

    /** نتیجه‌ی درخواست مجوز VPN از سیستم‌عامل (VpnService.prepare). */
    private var onVpnPermissionResult: ((Boolean) -> Unit)? = null
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onVpnPermissionResult?.invoke(result.resultCode == RESULT_OK)
    }

    fun requestVpnPermission(onResult: (Boolean) -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            onResult(true) // مجوز از قبل داده شده
        } else {
            onVpnPermissionResult = onResult
            vpnPermissionLauncher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            V2RayClientTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    AppNavHost(activity = this)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(activity: MainActivity) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "servers") {
        composable("servers") {
            ServerListScreen(
                activity = activity,
                onAddServer = { navController.navigate("add_server") },
                onOpenStats = { navController.navigate("stats") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("add_server") {
            AddServerScreen(onDone = { navController.popBackStack() })
        }
        composable("stats") {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
