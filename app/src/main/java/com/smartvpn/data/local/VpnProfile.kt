package com.smartvpn.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: ProfileType,
    val serverAddress: String,
    val serverPort: Int,
    
    // Auth & Identifiers
    val username: String? = null,
    val password: String? = null,
    val uuid: String? = null,
    val alterId: Int = 0,
    
    // Security & Customization
    val sni: String? = null,
    val enableTls: Boolean = false,
    val enableTlsFragmentation: Boolean = false,
    val fragmentSize: String = "50-100",
    val fragmentInterval: String = "10-20",
    
    // Payload Manipulation (For SSH / HTTP Custom)
    val customPayload: String? = null,
    
    // Raw JSON Config (For Xray/Sing-box full file imports)
    val rawJsonConfig: String? = null
)

enum class ProfileType {
    SSH, VLESS, VMESS, TROJAN, SHADOWSOCKS, HYSTERIA2, RAW_JSON
}
