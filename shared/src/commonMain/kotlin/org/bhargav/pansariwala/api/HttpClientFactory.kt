package org.bhargav.pansariwala.api

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
