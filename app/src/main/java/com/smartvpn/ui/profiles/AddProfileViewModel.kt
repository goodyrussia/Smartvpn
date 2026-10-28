package com.smartvpn.ui.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartvpn.data.local.ProfileType
import com.smartvpn.data.local.VpnProfile
import com.smartvpn.data.local.VpnProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddProfileViewModel @Inject constructor(
    private val vpnProfileDao: VpnProfileDao
) : ViewModel() {

    var profileName by mutableStateOf("")
    var serverAddress by mutableStateOf("")
    var serverPort by mutableStateOf("443")
    var uuid by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var sni by mutableStateOf("")
    var customPayload by mutableStateOf("")
    var rawJsonConfig by mutableStateOf("")
    var selectedType by mutableStateOf(ProfileType.VLESS)
    var enableTls by mutableStateOf(true)
    var enableTlsFragmentation by mutableStateOf(false)

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val portInt = serverPort.toIntOrNull() ?: 443
            val newProfile = VpnProfile(
                name = profileName.ifEmpty { "${selectedType.name} Config" },
                type = selectedType,
                serverAddress = serverAddress,
                serverPort = portInt,
                uuid = uuid.ifEmpty { null },
                username = username.ifEmpty { null },
                password = password.ifEmpty { null },
                sni = sni.ifEmpty { null },
                customPayload = customPayload.ifEmpty { null },
                rawJsonConfig = rawJsonConfig.ifEmpty { null },
                enableTls = enableTls,
                enableTlsFragmentation = enableTlsFragmentation
            )
            vpnProfileDao.insertProfile(newProfile)
            onSuccess()
        }
    }

    fun exportCurrentConfig(): String {
        val profile = VpnProfile(
            name = profileName,
            type = selectedType,
            serverAddress = serverAddress,
            serverPort = serverPort.toIntOrNull() ?: 443,
            uuid = uuid,
            sni = sni,
            rawJsonConfig = rawJsonConfig
        )
        return Gson().toJson(profile)
    }

    fun importFromJsonOrUri(data: String): Boolean {
        return try {
            if (data.trim().startsWith("{")) {
                // JSON Import
                val profile = Gson().fromJson(data, VpnProfile::class.java)
                profileName = profile.name
                serverAddress = profile.serverAddress
                serverPort = profile.serverPort.toString()
                uuid = profile.uuid ?: ""
                sni = profile.sni ?: ""
                selectedType = profile.type
                rawJsonConfig = profile.rawJsonConfig ?: ""
                true
            } else if (data.startsWith("vless://")) {
                // VLess URI Import
                selectedType = ProfileType.VLESS
                profileName = data.substringAfter("#", "Imported VLess")
                val cleanUri = data.substringBefore("#").substringAfter("vless://")
                uuid = cleanUri.substringBefore("@")
                val hostPort = cleanUri.substringAfter("@").substringBefore("?")
                serverAddress = hostPort.substringBefore(":")
                serverPort = hostPort.substringAfter(":", "443")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
