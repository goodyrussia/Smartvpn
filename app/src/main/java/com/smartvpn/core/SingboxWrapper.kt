package com.smartvpn.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A wrapper object to interface with the Go-compiled Sing-box library.
 * Once you compile the Go code, you will import it here (e.g., import libsingbox.Libsingbox)
 */
object SingboxWrapper {
    private var isRunning = false
    private const val TAG = "SingboxWrapper"

    suspend fun startEngine(configJson: String): Boolean = withContext(Dispatchers.IO) {
        if (isRunning) {
            Log.d(TAG, "Engine is already running")
            return@withContext true
        }
        
        try {
            Log.d(TAG, "Starting Engine with config length: ${configJson.length}")
            
            // TODO: Call your gomobile method here.
            // Example: Libsingbox.start(configJson)
            
            isRunning = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Engine: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun stopEngine() = withContext(Dispatchers.IO) {
        if (!isRunning) return@withContext
        try {
            Log.d(TAG, "Stopping Engine...")
            
            // TODO: Call your gomobile method here.
            // Example: Libsingbox.stop()
            
            isRunning = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Engine: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun isEngineRunning(): Boolean = isRunning
}
