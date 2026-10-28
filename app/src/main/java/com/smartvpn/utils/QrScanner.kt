package com.smartvpn.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator

object QrScanner {

    /**
     * Launches the ZXing QR Code scanner activity.
     */
    fun startScanner(activity: Activity) {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a VPN Configuration QR Code")
        integrator.setCameraId(0)  // Use a specific camera of the device
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    /**
     * Basic URI parser for VLess.
     * Example: vless://uuid@server:port?encryption=none&security=tls&sni=example.com#Name
     * Returns a map of extracted parameters, or null if invalid.
     */
    fun parseVlessUri(uriString: String): Map<String, String>? {
        if (!uriString.startsWith("vless://")) return null

        try {
            val result = mutableMapOf<String, String>()
            
            // Extract the Name (Fragment)
            val nameSplit = uriString.split("#")
            result["name"] = if (nameSplit.size > 1) nameSplit[1] else "Imported VLess"
            
            // Extract UUID and Authority
            val mainPart = nameSplit[0].substringAfter("vless://")
            val uuidSplit = mainPart.split("@")
            if (uuidSplit.size < 2) return null
            result["uuid"] = uuidSplit[0]
            
            // Extract Server, Port, and Query Params
            val serverAndQuery = uuidSplit[1].split("?")
            val serverPortSplit = serverAndQuery[0].split(":")
            if (serverPortSplit.size < 2) return null
            
            result["serverAddress"] = serverPortSplit[0]
            result["serverPort"] = serverPortSplit[1].substringBefore("/")

            // Extract Query parameters (SNI, security, etc.)
            if (serverAndQuery.size > 1) {
                val params = serverAndQuery[1].split("&")
                for (param in params) {
                    val kv = param.split("=")
                    if (kv.size == 2) {
                        result[kv[0]] = kv[1]
                    }
                }
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
