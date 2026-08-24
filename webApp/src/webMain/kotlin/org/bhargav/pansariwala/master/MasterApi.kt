package org.bhargav.pansariwala.master

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.api.ApiRuntime

class MasterApi(private val baseUrl: String = ApiRuntime.baseUrl) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = createPlatformHttpClient().config {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
    }

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    suspend fun login(username: String, password: String): TokenDto =
        client.post(url("/auth/admin/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(username, password))
        }.body()

    suspend fun products(token: String): List<ProductDto> =
        client.get(url("/master/products")) { bearer(token) }.body()

    suspend fun categories(token: String): List<CategoryDto> =
        client.get(url("/master/categories")) { bearer(token) }.body()

    suspend fun shopTypes(token: String): List<ShopTypeDto> =
        client.get(url("/master/shop-types")) { bearer(token) }.body()

    suspend fun shops(token: String): List<ShopDto> =
        client.get(url("/admin/shops")) { bearer(token) }.body()

    suspend fun saveProduct(token: String, body: ProductUpsert): ProductDto =
        client.post(url("/admin/master/products")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteProduct(token: String, id: String) {
        client.delete(url("/admin/master/products/$id")) { bearer(token) }
    }

    suspend fun saveCategory(token: String, body: CategoryUpsert): CategoryDto =
        client.post(url("/admin/master/categories")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteCategory(token: String, id: String) {
        client.delete(url("/admin/master/categories/$id")) { bearer(token) }
    }

    suspend fun saveShopType(token: String, body: ShopTypeUpsert): ShopTypeDto =
        client.post(url("/admin/master/shop-types")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteShopType(token: String, id: String) {
        client.delete(url("/admin/master/shop-types/$id")) { bearer(token) }
    }

    suspend fun patchShop(token: String, id: String, active: Boolean?, paymentsEnabled: Boolean?) {
        client.post(url("/admin/shops/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ShopPatch(active, paymentsEnabled))
        }
    }

    suspend fun createShop(token: String, body: ShopCreate): ShopDto =
        client.post(url("/admin/shops")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        headers.append("Authorization", "Bearer $token")
    }
}

@Serializable data class LoginBody(val username: String, val password: String)
@Serializable data class TokenDto(val accessToken: String, val role: String = "ADMIN")
@Serializable data class ProductDto(
    val id: String,
    val name: String,
    val nameHi: String = "",
    val categoryId: String,
    val unit: String = "KG",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
)
@Serializable data class ProductUpsert(
    val id: String? = null,
    val name: String,
    val nameHi: String = "",
    val categoryId: String,
    val unit: String = "KG",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
)
@Serializable data class CategoryDto(val id: String, val name: String, val parentId: String? = null)
@Serializable data class CategoryUpsert(val id: String? = null, val name: String, val parentId: String? = null)
@Serializable data class ShopTypeDto(val id: String, val name: String, val active: Boolean = true)
@Serializable data class ShopTypeUpsert(val id: String? = null, val name: String, val active: Boolean = true)
@Serializable data class ShopDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val shopType: String = "GENERAL_STORE",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)
@Serializable data class ShopPatch(val active: Boolean? = null, val paymentsEnabled: Boolean? = null)
@Serializable data class ShopCreate(
    val name: String,
    val shopType: String = "GENERAL_STORE",
    val address: String = "",
    val lat: Double = 28.6139,
    val lng: Double = 77.2090,
    val active: Boolean = true,
)
