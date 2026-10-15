package org.bhargav.pansariwala.printer.transport.network

import kotlinx.coroutines.CoroutineDispatcher

/** Private (site-local) IPv4 addresses of active Wi-Fi / Ethernet interfaces, best candidate first. */
internal expect fun localIpv4Addresses(): List<String>

internal expect val printerIoDispatcher: CoroutineDispatcher

internal fun isPrivateIpv4(ip: String): Boolean {
    val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return parts[0] == 10 ||
        (parts[0] == 172 && parts[1] in 16..31) ||
        (parts[0] == 192 && parts[1] == 168)
}
