package com.smartvpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartvpn.ui.profiles.AddProfileScreen
import com.smartvpn.ui.profiles.MainScreen
import com.smartvpn.vpn.SmartVpnService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingProfileId: Int? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && pendingProfileId != null) {
            startSmartVpn(pendingProfileId!!)
        } else {
            Toast.makeText(this, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                onNavigateToAddProfile = { navController.navigate("add_profile") },
                                onConnectClick = { profileId -> 
                                    checkAndStartVpn(profileId) 
                                },
                                onDisconnectClick = { stopSmartVpn() }
                            )
                        }
                        composable("add_profile") {
                            AddProfileScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndStartVpn(profileId: Int) {
        pendingProfileId = profileId
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startSmartVpn(profileId)
        }
    }

    private fun startSmartVpn(profileId: Int) {
        val serviceIntent = Intent(this, SmartVpnService::class.java).apply {
            action = SmartVpnService.ACTION_START_VPN
            putExtra(SmartVpnService.EXTRA_PROFILE_ID, profileId)
            putExtra("BYPASS_LAN", true)
        }
        startService(serviceIntent)
    }

    private fun stopSmartVpn() {
        val serviceIntent = Intent(this, SmartVpnService::class.java).apply {
            action = SmartVpnService.ACTION_STOP_VPN
        }
        startService(serviceIntent)
    }
}
