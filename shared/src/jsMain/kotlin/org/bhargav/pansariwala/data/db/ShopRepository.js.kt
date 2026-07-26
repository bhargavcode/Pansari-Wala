package org.bhargav.pansariwala.data.db

actual fun createShopRepository(): ShopRepository = InMemoryShopRepository()
