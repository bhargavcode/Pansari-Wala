package org.bhargav.pansariwala.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.TimePickerField
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_cashfree_vendor_hint
import pansariwala.shared.generated.resources.master_cashfree_vendor_id
import pansariwala.shared.generated.resources.shop_min_order_label
import pansariwala.shared.generated.resources.shop_min_order_hint
import pansariwala.shared.generated.resources.shop_min_order_invalid
import pansariwala.shared.generated.resources.shop_free_delivery_label
import pansariwala.shared.generated.resources.shop_free_delivery_amount
import pansariwala.shared.generated.resources.shop_free_delivery_invalid
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_add_new_shop_subtitle
import pansariwala.shared.generated.resources.master_add_new_shop_title
import pansariwala.shared.generated.resources.master_address_details
import pansariwala.shared.generated.resources.master_business_details
import pansariwala.shared.generated.resources.master_cancel
import pansariwala.shared.generated.resources.master_city
import pansariwala.shared.generated.resources.master_country
import pansariwala.shared.generated.resources.master_create_shop
import pansariwala.shared.generated.resources.master_edit_shop_title
import pansariwala.shared.generated.resources.master_feature_barcode
import pansariwala.shared.generated.resources.master_feature_inventory
import pansariwala.shared.generated.resources.master_feature_online
import pansariwala.shared.generated.resources.master_delivery_payments
import pansariwala.shared.generated.resources.master_delivery_until_help
import pansariwala.shared.generated.resources.master_err_delivery_radius
import pansariwala.shared.generated.resources.master_err_delivery_until
import pansariwala.shared.generated.resources.master_err_discount
import pansariwala.shared.generated.resources.master_placeholder_upi
import pansariwala.shared.generated.resources.shop_delivery_until_clear
import pansariwala.shared.generated.resources.shop_delivery_until_none
import pansariwala.shared.generated.resources.shop_home_delivery_hint
import pansariwala.shared.generated.resources.shop_home_delivery_label
import pansariwala.shared.generated.resources.profile_field_discount
import pansariwala.shared.generated.resources.profile_field_radius
import pansariwala.shared.generated.resources.profile_field_upi
import pansariwala.shared.generated.resources.profile_payments_enabled
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.shop_delivery_until_label
import pansariwala.shared.generated.resources.master_feature_reports
import pansariwala.shared.generated.resources.master_feature_scanner
import pansariwala.shared.generated.resources.master_feature_voice
import pansariwala.shared.generated.resources.master_features
import pansariwala.shared.generated.resources.master_general_info
import pansariwala.shared.generated.resources.master_hours_end
import pansariwala.shared.generated.resources.master_hours_start
import pansariwala.shared.generated.resources.master_image_url
import pansariwala.shared.generated.resources.master_inactive
import pansariwala.shared.generated.resources.master_operating_hours
import pansariwala.shared.generated.resources.master_owner_email
import pansariwala.shared.generated.resources.master_owner_name
import pansariwala.shared.generated.resources.master_owner_phone
import pansariwala.shared.generated.resources.master_placeholder_city
import pansariwala.shared.generated.resources.master_placeholder_email
import pansariwala.shared.generated.resources.master_placeholder_owner
import pansariwala.shared.generated.resources.master_placeholder_phone
import pansariwala.shared.generated.resources.master_placeholder_reg
import pansariwala.shared.generated.resources.master_placeholder_shop_name
import pansariwala.shared.generated.resources.master_placeholder_state
import pansariwala.shared.generated.resources.master_placeholder_street
import pansariwala.shared.generated.resources.master_placeholder_tax
import pansariwala.shared.generated.resources.master_placeholder_zip
import pansariwala.shared.generated.resources.master_registration_number
import pansariwala.shared.generated.resources.master_save_changes
import pansariwala.shared.generated.resources.master_shop_name
import pansariwala.shared.generated.resources.master_shop_type
import pansariwala.shared.generated.resources.master_state
import pansariwala.shared.generated.resources.master_status
import pansariwala.shared.generated.resources.master_street_address
import pansariwala.shared.generated.resources.master_tax_id
import pansariwala.shared.generated.resources.master_upload_shop_image
import pansariwala.shared.generated.resources.master_zip
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.master_err_login_password
import pansariwala.shared.generated.resources.master_err_login_username
import pansariwala.shared.generated.resources.master_err_owner_email
import pansariwala.shared.generated.resources.master_err_owner_phone
import pansariwala.shared.generated.resources.master_err_shop_address
import pansariwala.shared.generated.resources.master_err_shop_location
import pansariwala.shared.generated.resources.master_err_shop_name
import pansariwala.shared.generated.resources.master_login_password
import pansariwala.shared.generated.resources.master_login_password_reset
import pansariwala.shared.generated.resources.master_login_username
import pansariwala.shared.generated.resources.master_shop_login
import pansariwala.shared.generated.resources.master_shop_login_hint
import pansariwala.shared.generated.resources.master_choose_from_device
import pansariwala.shared.generated.resources.master_remove_image
import pansariwala.shared.generated.resources.master_upload_failed
import pansariwala.shared.generated.resources.master_upload_unsupported_image
import pansariwala.shared.generated.resources.shop_type_sweet_shop
import pansariwala.shared.generated.resources.shop_type_hardware
import pansariwala.shared.generated.resources.shop_type_medical_store
import pansariwala.shared.generated.resources.shop_type_general_store
import pansariwala.shared.generated.resources.shop_type_boutique
import pansariwala.shared.generated.resources.shop_type_grocery
import pansariwala.shared.generated.resources.shop_type_supermarket

/** Offline fallback; the live list comes from `/master/shop-types`. */
private val DefaultShopTypeChoices = ShopType.entries.map {
    ShopTypeDto(it.apiValue, it.apiValue.lowercase().split('_').joinToString(" ") { w -> w.replaceFirstChar(Char::uppercase) })
}

private const val MIN_SHOP_PASSWORD = 4
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopEditorScreen(
    token: String,
    shopId: String?,
    onNavigate: (MasterDest) -> Unit,
    onStatus: (String) -> Unit,
) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    val isEdit = shopId != null
    var name by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var ownerPhone by rememberSaveable { mutableStateOf("") }
    var ownerEmail by rememberSaveable { mutableStateOf("") }
    var shopType by rememberSaveable { mutableStateOf(ShopType.GROCERY.apiValue) }
    var loginUsername by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var zip by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("India") }
    var registration by rememberSaveable { mutableStateOf("") }
    var taxId by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var uploadingImage by remember { mutableStateOf(false) }
    var lat by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_MAP_LAT) }
    var lng by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_MAP_LNG) }
    var active by rememberSaveable { mutableStateOf(true) }
    var voice by rememberSaveable { mutableStateOf(true) }
    var barcode by rememberSaveable { mutableStateOf(true) }
    var online by rememberSaveable { mutableStateOf(true) }
    var inventory by rememberSaveable { mutableStateOf(true) }
    var reports by rememberSaveable { mutableStateOf(true) }
    var scanner by rememberSaveable { mutableStateOf(false) }
    var paymentsEnabled by rememberSaveable { mutableStateOf(true) }
    var upiId by rememberSaveable { mutableStateOf("") }
    var vendorId by rememberSaveable { mutableStateOf("") }
    var radiusKm by rememberSaveable { mutableStateOf("") }
    var discountPct by rememberSaveable { mutableStateOf("") }
    var deliveryUntil by rememberSaveable { mutableStateOf("") }
    var homeDelivery by rememberSaveable { mutableStateOf(true) }
    var minOrder by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_SHOP_MIN_ORDER_INR.plainNumber()) }
    var freeDelivery by rememberSaveable { mutableStateOf(false) }
    var freeDeliveryAbove by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_SHOP_FREE_DELIVERY_ABOVE_INR.plainNumber()) }
    var hours by remember { mutableStateOf(WEEK_DAYS.map { ShopHoursDayDto(it) }) }
    var shopTypes by remember { mutableStateOf(DefaultShopTypeChoices) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(token, shopId) {
        runCatching {
            val types = api.shopTypes(token)
            shopTypes = types.filter { it.active }.ifEmpty { DefaultShopTypeChoices }
        }
        if (shopId != null && !loaded) {
            runCatching {
                val s = api.shopDetail(token, shopId).shop
                loaded = true
                name = s.name
                ownerName = s.ownerName
                ownerPhone = s.ownerPhone
                ownerEmail = s.ownerEmail
                shopType = s.shopType.ifBlank { ShopType.GROCERY.apiValue }
                loginUsername = s.loginUsername
                address = s.address
                city = s.city
                state = s.state
                zip = s.zip
                country = s.country.ifBlank { "India" }
                registration = s.registrationNumber
                taxId = s.taxId
                imageUrl = s.imageUrl.orEmpty()
                lat = s.lat
                lng = s.lng
                active = s.active
                voice = s.features.voiceSearch
                barcode = s.features.barcodeSearch
                reports = s.features.reportGeneration
                scanner = s.features.barcodeScanner
                online = s.features.onlineOrders
                inventory = s.features.inventoryAlerts
                paymentsEnabled = s.paymentsEnabled
                upiId = s.upiId
                vendorId = s.cashfreeVendorId
                radiusKm = s.deliveryRadiusKm.plainNumber()
                discountPct = s.discountPercent.plainNumber()
                deliveryUntil = s.deliveryUntil
                homeDelivery = s.homeDeliveryEnabled
                minOrder = s.minOrderValueInr.plainNumber()
                freeDelivery = s.freeDeliveryEnabled
                freeDeliveryAbove = s.freeDeliveryAboveInr.plainNumber()
                hours = withFullWeek(s.operatingHours)
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }

    fun updateDay(day: String, transform: (ShopHoursDayDto) -> ShopHoursDayDto) {
        hours = hours.map { if (it.day == day) transform(it) else it }
    }

    fun features() = ShopFeaturesDto(
        voiceSearch = voice,
        barcodeSearch = barcode,
        reportGeneration = reports,
        onlineOrders = online,
        inventoryAlerts = inventory,
        barcodeScanner = scanner,
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(
            title = if (isEdit) stringResource(Res.string.master_edit_shop_title) else stringResource(Res.string.master_add_new_shop_title),
            subtitle = stringResource(Res.string.master_add_new_shop_subtitle),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdaptivePane(Modifier.fillMaxWidth()) { wc ->
                val cols = if (wc == WindowWidthClass.Compact) 1 else 3
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = cols,
                ) {
                    // Column 1 — image + general + type
                    MasterSectionCard(modifier = Modifier.widthIn(min = 260.dp)) {
                        val uploadFailed = stringResource(Res.string.master_upload_failed)
                        val unsupportedImage = stringResource(Res.string.master_upload_unsupported_image)
                        val pickImage: () -> Unit = {
                            if (!uploadingImage) {
                                scope.launch {
                                    runCatching {
                                        uploadMasterImageOrNull(api, token, AppConstants.S3Prefix.MASTER_SHOP_IMAGES) {
                                            uploadingImage = true
                                        }
                                    }
                                        .onSuccess { url -> if (url != null) imageUrl = url }
                                        .onFailure {
                                            onStatus(if (it is WebImageDecodeException) unsupportedImage else it.message ?: uploadFailed)
                                        }
                                    uploadingImage = false
                                }
                            }
                        }
                        DashedUploadBox(
                            imageUrl = imageUrl.ifBlank { null },
                            label = stringResource(Res.string.master_upload_shop_image),
                            uploading = uploadingImage,
                            onClick = pickImage,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = pickImage, enabled = !uploadingImage) {
                                Text(stringResource(Res.string.master_choose_from_device))
                            }
                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(onClick = { imageUrl = "" }, enabled = !uploadingImage) {
                                    Text(stringResource(Res.string.master_remove_image))
                                }
                            }
                        }
                        OutlinedTextField(
                            imageUrl,
                            { imageUrl = it },
                            label = { Text(stringResource(Res.string.master_image_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        SectionTitle(stringResource(Res.string.master_general_info))
                        LabeledField(stringResource(Res.string.master_shop_name), name, stringResource(Res.string.master_placeholder_shop_name)) { name = it }
                        LabeledField(stringResource(Res.string.master_owner_name), ownerName, stringResource(Res.string.master_placeholder_owner)) { ownerName = it }
                        LabeledField(stringResource(Res.string.master_owner_phone), ownerPhone, stringResource(Res.string.master_placeholder_phone)) { ownerPhone = it }
                        LabeledField(stringResource(Res.string.master_owner_email), ownerEmail, stringResource(Res.string.master_placeholder_email)) { ownerEmail = it }
                        SectionTitle(stringResource(Res.string.master_shop_type))
                        ShopTypePicker(types = shopTypes, selected = shopType, onSelect = { shopType = it })
                        SectionTitle(stringResource(Res.string.master_shop_login))
                        Text(
                            stringResource(Res.string.master_shop_login_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LabeledField(stringResource(Res.string.master_login_username), loginUsername, ownerPhone.ifBlank { stringResource(Res.string.master_placeholder_phone) }) {
                            loginUsername = it.trim()
                        }
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = {
                                Text(
                                    if (isEdit) stringResource(Res.string.master_login_password_reset)
                                    else stringResource(Res.string.master_login_password),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                    }
                    // Column 2 — address + map
                    MasterSectionCard(modifier = Modifier.widthIn(min = 260.dp)) {
                        SectionTitle(stringResource(Res.string.master_address_details))
                        LabeledField(stringResource(Res.string.master_street_address), address, stringResource(Res.string.master_placeholder_street)) { address = it }
                        LabeledField(stringResource(Res.string.master_city), city, stringResource(Res.string.master_placeholder_city)) { city = it }
                        LabeledField(stringResource(Res.string.master_state), state, stringResource(Res.string.master_placeholder_state)) { state = it }
                        LabeledField(stringResource(Res.string.master_zip), zip, stringResource(Res.string.master_placeholder_zip)) { zip = it }
                        LabeledField(stringResource(Res.string.master_country), country, "India") { country = it }
                        MapLocationPicker(lat = lat, lng = lng, onLatChange = { lat = it }, onLngChange = { lng = it })
                    }
                    // Column 3 — business + hours + status + features
                    MasterSectionCard(modifier = Modifier.widthIn(min = 260.dp)) {
                        SectionTitle(stringResource(Res.string.master_business_details))
                        LabeledField(stringResource(Res.string.master_registration_number), registration, stringResource(Res.string.master_placeholder_reg)) { registration = it }
                        LabeledField(stringResource(Res.string.master_tax_id), taxId, stringResource(Res.string.master_placeholder_tax)) { taxId = it }
                        SectionTitle(stringResource(Res.string.master_operating_hours))
                        hours.forEach { day ->
                            HoursRowCompact(
                                day = day.day,
                                start = day.start,
                                end = day.end,
                                closed = day.closed,
                                onStart = { v -> updateDay(day.day) { it.copy(start = v) } },
                                onEnd = { v -> updateDay(day.day) { it.copy(end = v) } },
                                onClosedChange = { v -> updateDay(day.day) { it.copy(closed = v) } },
                            )
                        }
                        SectionTitle(stringResource(Res.string.master_delivery_payments))
                        FeatureToggleRow(stringResource(Res.string.profile_payments_enabled), paymentsEnabled) { paymentsEnabled = !paymentsEnabled }
                        LabeledField(stringResource(Res.string.profile_field_upi), upiId, stringResource(Res.string.master_placeholder_upi)) { upiId = it.trim() }
                        LabeledField(stringResource(Res.string.master_cashfree_vendor_id), vendorId, "") { vendorId = it.trim() }
                        Text(
                            stringResource(Res.string.master_cashfree_vendor_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LabeledField(stringResource(Res.string.profile_field_radius), radiusKm, "") { radiusKm = it }
                        LabeledField(stringResource(Res.string.profile_field_discount), discountPct, "") { discountPct = it }
                        LabeledField(stringResource(Res.string.shop_min_order_label), minOrder, "") { minOrder = it.trim() }
                        Text(
                            stringResource(Res.string.shop_min_order_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FeatureToggleRow(stringResource(Res.string.shop_free_delivery_label), freeDelivery) { freeDelivery = !freeDelivery }
                        if (freeDelivery) {
                            LabeledField(stringResource(Res.string.shop_free_delivery_amount), freeDeliveryAbove, "") {
                                freeDeliveryAbove = it.trim()
                            }
                        }
                        FeatureToggleRow(stringResource(Res.string.shop_home_delivery_label), homeDelivery) { homeDelivery = !homeDelivery }
                        Text(
                            stringResource(Res.string.shop_home_delivery_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TimePickerField(
                            label = stringResource(Res.string.shop_delivery_until_label),
                            value = deliveryUntil,
                            onChange = { deliveryUntil = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = homeDelivery,
                            emptyText = stringResource(Res.string.shop_delivery_until_none),
                            clearLabel = stringResource(Res.string.shop_delivery_until_clear),
                            defaultHour = DEFAULT_DELIVERY_CUTOFF_HOUR,
                        )
                        Text(
                            stringResource(Res.string.master_delivery_until_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SectionTitle(stringResource(Res.string.master_status))
                        ActiveInactiveToggle(active) { active = it }
                        SectionTitle(stringResource(Res.string.master_features))
                        FeatureToggleRow(stringResource(Res.string.master_feature_voice), voice) { voice = !voice }
                        FeatureToggleRow(stringResource(Res.string.master_feature_barcode), barcode) { barcode = !barcode }
                        FeatureToggleRow(stringResource(Res.string.master_feature_online), online) { online = !online }
                        FeatureToggleRow(stringResource(Res.string.master_feature_inventory), inventory) { inventory = !inventory }
                        FeatureToggleRow(stringResource(Res.string.master_feature_reports), reports) { reports = !reports }
                        FeatureToggleRow(stringResource(Res.string.master_feature_scanner), scanner) { scanner = !scanner }
                    }
                }
            }
            val errNameRequired = stringResource(Res.string.master_err_shop_name)
            val errAddressRequired = stringResource(Res.string.master_err_shop_address)
            val errPhone = stringResource(Res.string.master_err_owner_phone)
            val errEmail = stringResource(Res.string.master_err_owner_email)
            val errLocation = stringResource(Res.string.master_err_shop_location)
            val errLogin = stringResource(Res.string.master_err_login_username)
            val errPassword = stringResource(Res.string.master_err_login_password, MIN_SHOP_PASSWORD)
            val errRadius = stringResource(Res.string.master_err_delivery_radius)
            val errDiscount = stringResource(Res.string.master_err_discount)
            val errDeliveryUntil = stringResource(Res.string.master_err_delivery_until)
            val errMinOrder = stringResource(Res.string.shop_min_order_invalid)
            val errFreeDelivery = stringResource(Res.string.shop_free_delivery_invalid)
            fun validate(): String? {
                val phoneDigits = ownerPhone.filter(Char::isDigit).takeLast(10)
                return when {
                    radiusKm.isNotBlank() && (radiusKm.toDoubleOrNull() ?: -1.0) <= 0.0 -> errRadius
                    discountPct.isNotBlank() && (discountPct.toDoubleOrNull() ?: -1.0) !in 0.0..100.0 -> errDiscount
                    deliveryUntil.isNotBlank() && !HH_MM_REGEX.matches(deliveryUntil) -> errDeliveryUntil
                    minOrder.isNotBlank() && (minOrder.toDoubleOrNull() ?: -1.0) < 0.0 -> errMinOrder
                    freeDelivery && (freeDeliveryAbove.toDoubleOrNull() ?: 0.0) <= 0.0 -> errFreeDelivery
                    name.isBlank() -> errNameRequired
                    address.isBlank() && city.isBlank() -> errAddressRequired
                    ownerPhone.isNotBlank() && phoneDigits.length != 10 -> errPhone
                    ownerEmail.isNotBlank() && !EMAIL_REGEX.matches(ownerEmail.trim()) -> errEmail
                    lat !in -90.0..90.0 || lng !in -180.0..180.0 || (lat == 0.0 && lng == 0.0) -> errLocation
                    !isEdit && loginUsername.isBlank() && phoneDigits.length != 10 -> errLogin
                    !isEdit && loginPassword.length < MIN_SHOP_PASSWORD -> errPassword
                    isEdit && loginPassword.isNotEmpty() && loginPassword.length < MIN_SHOP_PASSWORD -> errPassword
                    else -> null
                }
            }
            formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = !saving,
                    onClick = {
                        formError = validate()
                        if (formError != null) return@Button
                        saving = true
                        scope.launch {
                            runCatching {
                                suspend fun patchDeliveryAndPayments(id: String) = api.patchShop(
                                    token = token,
                                    id = id,
                                    paymentsEnabled = paymentsEnabled,
                                    upiId = upiId,
                                    deliveryRadiusKm = radiusKm.toDoubleOrNull(),
                                    discountPercent = discountPct.toDoubleOrNull(),
                                    deliveryUntil = deliveryUntil,
                                    homeDeliveryEnabled = homeDelivery,
                                    minOrderValueInr = minOrder.toDoubleOrNull() ?: 0.0,
                                    freeDeliveryEnabled = freeDelivery,
                                    freeDeliveryAboveInr = freeDeliveryAbove.toDoubleOrNull()?.takeIf { freeDelivery },
                                )
                                if (isEdit && shopId != null) {
                                    api.patchShop(
                                        token = token,
                                        id = shopId,
                                        paymentsEnabled = paymentsEnabled,
                                        upiId = upiId,
                                        deliveryRadiusKm = radiusKm.toDoubleOrNull(),
                                        discountPercent = discountPct.toDoubleOrNull(),
                                        deliveryUntil = deliveryUntil,
                                        homeDeliveryEnabled = homeDelivery,
                                        minOrderValueInr = minOrder.toDoubleOrNull() ?: 0.0,
                                        freeDeliveryEnabled = freeDelivery,
                                        freeDeliveryAboveInr = freeDeliveryAbove.toDoubleOrNull()?.takeIf { freeDelivery },
                                        active = active,
                                        features = features(),
                                        imageUrl = imageUrl.ifBlank { null },
                                        name = name,
                                        address = address,
                                        shopType = shopType,
                                        ownerName = ownerName,
                                        ownerPhone = ownerPhone,
                                        ownerEmail = ownerEmail,
                                        city = city,
                                        state = state,
                                        zip = zip,
                                        country = country,
                                        registrationNumber = registration,
                                        taxId = taxId,
                                        lat = lat,
                                        lng = lng,
                                        operatingHours = hours,
                                        loginUsername = loginUsername.ifBlank { null },
                                        loginPassword = loginPassword.ifBlank { null },
                                    )
                                    api.setShopVendorId(token, shopId, vendorId)
                                } else {
                                    val created = api.createShop(
                                        token,
                                        ShopCreate(
                                            name = name,
                                            shopType = shopType,
                                            address = address,
                                            lat = lat,
                                            lng = lng,
                                            active = active,
                                            imageUrl = imageUrl.ifBlank { null },
                                            ownerName = ownerName,
                                            ownerPhone = ownerPhone,
                                            ownerEmail = ownerEmail,
                                            city = city,
                                            state = state,
                                            zip = zip,
                                            country = country,
                                            registrationNumber = registration,
                                            taxId = taxId,
                                            operatingHours = hours,
                                            features = features(),
                                            loginUsername = loginUsername,
                                            loginPassword = loginPassword,
                                        ),
                                    )
                                    patchDeliveryAndPayments(created.id)
                                    if (vendorId.isNotBlank()) api.setShopVendorId(token, created.id, vendorId)
                                }
                            }.onSuccess {
                                saving = false
                                onNavigate(if (isEdit && shopId != null) MasterDest.ShopDetail(shopId) else MasterDest.Shops)
                            }.onFailure {
                                saving = false
                                formError = it.message
                                onStatus(it.message.orEmpty())
                            }
                        }
                    },
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (isEdit) stringResource(Res.string.master_save_changes)
                            else stringResource(Res.string.master_create_shop),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        onNavigate(if (isEdit && shopId != null) MasterDest.ShopDetail(shopId) else MasterDest.Shops)
                    },
                ) { Text(stringResource(Res.string.master_cancel)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun LabeledField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 110.dp).weight(0.45f), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(0.55f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopTypePicker(types: List<ShopTypeDto>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        types.forEach { t ->
            val isSelected = t.id == selected || t.name.equals(selected, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(t.id) },
                label = { Text(t.name, style = MaterialTheme.typography.bodySmall) },
            )
        }
    }
}

@Composable
private fun DashedUploadBox(imageUrl: String?, label: String, uploading: Boolean, onClick: () -> Unit) {
    val stroke = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !uploading, onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                drawRoundRect(
                    color = stroke,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (uploading) {
            CircularProgressIndicator(Modifier.size(36.dp))
        } else if (imageUrl != null) {
            ImageThumb(imageUrl, Modifier.size(120.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HoursRowCompact(
    day: String,
    start: String,
    end: String,
    closed: Boolean,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onClosedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(day, modifier = Modifier.width(32.dp), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
        TimePickerField(stringResource(Res.string.master_hours_start), start, onStart, Modifier.weight(1f), enabled = !closed)
        TimePickerField(stringResource(Res.string.master_hours_end), end, onEnd, Modifier.weight(1f), enabled = !closed, defaultHour = DEFAULT_CLOSING_HOUR)
        FilterChip(
            selected = closed,
            onClick = { onClosedChange(!closed) },
            label = { Text(stringResource(Res.string.shop_closed), style = MaterialTheme.typography.labelSmall) },
        )
    }
}

internal val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val HH_MM_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private const val DEFAULT_DELIVERY_CUTOFF_HOUR = 17
private const val DEFAULT_CLOSING_HOUR = 21

/** Always returns all seven days so a saved week never silently closes the missing ones. */
internal fun withFullWeek(saved: List<ShopHoursDayDto>): List<ShopHoursDayDto> {
    val byDay = saved.associateBy { it.day.take(3).lowercase() }
    return WEEK_DAYS.map { day -> byDay[day.lowercase()]?.copy(day = day) ?: ShopHoursDayDto(day) }
}

internal fun Double.plainNumber(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Composable
fun ActiveInactiveToggle(active: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
            modifier = Modifier.clickable { onChange(true) },
        ) {
            Text(
                stringResource(Res.string.master_active),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                color = if (active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Surface(
            color = if (!active) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
            modifier = Modifier.clickable { onChange(false) },
        ) {
            Text(
                stringResource(Res.string.master_inactive),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                color = if (!active) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun FeatureToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked, { onToggle() })
    }
}

@Composable
fun shopTypeDisplayName(shopType: String, types: List<ShopTypeDto> = emptyList()): String {
    types.firstOrNull { it.id == shopType }?.name?.let { return it }
    val known = when (shopType) {
        "SUPERMARKET" -> Res.string.shop_type_supermarket
        "GROCERY" -> Res.string.shop_type_grocery
        "BOUTIQUE" -> Res.string.shop_type_boutique
        "GENERAL_STORE" -> Res.string.shop_type_general_store
        "MEDICAL_STORE" -> Res.string.shop_type_medical_store
        "HARDWARE" -> Res.string.shop_type_hardware
        "SWEET_SHOP" -> Res.string.shop_type_sweet_shop
        else -> null
    }
    return when {
        known != null -> stringResource(known)
        else -> shopType.replace('_', ' ')
    }
}
