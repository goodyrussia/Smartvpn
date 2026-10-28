package com.smartvpn.utils

import android.util.Log
import com.topjohnwu.superuser.Shell

object TetheringManager {

    private const val TAG = "TetheringManager"
    
    // Default Android Hotspot interface. This can vary by manufacturer (e.g., swlan0, wlan1)
    private const val HOTSPOT_INTERFACE = "wlan0" 
    
    // The TUN interface created by SmartVpnService
    private const val VPN_INTERFACE = "tun0" 

    /**
     * Checks if the device has root access available.
     */
    fun isRootAvailable(): Boolean {
        return Shell.getShell().isRoot
    }

    /**
     * Applies iptables rules to forward Hotspot traffic through the VPN TUN interface.
     * Requires Root.
     */
    fun enableRootTethering(): Boolean {
        if (!isRootAvailable()) {
            Log.e(TAG, "Root is not available.")
            return false
        }

        try {
            // Enable IP Forwarding
            Shell.cmd("sysctl -w net.ipv4.ip_forward=1").exec()

            // Forward traffic from Hotspot to VPN
            Shell.cmd("iptables -t filter -A FORWARD -i $HOTSPOT_INTERFACE -o $VPN_INTERFACE -j ACCEPT").exec()
            Shell.cmd("iptables -t filter -A FORWARD -i $VPN_INTERFACE -o $HOTSPOT_INTERFACE -m state --state ESTABLISHED,RELATED -j ACCEPT").exec()
            
            // Masquerade the traffic so it appears to come from the phone's IP on the TUN interface
            Shell.cmd("iptables -t nat -A POSTROUTING -o $VPN_INTERFACE -j MASQUERADE").exec()
            
            Log.d(TAG, "Root tethering enabled successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply iptables rules: ${e.message}")
            return false
        }
    }

    /**
     * Removes the iptables rules to stop tethering through the VPN.
     */
    fun disableRootTethering() {
        if (!isRootAvailable()) return

        try {
            Shell.cmd("iptables -t filter -D FORWARD -i $HOTSPOT_INTERFACE -o $VPN_INTERFACE -j ACCEPT").exec()
            Shell.cmd("iptables -t filter -D FORWARD -i $VPN_INTERFACE -o $HOTSPOT_INTERFACE -m state --state ESTABLISHED,RELATED -j ACCEPT").exec()
            Shell.cmd("iptables -t nat -D POSTROUTING -o $VPN_INTERFACE -j MASQUERADE").exec()
            
            Log.d(TAG, "Root tethering disabled.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove iptables rules: ${e.message}")
        }
    }
}
