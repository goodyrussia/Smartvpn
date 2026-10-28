package com.smartvpn.utils

object PayloadGenerator {

    /**
     * Replaces standard payload tags with actual values to create a valid HTTP request.
     * Commonly used tags: [netData], [host_port], [host], [port], [crlf], [crlf*2]
     * 
     * Example input: "GET http://[host]/ HTTP/1.1[crlf]Host: [host][crlf]Connection: Keep-Alive[crlf*2]"
     */
    fun generateHttpPayload(
        customPayload: String,
        targetHost: String,
        targetPort: Int,
        sni: String = ""
    ): String {
        var payload = customPayload

        val hostPort = "$targetHost:$targetPort"
        val spoofedHost = if (sni.isNotEmpty()) sni else targetHost
        
        // Define standard HTTP methods to be dynamically replaced if needed
        val method = "CONNECT"
        val protocol = "HTTP/1.1"

        // Replace tags
        payload = payload.replace("[netData]", "$method $hostPort $protocol")
        payload = payload.replace("[host_port]", hostPort)
        payload = payload.replace("[host]", spoofedHost)
        payload = payload.replace("[port]", targetPort.toString())
        payload = payload.replace("[protocol]", protocol)
        payload = payload.replace("[method]", method)
        
        // Handle carriage returns and line feeds
        payload = payload.replace("[cr]", "\r")
        payload = payload.replace("[lf]", "\n")
        payload = payload.replace("[crlf]", "\r\n")
        payload = payload.replace("[crlf*2]", "\r\n\r\n")

        // Split by [split] if fragmentation logic is needed in the SSH tunnel
        // payloadParts = payload.split("[split]") // Implementation depends on SSH client

        return payload
    }
}
