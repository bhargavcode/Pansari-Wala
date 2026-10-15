package org.bhargav.pansariwala.printer.transport.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.net.Inet4Address
import java.net.NetworkInterface

internal actual fun localIpv4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
        .flatMap { iface -> iface.inetAddresses.toList().filterIsInstance<Inet4Address>() }
        .mapNotNull { it.hostAddress }
        .filter(::isPrivateIpv4)
        .distinct()
}.getOrDefault(emptyList())

internal actual val printerIoDispatcher: CoroutineDispatcher = Dispatchers.IO
