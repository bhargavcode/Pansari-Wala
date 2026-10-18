package org.bhargav.pansariwala.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.OfferScope
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopDetailsDto
import org.bhargav.pansariwala.api.ShopFeaturesDto
import org.bhargav.pansariwala.api.ShopHoursDayDto
import org.bhargav.pansariwala.api.ShopOfferDto
import org.bhargav.pansariwala.api.ShopProfileDto
import org.bhargav.pansariwala.api.ShopProfilePatch
import org.bhargav.pansariwala.api.ShopStaffDto
import org.bhargav.pansariwala.api.ShopStaffUpsert
import org.bhargav.pansariwala.api.VisitorStatsDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.auth.ShopRoles
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ShopReview
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppClock
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_invalid_number
import pansariwala.shared.generated.resources.error_required_fields
import pansariwala.shared.generated.resources.profile_saved

enum class ProfileTab(val permission: ShopPermission?) {
    SHOP(null),
    STAFF(ShopPermission.MANAGE_STAFF),
    OFFERS(ShopPermission.OFFERS),
    RATINGS(ShopPermission.RATINGS),
    VISITORS(ShopPermission.VISITORS),
}

enum class VisitorRange(val days: Int?) {
    TODAY(1),
    WEEK(7),
    MONTH(30),
    ALL(null),
}

data class ShopDetailsDraft(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val upiId: String = "",
    val deliveryRadiusKm: String = "",
    val discountPercent: String = "",
    val registrationNumber: String = "",
    val taxId: String = "",
    /** "HH:mm" or blank for all-day delivery. */
    val deliveryUntil: String = "",
)

data class StaffDraft(
    val id: String? = null,
    val displayName: String = "",
    val username: String = "",
    val password: String = "",
    val active: Boolean = true,
    val permissions: Set<ShopPermission> = setOf(ShopPermission.BILLING),
)

data class OfferDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val scope: String = OfferScope.BILL,
    val discountType: String = org.bhargav.pansariwala.api.OfferDiscountType.PERCENT,
    val value: String = "",
    val minBillAmount: String = "",
    val productIds: Set<String> = emptySet(),
    val active: Boolean = true,
    val productQuery: String = "",
)

data class ReplyDraft(val orderId: String, val text: String)

data class ShopProfileUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val profile: ShopProfileDto? = null,
    val access: ShopAccess = ShopAccess.FULL,
    val tab: ProfileTab = ProfileTab.SHOP,
    val staff: List<ShopStaffDto> = emptyList(),
    val offers: List<ShopOfferDto> = emptyList(),
    val products: List<Product> = emptyList(),
    val ratings: List<ShopReview> = emptyList(),
    val visitorRange: VisitorRange = VisitorRange.WEEK,
    val visitors: VisitorStatsDto? = null,
    val detailsDraft: ShopDetailsDraft? = null,
    val hoursDraft: List<ShopHoursDayDto>? = null,
    val staffDraft: StaffDraft? = null,
    val offerDraft: OfferDraft? = null,
    val replyDraft: ReplyDraft? = null,
    val confirmDeleteStaff: ShopStaffDto? = null,
    val confirmDeleteOffer: ShopOfferDto? = null,
) {
    val tabs: List<ProfileTab> get() = ProfileTab.entries.filter { it.permission == null || access.can(it.permission) }
}

class ShopProfileViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopProfileUiState())
    val uiState: StateFlow<ShopProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, access = preferences.getShopAccess()) }
            runCatching { api.shopProfile() }
                .onSuccess { profile ->
                    preferences.setShopAccess(profile.me.role, profile.me.permissions)
                    preferences.setShopFeatures(profile.shop.features)
                    _uiState.update {
                        it.copy(loading = false, profile = profile, access = preferences.getShopAccess())
                    }
                    loadTab(_uiState.value.tab)
                }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(loading = false, error = e.toApiUiText()) }
                }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(tab = tab) }
        loadTab(tab)
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun loadTab(tab: ProfileTab) {
        when (tab) {
            ProfileTab.SHOP -> Unit
            ProfileTab.STAFF -> request { _uiState.update { it.copy(staff = api.shopStaff()) } }
            ProfileTab.OFFERS -> request {
                val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
                val products = shopRepository.observeProducts(shopId).first()
                _uiState.update { it.copy(offers = api.shopManagedOffers(all = true), products = products) }
            }
            ProfileTab.RATINGS -> request { _uiState.update { it.copy(ratings = api.shopOwnRatings()) } }
            ProfileTab.VISITORS -> loadVisitors()
        }
    }

    fun setVisitorRange(range: VisitorRange) {
        _uiState.update { it.copy(visitorRange = range) }
        loadVisitors()
    }

    private fun loadVisitors() {
        val days = _uiState.value.visitorRange.days
        val from = days?.let { AppClock.startOfTodayMillis() - (it - 1) * MILLIS_PER_DAY }
        request { _uiState.update { it.copy(visitors = api.shopVisitors(from, null)) } }
    }

    // ---- Shop details & features ----

    fun startEditDetails() {
        val shop = _uiState.value.profile?.shop ?: return
        _uiState.update { it.copy(detailsDraft = shop.toDraft()) }
    }

    fun updateDetailsDraft(transform: (ShopDetailsDraft) -> ShopDetailsDraft) =
        _uiState.update { state -> state.copy(detailsDraft = state.detailsDraft?.let(transform)) }

    fun cancelEditDetails() = _uiState.update { it.copy(detailsDraft = null) }

    fun saveDetails() {
        val draft = _uiState.value.detailsDraft ?: return
        if (draft.name.isBlank()) return showError(UiText.res(Res.string.error_required_fields))
        val radius = draft.deliveryRadiusKm.toDoubleOrNull()
        val discount = draft.discountPercent.ifBlank { "0" }.toDoubleOrNull()
        if (radius == null || discount == null) return showError(UiText.res(Res.string.error_invalid_number))
        val patch = ShopProfilePatch(
            name = draft.name.trim(),
            address = draft.address.trim(),
            city = draft.city.trim(),
            state = draft.state.trim(),
            zip = draft.zip.trim(),
            ownerName = draft.ownerName.trim(),
            ownerPhone = draft.ownerPhone.trim(),
            ownerEmail = draft.ownerEmail.trim(),
            upiId = draft.upiId.trim(),
            deliveryRadiusKm = radius,
            discountPercent = discount,
            registrationNumber = draft.registrationNumber.trim(),
            taxId = draft.taxId.trim(),
            deliveryUntil = draft.deliveryUntil,
        )
        saveProfile(patch) { it.copy(detailsDraft = null) }
    }

    fun startEditHours() {
        val shop = _uiState.value.profile?.shop ?: return
        _uiState.update { it.copy(hoursDraft = shop.operatingHours.withFullWeek()) }
    }

    fun updateHoursDay(day: String, transform: (ShopHoursDayDto) -> ShopHoursDayDto) =
        _uiState.update { state ->
            state.copy(hoursDraft = state.hoursDraft?.map { if (it.day == day) transform(it) else it })
        }

    fun cancelEditHours() = _uiState.update { it.copy(hoursDraft = null) }

    fun saveHours() {
        val draft = _uiState.value.hoursDraft ?: return
        saveProfile(ShopProfilePatch(operatingHours = draft)) { it.copy(hoursDraft = null) }
    }

    fun setHomeDeliveryEnabled(enabled: Boolean) = saveProfile(ShopProfilePatch(homeDeliveryEnabled = enabled))

    fun setShopActive(active: Boolean) = saveProfile(ShopProfilePatch(active = active))
    fun setPaymentsEnabled(enabled: Boolean) = saveProfile(ShopProfilePatch(paymentsEnabled = enabled))

    private fun saveProfile(
        patch: ShopProfilePatch,
        after: (ShopProfileUiState) -> ShopProfileUiState = { it },
    ) = save {
        val profile = api.updateShopProfile(patch)
        preferences.setShopPaymentInfo(profile.shop.name, profile.shop.upiId)
        preferences.setShopFeatures(profile.shop.features)
        _uiState.update { after(it.copy(profile = profile)) }
    }

    // ---- Staff ----

    fun startAddStaff() = _uiState.update { it.copy(staffDraft = StaffDraft()) }

    fun startEditStaff(staff: ShopStaffDto) = _uiState.update {
        it.copy(
            staffDraft = StaffDraft(
                id = staff.id,
                displayName = staff.displayName,
                username = staff.username,
                active = staff.active,
                permissions = ShopPermission.parse(staff.permissions).let { perms ->
                    if (ShopPermission.ADMIN in perms) setOf(ShopPermission.ADMIN) else perms
                },
            ),
        )
    }

    fun updateStaffDraft(transform: (StaffDraft) -> StaffDraft) =
        _uiState.update { state -> state.copy(staffDraft = state.staffDraft?.let(transform)) }

    fun cancelStaffDraft() = _uiState.update { it.copy(staffDraft = null) }

    fun saveStaff() {
        val draft = _uiState.value.staffDraft ?: return
        val isNew = draft.id == null
        if (draft.displayName.isBlank() || (isNew && (draft.username.isBlank() || draft.password.isBlank()))) {
            return showError(UiText.res(Res.string.error_required_fields))
        }
        save {
            api.saveShopStaff(
                draft.id,
                ShopStaffUpsert(
                    username = draft.username.trim(),
                    displayName = draft.displayName.trim(),
                    password = draft.password,
                    permissions = draft.permissions.map { it.name },
                    active = draft.active,
                ),
            )
            _uiState.update { it.copy(staffDraft = null, staff = api.shopStaff()) }
        }
    }

    fun requestDeleteStaff(staff: ShopStaffDto?) = _uiState.update { it.copy(confirmDeleteStaff = staff) }

    fun confirmDeleteStaff() {
        val staff = _uiState.value.confirmDeleteStaff ?: return
        save {
            api.deleteShopStaff(staff.id)
            _uiState.update { it.copy(confirmDeleteStaff = null, staff = api.shopStaff()) }
        }
    }

    // ---- Offers ----

    fun startAddOffer() = _uiState.update { it.copy(offerDraft = OfferDraft()) }

    fun startEditOffer(offer: ShopOfferDto) = _uiState.update {
        it.copy(
            offerDraft = OfferDraft(
                id = offer.id,
                title = offer.title,
                description = offer.description,
                scope = offer.scope,
                discountType = offer.discountType,
                value = offer.value.toPlainString(),
                minBillAmount = offer.minBillAmount.takeIf { v -> v > 0.0 }?.toPlainString().orEmpty(),
                productIds = offer.productIds.toSet(),
                active = offer.active,
            ),
        )
    }

    fun updateOfferDraft(transform: (OfferDraft) -> OfferDraft) =
        _uiState.update { state -> state.copy(offerDraft = state.offerDraft?.let(transform)) }

    fun cancelOfferDraft() = _uiState.update { it.copy(offerDraft = null) }

    fun saveOffer() {
        val draft = _uiState.value.offerDraft ?: return
        val value = draft.value.toDoubleOrNull()
        val minBill = draft.minBillAmount.ifBlank { "0" }.toDoubleOrNull()
        if (draft.title.isBlank()) return showError(UiText.res(Res.string.error_required_fields))
        if (value == null || value <= 0.0 || minBill == null) return showError(UiText.res(Res.string.error_invalid_number))
        val body = ShopOfferDto(
            title = draft.title.trim(),
            description = draft.description.trim(),
            scope = draft.scope,
            discountType = draft.discountType,
            value = value,
            productIds = draft.productIds.toList(),
            minBillAmount = minBill,
            active = draft.active,
        )
        save {
            api.saveShopOffer(draft.id, body)
            _uiState.update { it.copy(offerDraft = null, offers = api.shopManagedOffers(all = true)) }
        }
    }

    fun setOfferActive(offer: ShopOfferDto, active: Boolean) = save {
        api.saveShopOffer(offer.id, offer.copy(active = active))
        _uiState.update { it.copy(offers = api.shopManagedOffers(all = true)) }
    }

    fun requestDeleteOffer(offer: ShopOfferDto?) = _uiState.update { it.copy(confirmDeleteOffer = offer) }

    fun confirmDeleteOffer() {
        val offer = _uiState.value.confirmDeleteOffer ?: return
        save {
            api.deleteShopOffer(offer.id)
            _uiState.update { it.copy(confirmDeleteOffer = null, offers = api.shopManagedOffers(all = true)) }
        }
    }

    // ---- Ratings ----

    fun startReply(review: ShopReview) =
        _uiState.update { it.copy(replyDraft = ReplyDraft(review.id, review.reply.orEmpty())) }

    fun updateReply(text: String) =
        _uiState.update { state -> state.copy(replyDraft = state.replyDraft?.copy(text = text)) }

    fun cancelReply() = _uiState.update { it.copy(replyDraft = null) }

    fun saveReply() {
        val draft = _uiState.value.replyDraft ?: return
        if (draft.text.isBlank()) return showError(UiText.res(Res.string.error_required_fields))
        save {
            val updated = api.replyToRating(draft.orderId, draft.text.trim())
            _uiState.update { state ->
                state.copy(
                    replyDraft = null,
                    ratings = state.ratings.map { if (it.id == updated.id) updated else it },
                )
            }
        }
    }

    private fun showError(text: UiText) = _uiState.update { it.copy(error = text) }

    private fun request(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                _uiState.update { it.copy(error = e.toApiUiText()) }
            }
        }
    }

    private fun save(block: suspend () -> Unit) {
        if (_uiState.value.saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            runCatching { block() }
                .onSuccess { _uiState.update { it.copy(saving = false, message = UiText.res(Res.string.profile_saved)) } }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(saving = false, error = e.toApiUiText()) }
                }
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

fun ShopStaffDto.isShopAdmin(): Boolean = role != ShopRoles.EXECUTIVE

private val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Always all seven days so saving never silently drops (and closes) a missing day. */
private fun List<ShopHoursDayDto>.withFullWeek(): List<ShopHoursDayDto> {
    val byDay = associateBy { it.day.take(3).lowercase() }
    return WEEK_DAYS.map { day -> byDay[day.lowercase()]?.copy(day = day) ?: ShopHoursDayDto(day) }
}

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else toString()

private fun ShopDetailsDto.toDraft() = ShopDetailsDraft(
    name = name,
    address = address,
    city = city,
    state = state,
    zip = zip,
    ownerName = ownerName,
    ownerPhone = ownerPhone,
    ownerEmail = ownerEmail,
    upiId = upiId,
    deliveryRadiusKm = deliveryRadiusKm.toPlainString(),
    discountPercent = discountPercent.toPlainString(),
    registrationNumber = registrationNumber,
    taxId = taxId,
    deliveryUntil = deliveryUntil,
)
