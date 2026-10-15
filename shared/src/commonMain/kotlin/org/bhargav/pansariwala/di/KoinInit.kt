package org.bhargav.pansariwala.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    platformModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {},
) {
    val app = startKoin {
        appDeclaration()
        modules(listOf(appModule, platformModule) + platformModules)
    }
    // Opens DNS + TLS to the API while splash renders so the first real call skips the handshake.
    val api = app.koin.get<PansariApi>()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { api.warmUp() }
}
