package com.smartvpn.utils

import com.smartvpn.data.local.ProfileType
import com.smartvpn.data.local.VpnProfile
import org.json.JSONArray
import org.json.JSONObject

object EngineConfigBuilder {

    /**
     * Builds a complete Sing-box JSON configuration based on the user's profile.
     * @param profile The VpnProfile from the Room database.
     * @param tunFd The File Descriptor of the Android VpnService interface.
     */
    fun buildSingboxConfig(profile: VpnProfile, tunFd: Int): String {
        val config = JSONObject()

        // 1. Log configuration
        config.put("log", JSONObject().apply {
            put("level", "info")
            put("timestamp", true)
        })

        // 2. DNS configuration (Routing through the proxy to prevent leaks)
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("tag", "remote-dns")
                    put("address", "tcp://1.1.1.1") // Can be customizable
                    put("detour", "proxy")
                })
            })
            put("rules", JSONArray())
        })

        // 3. Inbounds: This captures the traffic from Android's TUN interface
        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                // Sing-box allows passing the VpnService File Descriptor directly
                put("file_descriptor", tunFd) 
                put("mtu", 1500)
                put("auto_route", true)
                put("strict_route", true)
                put("endpoint_independent_nat", true)
            })
        })

        // 4. Outbounds: Where the traffic goes (VLESS, VMESS, SSH, etc.)
        val outbounds = JSONArray()
        
        // Add the primary proxy outbound based on profile type
        val proxyOutbound = when (profile.type) {
            ProfileType.VLESS -> createVlessOutbound(profile)
            ProfileType.VMESS -> createVmessOutbound(profile)
            else -> createDirectOutbound("proxy") // Fallback
        }
        outbounds.put(proxyOutbound)

        // Add a direct outbound for bypassed traffic
        outbounds.put(createDirectOutbound("direct"))
        // Add a block outbound for ad-blocking/routing drops
        outbounds.put(JSONObject().apply {
            put("type", "block")
            put("tag", "block")
        })

        config.put("outbounds", outbounds)

        // 5. Routing Rules (Send traffic to proxy by default)
        config.put("route", JSONObject().apply {
            put("auto_detect_interface", true)
            put("rules", JSONArray().apply {
                // Rule to route DNS queries to the remote-dns tag
                put(JSONObject().apply {
                    put("port", 53)
                    put("outbound", "remote-dns")
                })
            })
        })

        return config.toString(2) // Pretty print with 2 spaces for debugging
    }

    private fun createVlessOutbound(profile: VpnProfile): JSONObject {
        return JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", profile.serverAddress)
            put("server_port", profile.serverPort)
            put("uuid", profile.uuid ?: "")
            put("network", "tcp")

            // Setup TLS and DPI Fragmentation if enabled
            if (profile.enableTls) {
                put("tls", JSONObject().apply {
                    put("enabled", true)
                    put("server_name", profile.sni ?: profile.serverAddress)
                    put("insecure", false)
                    put("utls", JSONObject().apply {
                        put("enabled", true)
                        put("fingerprint", "chrome") // uTLS masquerading
                    })
                })
            }

            // Multiplexing for stability
            put("multiplex", JSONObject().apply {
                put("enabled", true)
                put("protocol", "smux")
            })
        }
    }

    private fun createVmessOutbound(profile: VpnProfile): JSONObject {
        return JSONObject().apply {
            put("type", "vmess")
            put("tag", "proxy")
            put("server", profile.serverAddress)
            put("server_port", profile.serverPort)
            put("uuid", profile.uuid ?: "")
            put("security", "auto")
            put("alter_id", 0)
        }
    }

    private fun createDirectOutbound(tag: String): JSONObject {
        return JSONObject().apply {
            put("type", "direct")
            put("tag", tag)
        }
    }
}
