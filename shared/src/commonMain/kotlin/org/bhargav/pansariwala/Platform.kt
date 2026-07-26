package org.bhargav.pansariwala

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform