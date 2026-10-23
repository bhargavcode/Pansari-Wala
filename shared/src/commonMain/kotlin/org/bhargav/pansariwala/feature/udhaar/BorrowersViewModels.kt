package org.bhargav.pansariwala.feature.udhaar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.BorrowerDetailDto
import org.bhargav.pansariwala.api.BorrowerDto
import org.bhargav.pansariwala.api.BorrowerReportDto
import org.bhargav.pansariwala.api.BorrowerUpdateRequest
import org.bhargav.pansariwala.api.CreditTxnDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.RepaymentRequest
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.pickImageCatching
import org.bhargav.pansariwala.platform.toImagePickErrorText
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import io.ktor.http.encodeURLParameter
import org.bhargav.pansariwala.platform.e164Phone
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.udhaar_remind_default_template
import pansariwala.shared.generated.resources.udhaar_remind_sent
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_photo_pick_failed
import pansariwala.shared.generated.resources.error_udhaar_amount
import pansariwala.shared.generated.resources.udhaar_repay_done
import pansariwala.shared.generated.resources.udhaar_saved
import kotlin.time.Instant

private const val SEARCH_DEBOUNCE_MS = 350L
private const val SECONDS_PER_MINUTE = 60
private const val MONEY_EPSILON = 0.005

enum class UdhaarRange(val days: Int) { TODAY(1), WEEK(7), MONTH(30), QUARTER(90), YEAR(365) }

enum class UdhaarTxnFilter { ALL, PURCHASES, PAYMENTS }

fun localUtcOffsetMinutes(): Int =
    TimeZone.currentSystemDefault().offsetAt(Instant.fromEpochMilliseconds(AppClock.nowMillis())).totalSeconds /
        SECONDS_PER_MINUTE

/** Local midnight [daysAgo] days before today. */
fun localDayStartMs(daysAgo: Int): Long {
    val tz = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(tz).date
    return today.minus(daysAgo, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
}

fun List<CreditTxnDto>.filterBy(filter: UdhaarTxnFilter): List<CreditTxnDto> = when (filter) {
    UdhaarTxnFilter.ALL -> this
    UdhaarTxnFilter.PURCHASES -> filter { it.type == AppConstants.Udhaar.TYPE_CREDIT }
    UdhaarTxnFilter.PAYMENTS -> filter { it.type == AppConstants.Udhaar.TYPE_REPAYMENT }
}

/** Parses a rupee amount and checks it is within (0, [max]]. */
fun parseRepayAmount(raw: String, max: Double): Double? =
    raw.trim().toDoubleOrNull()?.takeIf { it > 0.0 && it <= max + MONEY_EPSILON }

// ---------- Report ----------

data class BorrowersUiState(
    val range: UdhaarRange = UdhaarRange.MONTH,
    val query: String = "",
    val sort: String = AppConstants.Udhaar.SORT_OUTSTANDING,
    val report: BorrowerReportDto? = null,
    val borrowers: List<BorrowerDto> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: UiText? = null,
)

class BorrowersViewModel(private val api: PansariApi) : ViewModel() {
    private val _state = MutableStateFlow(BorrowersUiState())
    val state: StateFlow<BorrowersUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun refresh() = load()

    fun setRange(range: UdhaarRange) {
        if (range == _state.value.range) return
        _state.update { it.copy(range = range) }
        load()
    }

    fun setSort(sort: String) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        load()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        load(debounce = true)
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.loadingMore || s.loading) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching { fetch(s, s.page + 1) }
                .onSuccess { report ->
                    _state.update {
                        it.copy(
                            borrowers = it.borrowers + report.borrowers,
                            page = s.page + 1,
                            hasMore = report.hasMore,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loadingMore = false, error = err.toApiUiText()) }
                }
        }
    }

    private fun load(debounce: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(loading = true, error = null) }
            val s = _state.value
            runCatching { fetch(s, 0) }
                .onSuccess { report ->
                    _state.update {
                        it.copy(
                            report = report,
                            borrowers = report.borrowers,
                            page = 0,
                            hasMore = report.hasMore,
                            loading = false,
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    private suspend fun fetch(s: BorrowersUiState, page: Int): BorrowerReportDto = api.borrowerReport(
        fromMs = localDayStartMs(s.range.days - 1),
        toMs = AppClock.nowMillis(),
        tzOffsetMin = localUtcOffsetMinutes(),
        query = s.query.trim(),
        sort = s.sort,
        page = page,
        size = AppConstants.Udhaar.PAGE_SIZE,
    )
}

// ---------- Detail ----------

data class RepayForm(
    val amount: String = "",
    val mode: String = AppConstants.PosPayment.CASH,
    val receivedBy: String = "",
    val note: String = "",
    val busy: Boolean = false,
    val error: UiText? = null,
)

enum class PayRequestChannel { WHATSAPP, SMS }

/** "Request payment" sheet; [template] holds the editable text with [AppConstants.Udhaar] tokens. */
data class PayRequestForm(
    val amount: String = "",
    val template: String = "",
    val defaultTemplate: String = "",
    val busy: Boolean = false,
    val error: UiText? = null,
)

data class BorrowerEditForm(
    val name: String = "",
    val address: String = "",
    val notes: String = "",
    val busy: Boolean = false,
)

enum class BorrowerDetailTab { ORDERS, TRANSACTIONS }

/** One paged list on the borrower screen; [page] is the last loaded page (-1 = none yet). */
data class CreditTxnPage(
    val items: List<CreditTxnDto> = emptyList(),
    val page: Int = -1,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val totalCount: Int = 0,
)

data class BorrowerDetailUiState(
    val detail: BorrowerDetailDto? = null,
    val tab: BorrowerDetailTab = BorrowerDetailTab.ORDERS,
    val orders: CreditTxnPage = CreditTxnPage(),
    val txns: CreditTxnPage = CreditTxnPage(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val filter: UdhaarTxnFilter = UdhaarTxnFilter.ALL,
    val staff: List<String> = emptyList(),
    val repay: RepayForm? = null,
    val payRequest: PayRequestForm? = null,
    /** WhatsApp / SMS deep link ready to open; the screen opens it once and clears it. */
    val shareUri: String? = null,
    val edit: BorrowerEditForm? = null,
    val photoUploading: Boolean = false,
    val photoPreview: ByteArray? = null,
    val message: UiText? = null,
)

class BorrowerDetailViewModel(
    private val phone: String,
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val imagePicker: ImagePicker,
) : ViewModel() {
    private val _state = MutableStateFlow(BorrowerDetailUiState())
    val state: StateFlow<BorrowerDetailUiState> = _state.asStateFlow()
    private var photoJob: Job? = null

    init {
        refresh()
        loadStaff()
    }

    private var ordersJob: Job? = null
    private var txnsJob: Job? = null

    fun refresh() {
        _state.update { it.copy(loading = true, error = null, orders = CreditTxnPage(), txns = CreditTxnPage()) }
        loadPage(BorrowerDetailTab.ORDERS, reset = true)
        loadPage(BorrowerDetailTab.TRANSACTIONS, reset = true)
    }

    fun selectTab(tab: BorrowerDetailTab) = _state.update { it.copy(tab = tab) }

    fun loadMore(tab: BorrowerDetailTab) {
        val page = _state.value.page(tab)
        if (!page.hasMore || page.loading) return
        loadPage(tab, reset = false)
    }

    private fun BorrowerDetailUiState.page(tab: BorrowerDetailTab) =
        if (tab == BorrowerDetailTab.ORDERS) orders else txns

    private fun BorrowerDetailUiState.withPage(tab: BorrowerDetailTab, page: CreditTxnPage) =
        if (tab == BorrowerDetailTab.ORDERS) copy(orders = page) else copy(txns = page)

    private fun loadPage(tab: BorrowerDetailTab, reset: Boolean) {
        val type = when (tab) {
            BorrowerDetailTab.ORDERS -> AppConstants.Udhaar.TYPE_CREDIT
            BorrowerDetailTab.TRANSACTIONS -> when (_state.value.filter) {
                UdhaarTxnFilter.ALL -> null
                UdhaarTxnFilter.PURCHASES -> AppConstants.Udhaar.TYPE_CREDIT
                UdhaarTxnFilter.PAYMENTS -> AppConstants.Udhaar.TYPE_REPAYMENT
            }
        }
        val current = if (reset) CreditTxnPage() else _state.value.page(tab)
        val nextPage = current.page + 1
        _state.update { it.withPage(tab, current.copy(loading = true)) }
        val job = viewModelScope.launch {
            runCatching { api.borrowerDetail(phone, type, nextPage, AppConstants.Udhaar.PAGE_SIZE) }
                .onSuccess { detail ->
                    _state.update { s ->
                        val base = s.page(tab)
                        s.withPage(
                            tab,
                            CreditTxnPage(
                                items = if (nextPage == 0) detail.transactions else base.items + detail.transactions,
                                page = nextPage,
                                hasMore = detail.hasMore,
                                loading = false,
                                totalCount = detail.totalCount,
                            ),
                        ).copy(detail = detail, loading = false)
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { s ->
                        s.withPage(tab, s.page(tab).copy(loading = false)).copy(loading = false, error = err.toApiUiText())
                    }
                }
        }
        if (tab == BorrowerDetailTab.ORDERS) {
            ordersJob?.cancel(); ordersJob = job
        } else {
            txnsJob?.cancel(); txnsJob = job
        }
    }

    private fun loadStaff() {
        viewModelScope.launch {
            val me = preferences.getDisplayName().orEmpty()
            val staff = runCatching { api.shopStaff() }
                .onFailure { it.rethrowIfStructuredCancellation() }
                .getOrDefault(emptyList())
                .filter { it.active }
                .map { it.displayName.ifBlank { it.username } }
            _state.update { it.copy(staff = (listOf(me) + staff).filter(String::isNotBlank).distinct()) }
        }
    }

    fun setFilter(filter: UdhaarTxnFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        loadPage(BorrowerDetailTab.TRANSACTIONS, reset = true)
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // payment request (WhatsApp / SMS reminder with a UPI link)
    fun openPayRequest() {
        viewModelScope.launch {
            val default = getString(Res.string.udhaar_remind_default_template)
            val saved = preferences.getUdhaarReminderTemplate()
            _state.update { s ->
                val due = s.detail?.borrower?.outstanding ?: 0.0
                s.copy(
                    payRequest = PayRequestForm(
                        amount = due.takeIf { it > 0 }?.let(::formatAmount).orEmpty(),
                        template = saved.ifBlank { default },
                        defaultTemplate = default,
                    ),
                )
            }
        }
    }

    fun closePayRequest() = _state.update { it.copy(payRequest = null) }

    fun updatePayRequest(transform: (PayRequestForm) -> PayRequestForm) =
        _state.update { s -> s.copy(payRequest = s.payRequest?.let(transform)?.copy(error = null)) }

    fun consumeShareUri() = _state.update { it.copy(shareUri = null) }

    fun sendPayRequest(channel: PayRequestChannel) {
        val s = _state.value
        val form = s.payRequest ?: return
        val borrower = s.detail?.borrower ?: return
        val amount = parseRepayAmount(form.amount, borrower.outstanding)
        if (amount == null) {
            _state.update { it.copy(payRequest = form.copy(error = UiText.res(Res.string.error_udhaar_amount))) }
            return
        }
        _state.update { it.copy(payRequest = form.copy(busy = true)) }
        viewModelScope.launch {
            runCatching { api.createPayLink(phone, amount) }
                .onSuccess { link ->
                    val template = form.template.trim()
                    preferences.setUdhaarReminderTemplate(if (template == form.defaultTemplate.trim()) "" else template)
                    val message = fillReminderTemplate(
                        template = template.ifBlank { form.defaultTemplate },
                        name = borrower.name.ifBlank { borrower.phone },
                        amount = amount.asMoney(),
                        due = borrower.outstanding.asMoney(),
                        shop = preferences.getShopName(),
                        link = link.url,
                    )
                    _state.update {
                        it.copy(
                            payRequest = null,
                            shareUri = payRequestUri(channel, borrower.phone, message),
                            message = UiText.res(Res.string.udhaar_remind_sent),
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { st -> st.copy(payRequest = st.payRequest?.copy(busy = false, error = err.toApiUiText())) }
                }
        }
    }

    // repayment
    fun openRepay() = _state.update { s ->
        val due = s.detail?.borrower?.outstanding ?: 0.0
        s.copy(repay = RepayForm(amount = due.takeIf { it > 0 }?.let(::formatAmount).orEmpty(), receivedBy = s.staff.firstOrNull().orEmpty()))
    }

    fun closeRepay() = _state.update { it.copy(repay = null) }

    fun updateRepay(transform: (RepayForm) -> RepayForm) =
        _state.update { s -> s.copy(repay = s.repay?.let(transform)?.copy(error = null)) }

    fun submitRepay() {
        val s = _state.value
        val form = s.repay ?: return
        val due = s.detail?.borrower?.outstanding ?: 0.0
        val amount = parseRepayAmount(form.amount, due)
        if (amount == null) {
            _state.update { it.copy(repay = form.copy(error = UiText.res(Res.string.error_udhaar_amount))) }
            return
        }
        _state.update { it.copy(repay = form.copy(busy = true)) }
        viewModelScope.launch {
            runCatching {
                api.recordRepayment(
                    phone,
                    RepaymentRequest(amount = amount, paymentMode = form.mode, receivedBy = form.receivedBy, note = form.note.trim()),
                )
            }.onSuccess {
                _state.update { it.copy(repay = null, message = UiText.res(Res.string.udhaar_repay_done, amount.asMoney())) }
                refresh()
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                _state.update { st -> st.copy(repay = st.repay?.copy(busy = false, error = err.toApiUiText())) }
            }
        }
    }

    // info edit
    fun openEdit() = _state.update { s ->
        val b = s.detail?.borrower ?: return@update s
        s.copy(edit = BorrowerEditForm(name = b.name, address = b.address, notes = b.notes))
    }

    fun closeEdit() = _state.update { it.copy(edit = null) }

    fun updateEdit(transform: (BorrowerEditForm) -> BorrowerEditForm) =
        _state.update { s -> s.copy(edit = s.edit?.let(transform)) }

    fun saveEdit() {
        val form = _state.value.edit ?: return
        val current = _state.value.detail?.borrower ?: return
        _state.update { it.copy(edit = form.copy(busy = true)) }
        viewModelScope.launch {
            runCatching {
                api.updateBorrower(
                    phone,
                    BorrowerUpdateRequest(
                        name = form.name.trim(),
                        address = form.address.trim(),
                        notes = form.notes.trim(),
                        photoUrl = current.photoUrl,
                    ),
                )
            }.onSuccess { updated ->
                _state.update { s ->
                    s.copy(
                        edit = null,
                        detail = s.detail?.copy(borrower = updated),
                        message = UiText.res(Res.string.udhaar_saved),
                    )
                }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                _state.update { s -> s.copy(edit = s.edit?.copy(busy = false), error = err.toApiUiText()) }
            }
        }
    }

    fun changePhoto(source: ImageSource) {
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse { err ->
                _state.update { it.copy(error = err.toImagePickErrorText()) }
                return@launch
            } ?: return@launch
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse {
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            _state.update { it.copy(photoUploading = true, photoPreview = compressed.previewBytes) }
            runCatching {
                val url = api.uploadImage(
                    prefix = ImageUploadFeature.BORROWER_PHOTO.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
                val b = _state.value.detail?.borrower
                api.updateBorrower(
                    phone,
                    BorrowerUpdateRequest(
                        name = b?.name.orEmpty(),
                        address = b?.address.orEmpty(),
                        notes = b?.notes.orEmpty(),
                        photoUrl = url,
                    ),
                )
            }.onSuccess { updated ->
                _state.update { s -> s.copy(photoUploading = false, detail = s.detail?.copy(borrower = updated)) }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                _state.update {
                    it.copy(photoUploading = false, photoPreview = null, error = UiText.res(Res.string.error_image_upload_failed))
                }
            }
        }
    }

}

internal fun formatAmount(value: Double): String {
    val paise = kotlin.math.round(value * 100).toLong()
    return if (paise % 100 == 0L) (paise / 100).toString() else (paise / 100.0).toString()
}

/** Replaces the [AppConstants.Udhaar] tokens; the link is appended when the shopkeeper removed its token. */
internal fun fillReminderTemplate(template: String, name: String, amount: String, due: String, shop: String, link: String): String {
    val filled = template
        .replace(AppConstants.Udhaar.TOKEN_NAME, name)
        .replace(AppConstants.Udhaar.TOKEN_AMOUNT, amount)
        .replace(AppConstants.Udhaar.TOKEN_DUE, due)
        .replace(AppConstants.Udhaar.TOKEN_SHOP, shop)
    return if (AppConstants.Udhaar.TOKEN_LINK in filled) {
        filled.replace(AppConstants.Udhaar.TOKEN_LINK, link)
    } else {
        "$filled\n$link"
    }
}

private fun payRequestUri(channel: PayRequestChannel, phone: String, message: String): String {
    val e164 = e164Phone(phone)
    val body = message.encodeURLParameter()
    return when (channel) {
        PayRequestChannel.WHATSAPP -> AppConstants.WHATSAPP_CHAT_URL + e164.removePrefix("+") + "?text=" + body
        // "?&body=" is understood by both Android and iOS messaging apps.
        PayRequestChannel.SMS -> "sms:$e164?&body=$body"
    }
}
