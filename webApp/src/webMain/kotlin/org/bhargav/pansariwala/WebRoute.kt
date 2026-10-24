package org.bhargav.pansariwala

enum class WebRoute {
    Landing,
    UserApp,
    PosApp,
    MasterAdmin,
}

fun currentWebRoute(): WebRoute {
    val path = jsPathname().lowercase()
    return when {
        path.startsWith("/master") -> WebRoute.MasterAdmin
        path.startsWith("/app") -> WebRoute.UserApp
        path.startsWith("/pos") -> WebRoute.PosApp
        else -> WebRoute.Landing
    }
}
