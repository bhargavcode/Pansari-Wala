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
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
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
import pansariwala.shared.generated.resources.master_feature_online_search
import pansariwala.shared.generated.resources.master_feature_reports
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

private val DefaultShopTypeChoices = listOf(
    ShopTypeDto("SUPERMARKET", "Supermarket"),
    ShopTypeDto("GROCERY", "Grocery"),
    ShopTypeDto("BOUTIQUE", "Boutique"),
)

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
    var shopType by rememberSaveable { mutableStateOf("SUPERMARKET") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var zip by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("India") }
    var registration by rememberSaveable { mutableStateOf("") }
    var taxId by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf(41.0149426) }
    var lng by rememberSaveable { mutableStateOf(-55.4878477) }
    var active by rememberSaveable { mutableStateOf(true) }
    var voice by rememberSaveable { mutableStateOf(true) }
    var barcode by rememberSaveable { mutableStateOf(true) }
    var onlineSearch by rememberSaveable { mutableStateOf(true) }
    var online by rememberSaveable { mutableStateOf(true) }
    var inventory by rememberSaveable { mutableStateOf(true) }
    var reports by rememberSaveable { mutableStateOf(true) }
    var monStart by rememberSaveable { mutableStateOf("09:00") }
    var monEnd by rememberSaveable { mutableStateOf("21:00") }
    var tueStart by rememberSaveable { mutableStateOf("09:00") }
    var tueEnd by rememberSaveable { mutableStateOf("21:00") }
    var wedStart by rememberSaveable { mutableStateOf("09:00") }
    var wedEnd by rememberSaveable { mutableStateOf("21:00") }
    var thuStart by rememberSaveable { mutableStateOf("09:00") }
    var thuEnd by rememberSaveable { mutableStateOf("21:00") }
    var friStart by rememberSaveable { mutableStateOf("09:00") }
    var friEnd by rememberSaveable { mutableStateOf("21:00") }
    var shopTypes by remember { mutableStateOf(DefaultShopTypeChoices) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(token, shopId) {
        runCatching {
            val types = api.shopTypes(token)
            if (types.isNotEmpty()) {
                shopTypes = types.filter {
                    it.id in setOf("SUPERMARKET", "GROCERY", "BOUTIQUE") ||
                        it.name in setOf("Supermarket", "Grocery", "Boutique")
                }.ifEmpty { DefaultShopTypeChoices }
            }
        }
        if (shopId != null && !loaded) {
            runCatching {
                val s = api.shopDetail(token, shopId).shop
                loaded = true
                name = s.name
                ownerName = s.ownerName
                ownerPhone = s.ownerPhone
                ownerEmail = s.ownerEmail
                shopType = s.shopType.ifBlank { "SUPERMARKET" }
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
                online = s.features.onlineOrders
                inventory = s.features.inventoryAlerts
                onlineSearch = s.features.reportGeneration
                val byDay = s.operatingHours.associateBy { it.day }
                byDay["Mon"]?.let { monStart = it.start; monEnd = it.end }
                byDay["Tue"]?.let { tueStart = it.start; tueEnd = it.end }
                byDay["Wed"]?.let { wedStart = it.start; wedEnd = it.end }
                byDay["Thu"]?.let { thuStart = it.start; thuEnd = it.end }
                byDay["Fri"]?.let { friStart = it.start; friEnd = it.end }
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }

    fun hours() = listOf(
        ShopHoursDayDto("Mon", monStart, monEnd),
        ShopHoursDayDto("Tue", tueStart, tueEnd),
        ShopHoursDayDto("Wed", wedStart, wedEnd),
        ShopHoursDayDto("Thu", thuStart, thuEnd),
        ShopHoursDayDto("Fri", friStart, friEnd),
    )

    fun features() = ShopFeaturesDto(
        voiceSearch = voice,
        barcodeSearch = barcode,
        reportGeneration = reports || onlineSearch,
        onlineOrders = online,
        inventoryAlerts = inventory,
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
                        DashedUploadBox(
                            imageUrl = imageUrl.ifBlank { null },
                            label = stringResource(Res.string.master_upload_shop_image),
                        )
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
                        HoursRowCompact("Mon", monStart, monEnd, { monStart = it }, { monEnd = it })
                        HoursRowCompact("Tue", tueStart, tueEnd, { tueStart = it }, { tueEnd = it })
                        HoursRowCompact("Wed", wedStart, wedEnd, { wedStart = it }, { wedEnd = it })
                        HoursRowCompact("Thu", thuStart, thuEnd, { thuStart = it }, { thuEnd = it })
                        HoursRowCompact("Fri", friStart, friEnd, { friStart = it }, { friEnd = it })
                        SectionTitle(stringResource(Res.string.master_status))
                        ActiveInactiveToggle(active) { active = it }
                        SectionTitle(stringResource(Res.string.master_features))
                        FeatureToggleRow(stringResource(Res.string.master_feature_voice), voice) { voice = !voice }
                        FeatureToggleRow(stringResource(Res.string.master_feature_barcode), barcode) { barcode = !barcode }
                        FeatureToggleRow(stringResource(Res.string.master_feature_online_search), onlineSearch) { onlineSearch = !onlineSearch }
                        FeatureToggleRow(stringResource(Res.string.master_feature_online), online) { online = !online }
                        FeatureToggleRow(stringResource(Res.string.master_feature_inventory), inventory) { inventory = !inventory }
                        FeatureToggleRow(stringResource(Res.string.master_feature_reports), reports) { reports = !reports }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                if (isEdit && shopId != null) {
                                    api.patchShop(
                                        token = token,
                                        id = shopId,
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
                                        operatingHours = hours(),
                                    )
                                } else {
                                    api.createShop(
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
                                            operatingHours = hours(),
                                            features = features(),
                                        ),
                                    )
                                }
                            }.onSuccess {
                                onNavigate(if (isEdit && shopId != null) MasterDest.ShopDetail(shopId) else MasterDest.Shops)
                            }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                ) {
                    Text(
                        if (isEdit) stringResource(Res.string.master_save_changes)
                        else stringResource(Res.string.master_create_shop),
                    )
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

@Composable
private fun ShopTypePicker(types: List<ShopTypeDto>, selected: String, onSelect: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        types.forEach { t ->
            val isSelected = t.id == selected || t.name.equals(selected, ignoreCase = true)
            Text(
                t.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(t.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DashedUploadBox(imageUrl: String?, label: String) {
    val stroke = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
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
        if (imageUrl != null) {
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
private fun HoursRowCompact(day: String, start: String, end: String, onStart: (String) -> Unit, onEnd: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(day, modifier = Modifier.width(32.dp), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(start, onStart, label = { Text(stringResource(Res.string.master_hours_start)) }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
        OutlinedTextField(end, onEnd, label = { Text(stringResource(Res.string.master_hours_end)) }, modifier = Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
    }
}

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

fun shopTypeDisplayName(shopType: String, types: List<ShopTypeDto> = emptyList()): String {
    types.firstOrNull { it.id == shopType }?.name?.let { return it }
    return when (shopType) {
        "SUPERMARKET" -> "Supermarket"
        "GROCERY" -> "Grocery"
        "BOUTIQUE" -> "Boutique"
        "GENERAL_STORE" -> "General Store"
        "MEDICAL_STORE" -> "Medical Store"
        "HARDWARE" -> "Hardware"
        "SWEET_SHOP" -> "Sweet Shop"
        else -> shopType.replace('_', ' ')
    }
}
