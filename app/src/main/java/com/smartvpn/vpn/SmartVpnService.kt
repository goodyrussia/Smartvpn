package com.smartvpn.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartvpn.MainActivity
import com.smartvpn.SmartVpnApp
import com.smartvpn.core.SingboxWrapper
import com.smartvpn.data.local.VpnProfileDao
import com.smartvpn.utils.EngineConfigBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartVpnService : VpnService() {

    @Inject
    lateinit var vpnProfileDao: VpnProfileDao

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        const val ACTION_START_VPN = "com.smartvpn.START_VPN"
        const val ACTION_STOP_VPN = "com.smartvpn.STOP_VPN"
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> {
                val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, -1)
                val allowedApps = intent.getStringArrayListExtra("ALLOWED_APPS") ?: arrayListOf()
                val bypassLan = intent.getBooleanExtra("BYPASS_LAN", true)

                if (profileId != -1) {
                    startForegroundService()
                    serviceScope.launch {
                        val profile = vpnProfileDao.getProfileById(profileId)
                        if (profile != null) {
                            startVpnConnection(profile, allowedApps, bypassLan)
                        } else {
                            Log.e("SmartVpn", "Profile not found for ID: $profileId")
                            stopVpnConnection()
                        }
                    }
                } else {
                    Log.e("SmartVpn", "No profile ID provided")
                    stopSelf()
                }
            }
            ACTION_STOP_VPN -> stopVpnConnection()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, SmartVpnApp.VPN_CHANNEL_ID)
            .setContentTitle("Smart VPN is active")
            .setContentText("Securing your connection")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private suspend fun startVpnConnection(
        profile: com.smartvpn.data.local.VpnProfile,
        allowedApps: List<String>,
        bypassLan: Boolean
    ) {
        try {
            val builder = Builder()
                .setSession("Smart VPN - ${profile.name}")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)

            builder.addDnsServer("1.1.1.1")
            builder.addDnsServer("1.0.0.1")

            if (bypassLan) {
                // Route public IPs, bypass local subnets (192.168.x.x, 10.x.x.x, 172.16.x.x)
                builder.addRoute("1.0.0.0", 8)
                builder.addRoute("2.0.0.0", 7)
            }
            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)

            if (allowedApps.isNotEmpty()) {
                for (appPackage in allowedApps) {
                    try {
                        builder.addAllowedApplication(appPackage)
                    } catch (e: Exception) {
                        Log.e("SmartVpn", "Package not found: $appPackage")
                    }
                }
            }

            vpnInterface = builder.establish()
            val fd = vpnInterface?.fd

            if (fd != null) {
                Log.d("SmartVpn", "VPN Interface established. FD: $fd")
                
                // Generate the JSON configuration based on the user's profile
                val configJson = EngineConfigBuilder.buildSingboxConfig(profile, fd)
                
                // Start the Go engine with the configuration
                val success = SingboxWrapper.startEngine(configJson)
                if (!success) {
                    Log.e("SmartVpn", "Engine failed to start.")
                    stopVpnConnection()
                }
            } else {
                Log.e("SmartVpn", "Failed to establish VPN interface (FD is null)")
                stopVpnConnection()
            }

        } catch (e: Exception) {
            Log.e("SmartVpn", "Error starting VPN: ${e.message}")
            stopVpnConnection()
        }
    }

    private fun stopVpnConnection() {
        serviceScope.launch {
            SingboxWrapper.stopEngine()
            try {
                vpnInterface?.close()
            } catch (e: Exception) {
                Log.e("SmartVpn", "Error closing VPN interface: ${e.message}")
            } finally {
                vpnInterface = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
