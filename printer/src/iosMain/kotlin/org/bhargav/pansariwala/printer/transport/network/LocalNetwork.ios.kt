package org.bhargav.pansariwala.printer.transport.network

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.INET_ADDRSTRLEN
import platform.posix.IFF_LOOPBACK
import platform.posix.IFF_UP
import platform.posix.sockaddr_in

@OptIn(ExperimentalForeignApi::class)
internal actual fun localIpv4Addresses(): List<String> = memScoped {
    val head = alloc<CPointerVar<ifaddrs>>()
    if (getifaddrs(head.ptr) != 0) return@memScoped emptyList()
    val found = mutableListOf<Pair<String, String>>()
    try {
        var cursor = head.value
        while (cursor != null) {
            val ifa = cursor.pointed
            val addr = ifa.ifa_addr
            val flags = ifa.ifa_flags.toInt()
            if (addr != null &&
                addr.pointed.sa_family.toInt() == AF_INET &&
                flags and IFF_UP != 0 &&
                flags and IFF_LOOPBACK == 0
            ) {
                val buffer = allocArray<ByteVar>(INET_ADDRSTRLEN)
                val sin = addr.reinterpret<sockaddr_in>()
                if (inet_ntop(AF_INET, sin.pointed.sin_addr.ptr, buffer, INET_ADDRSTRLEN.convert()) != null) {
                    found += (ifa.ifa_name?.toKString().orEmpty()) to buffer.toKString()
                }
            }
            cursor = ifa.ifa_next
        }
    } finally {
        freeifaddrs(head.value)
    }
    found.sortedBy { if (it.first == "en0") 0 else 1 }
        .map { it.second }
        .filter(::isPrivateIpv4)
        .distinct()
}

internal actual val printerIoDispatcher: CoroutineDispatcher = Dispatchers.IO
