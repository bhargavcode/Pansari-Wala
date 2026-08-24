package org.bhargav.pansariwala

enum class WebRoute {
    Landing,
    UserApp,
    MasterAdmin,
}

fun currentWebRoute(): WebRoute {
    val path = jsPathname().lowercase()
    return when {
        path.startsWith("/master") -> WebRoute.MasterAdmin
        path.startsWith("/app") -> WebRoute.UserApp
        else -> WebRoute.Landing
    }
}
