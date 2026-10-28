package com.smartvpn.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvpn.data.local.VpnProfile
import com.smartvpn.data.local.VpnProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val vpnProfileDao: VpnProfileDao
) : ViewModel() {

    // Fetch all profiles from the database as a Flow
    val profiles: StateFlow<List<VpnProfile>> = vpnProfileDao.getAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteProfile(profile: VpnProfile) {
        viewModelScope.launch {
            vpnProfileDao.deleteProfile(profile)
        }
    }
    
    // In a real app, you'd observe a service state to know if connected
    // For now, we simulate basic state tracking
    var isConnected = false
        private set
        
    var connectedProfileId: Int? = null
        private set

    fun setConnectionState(connected: Boolean, profileId: Int? = null) {
        isConnected = connected
        connectedProfileId = profileId
    }
}
