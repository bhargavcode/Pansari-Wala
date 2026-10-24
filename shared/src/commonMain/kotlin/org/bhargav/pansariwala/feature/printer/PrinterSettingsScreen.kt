package org.bhargav.pansariwala.feature.printer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.rememberPrinterAccessRequester
import org.bhargav.pansariwala.printer.DEFAULT_RAW_PRINT_PORT
import org.bhargav.pansariwala.printer.model.DitherMode
import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PaperWidth
import org.bhargav.pansariwala.printer.model.PrintAlignment
import org.bhargav.pansariwala.printer.model.PrintFont
import org.bhargav.pansariwala.printer.model.PrintFontSize
import org.bhargav.pansariwala.printer.model.PrintHeaderFooter
import org.bhargav.pansariwala.printer.model.PrinterConnectionState
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.printer_action_add_connect
import pansariwala.shared.generated.resources.printer_action_disconnect
import pansariwala.shared.generated.resources.printer_action_enable_bluetooth
import pansariwala.shared.generated.resources.printer_action_forget
import pansariwala.shared.generated.resources.printer_action_grant_permission
import pansariwala.shared.generated.resources.printer_action_reconnect
import pansariwala.shared.generated.resources.printer_action_scan
import pansariwala.shared.generated.resources.printer_action_stop_scan
import pansariwala.shared.generated.resources.printer_address
import pansariwala.shared.generated.resources.printer_align_center
import pansariwala.shared.generated.resources.printer_align_left
import pansariwala.shared.generated.resources.printer_align_right
import pansariwala.shared.generated.resources.printer_auto_cut
import pansariwala.shared.generated.resources.printer_auto_print
import pansariwala.shared.generated.resources.printer_auto_reconnect
import pansariwala.shared.generated.resources.printer_beep
import pansariwala.shared.generated.resources.printer_bold_shop_name
import pansariwala.shared.generated.resources.printer_bold_titles
import pansariwala.shared.generated.resources.printer_brightness
import pansariwala.shared.generated.resources.printer_cash_drawer
import pansariwala.shared.generated.resources.printer_chars_auto
import pansariwala.shared.generated.resources.printer_chars_per_line
import pansariwala.shared.generated.resources.printer_copies
import pansariwala.shared.generated.resources.printer_currency
import pansariwala.shared.generated.resources.printer_current_section
import pansariwala.shared.generated.resources.printer_density
import pansariwala.shared.generated.resources.printer_dither
import pansariwala.shared.generated.resources.printer_dither_atkinson
import pansariwala.shared.generated.resources.printer_dither_floyd
import pansariwala.shared.generated.resources.printer_dither_ordered
import pansariwala.shared.generated.resources.printer_contrast
import pansariwala.shared.generated.resources.printer_gray_tone
import pansariwala.shared.generated.resources.printer_dither_threshold
import pansariwala.shared.generated.resources.printer_feed_lines
import pansariwala.shared.generated.resources.printer_find_section
import pansariwala.shared.generated.resources.printer_find_subtitle
import pansariwala.shared.generated.resources.printer_font
import pansariwala.shared.generated.resources.printer_font_auto
import pansariwala.shared.generated.resources.printer_font_builtin
import pansariwala.shared.generated.resources.printer_font_hint_auto
import pansariwala.shared.generated.resources.printer_font_hint_builtin
import pansariwala.shared.generated.resources.printer_font_hint_device
import pansariwala.shared.generated.resources.printer_font_large
import pansariwala.shared.generated.resources.printer_font_mono
import pansariwala.shared.generated.resources.printer_font_sans
import pansariwala.shared.generated.resources.printer_font_serif
import pansariwala.shared.generated.resources.printer_font_normal
import pansariwala.shared.generated.resources.printer_font_size
import pansariwala.shared.generated.resources.printer_font_small
import pansariwala.shared.generated.resources.printer_font_xl
import pansariwala.shared.generated.resources.printer_footer_note
import pansariwala.shared.generated.resources.printer_header_alignment
import pansariwala.shared.generated.resources.printer_header_note
import pansariwala.shared.generated.resources.printer_header_section
import pansariwala.shared.generated.resources.printer_image_section
import pansariwala.shared.generated.resources.printer_invert
import pansariwala.shared.generated.resources.printer_line_spacing
import pansariwala.shared.generated.resources.printer_logo_reload_hint
import pansariwala.shared.generated.resources.printer_logo_width
import pansariwala.shared.generated.resources.printer_manual_ip
import pansariwala.shared.generated.resources.printer_manual_port
import pansariwala.shared.generated.resources.printer_manual_title
import pansariwala.shared.generated.resources.printer_no_device
import pansariwala.shared.generated.resources.printer_no_devices_found
import pansariwala.shared.generated.resources.printer_output_section
import pansariwala.shared.generated.resources.printer_paired
import pansariwala.shared.generated.resources.printer_paper_58
import pansariwala.shared.generated.resources.printer_paper_80
import pansariwala.shared.generated.resources.printer_paper_section
import pansariwala.shared.generated.resources.printer_paper_width
import pansariwala.shared.generated.resources.printer_phone
import pansariwala.shared.generated.resources.printer_pick_logo
import pansariwala.shared.generated.resources.printer_print_logo
import pansariwala.shared.generated.resources.printer_qr_content
import pansariwala.shared.generated.resources.printer_receipt_section
import pansariwala.shared.generated.resources.printer_remove_logo
import pansariwala.shared.generated.resources.printer_scanning
import pansariwala.shared.generated.resources.printer_settings_title
import pansariwala.shared.generated.resources.printer_shop_name
import pansariwala.shared.generated.resources.printer_show_barcode
import pansariwala.shared.generated.resources.printer_show_footer
import pansariwala.shared.generated.resources.printer_show_header
import pansariwala.shared.generated.resources.printer_show_qr
import pansariwala.shared.generated.resources.printer_signal
import pansariwala.shared.generated.resources.printer_tax_id
import pansariwala.shared.generated.resources.printer_test_inventory
import pansariwala.shared.generated.resources.printer_test_invoice
import pansariwala.shared.generated.resources.printer_test_labels
import pansariwala.shared.generated.resources.printer_test_low_stock
import pansariwala.shared.generated.resources.printer_test_page
import pansariwala.shared.generated.resources.printer_test_receipt
import pansariwala.shared.generated.resources.printer_test_section
import pansariwala.shared.generated.resources.printer_test_ticket
import pansariwala.shared.generated.resources.printer_threshold
import pansariwala.shared.generated.resources.printer_web_unsupported
import kotlin.math.roundToInt

@Composable
fun PrinterSettingsScreen(
    onBack: () -> Unit,
    viewModel: PrinterSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message.resolve())
        }
    }

    // Radio scans must never outlive the visible screen.
    LifecycleStartEffect(viewModel) {
        onStopOrDispose { viewModel.stopScan() }
    }

    val access = rememberPrinterAccessRequester { granted -> if (granted) viewModel.startScan() }
    val bluetoothSelected = state.selectedTypes.any { it != PrinterConnectionType.NETWORK }
    val onScan = {
        when {
            bluetoothSelected && viewModel.needsBluetoothPermission() -> access.requestBluetoothPermission()
            bluetoothSelected && viewModel.bluetoothDisabled() && access.canRequestEnableBluetooth ->
                access.requestEnableBluetooth()
            else -> viewModel.startScan()
        }
    }

    PansariScreen(
        title = stringResource(Res.string.printer_settings_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CurrentPrinterSection(state, viewModel)
            if (state.isSupported) {
                FindPrintersSection(
                    state = state,
                    viewModel = viewModel,
                    onScan = onScan,
                    onGrantPermission = access::requestBluetoothPermission,
                    onEnableBluetooth = access::requestEnableBluetooth.takeIf { access.canRequestEnableBluetooth },
                )
            } else {
                SectionCard(title = stringResource(Res.string.printer_find_section)) {
                    Text(
                        text = stringResource(Res.string.printer_web_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TestPrintSection(state, viewModel)
            PaperSection(state.settings, viewModel)
            HeaderFooterSection(state.settings, viewModel)
            ImageSection(state, viewModel)
            ReceiptExtrasSection(state.settings, viewModel)
            OutputSection(state.settings, viewModel)
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CurrentPrinterSection(state: PrinterSettingsUiState, viewModel: PrinterSettingsViewModel) {
    val connection = state.connection
    val device = connection.deviceOrNull ?: state.settings.lastDevice
    SectionCard(title = stringResource(Res.string.printer_current_section)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device?.displayName ?: stringResource(Res.string.printer_no_device),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                device?.let {
                    Text(
                        text = "${stringResource(it.type.labelRes)} · ${it.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = connection.statusLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (connection is PrinterConnectionState.Connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            if (connection is PrinterConnectionState.Connecting) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        (connection as? PrinterConnectionState.Failed)?.let {
            Text(
                text = stringResource(it.reason.messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (device != null) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (connection) {
                    is PrinterConnectionState.Connected, is PrinterConnectionState.Connecting ->
                        OutlinedButton(onClick = viewModel::disconnect) {
                            Text(stringResource(Res.string.printer_action_disconnect))
                        }
                    else -> Button(onClick = viewModel::reconnect) {
                        Text(stringResource(Res.string.printer_action_reconnect))
                    }
                }
                TextButton(onClick = viewModel::forget) { Text(stringResource(Res.string.printer_action_forget)) }
            }
        }
    }
}

@Composable
private fun FindPrintersSection(
    state: PrinterSettingsUiState,
    viewModel: PrinterSettingsViewModel,
    onScan: () -> Unit,
    onGrantPermission: () -> Unit,
    onEnableBluetooth: (() -> Unit)?,
) {
    val scan = state.scan
    SectionCard(
        title = stringResource(Res.string.printer_find_section),
        subtitle = stringResource(Res.string.printer_find_subtitle),
        action = {
            if (scan.isScanning) {
                OutlinedButton(onClick = viewModel::stopScan) { Text(stringResource(Res.string.printer_action_stop_scan)) }
            } else {
                Button(onClick = onScan) { Text(stringResource(Res.string.printer_action_scan)) }
            }
        },
    ) {
        if (state.supportedTypes.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.supportedTypes.forEach { type ->
                    FilterChip(
                        selected = type in state.selectedTypes,
                        onClick = { viewModel.toggleType(type) },
                        label = { Text(stringResource(type.labelRes)) },
                        leadingIcon = { Icon(type.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
        if (scan.isScanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text(
                text = stringResource(Res.string.printer_scanning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        scan.error?.let { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
            when {
                error == PrinterError.PERMISSION_DENIED -> TextButton(onClick = onGrantPermission) {
                    Text(stringResource(Res.string.printer_action_grant_permission))
                }
                error == PrinterError.BLUETOOTH_OFF && onEnableBluetooth != null -> TextButton(onClick = onEnableBluetooth) {
                    Text(stringResource(Res.string.printer_action_enable_bluetooth))
                }
            }
        }
        val connectedId = (state.connection as? PrinterConnectionState.Connected)?.device?.id
        val connectingId = (state.connection as? PrinterConnectionState.Connecting)?.device?.id
        if (scan.devices.isEmpty() && !scan.isScanning) {
            Text(
                text = stringResource(Res.string.printer_no_devices_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        scan.devices.forEachIndexed { index, device ->
            if (index > 0) HorizontalDivider()
            DeviceRow(
                device = device,
                connected = device.id == connectedId,
                connecting = device.id == connectingId,
                onClick = { viewModel.connect(device) },
            )
        }
        if (PrinterConnectionType.NETWORK in state.supportedTypes) {
            ManualNetworkPrinter(onAdd = viewModel::addNetworkPrinter)
        }
    }
}

@Composable
private fun DeviceRow(device: PrinterDevice, connected: Boolean, connecting: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !connected && !connecting, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(device.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(device.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val meta = buildList {
                add(device.address)
                if (device.isPaired) add(stringResource(Res.string.printer_paired))
                device.signalStrength?.let { add(stringResource(Res.string.printer_signal, it)) }
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            connected -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            connecting -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun ManualNetworkPrinter(onAdd: (String, String) -> Boolean) {
    var ip by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf(DEFAULT_RAW_PRINT_PORT.toString()) }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text(stringResource(Res.string.printer_manual_title), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(Res.string.printer_manual_ip)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it.filter(Char::isDigit).take(5) },
            label = { Text(stringResource(Res.string.printer_manual_port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.widthIn(max = 110.dp),
        )
    }
    Button(onClick = { onAdd(ip, port) }, modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(Res.string.printer_action_add_connect))
    }
}

@Composable
private fun TestPrintSection(state: PrinterSettingsUiState, viewModel: PrinterSettingsViewModel) {
    SectionCard(title = stringResource(Res.string.printer_test_section)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                PrintJob.TEST_PAGE to Res.string.printer_test_page,
                PrintJob.RECEIPT to Res.string.printer_test_receipt,
                PrintJob.TICKET to Res.string.printer_test_ticket,
                PrintJob.INVOICE to Res.string.printer_test_invoice,
                PrintJob.LABELS to Res.string.printer_test_labels,
                PrintJob.INVENTORY to Res.string.printer_test_inventory,
                PrintJob.LOW_STOCK to Res.string.printer_test_low_stock,
            ).forEach { (job, label) ->
                OutlinedButton(
                    onClick = { viewModel.print(job) },
                    enabled = state.runningJob == null && state.isSupported,
                ) {
                    if (state.runningJob == job) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(label))
                }
            }
        }
    }
}

@Composable
private fun PaperSection(settings: PrinterSettings, viewModel: PrinterSettingsViewModel) {
    SectionCard(title = stringResource(Res.string.printer_paper_section)) {
        Label(Res.string.printer_paper_width)
        Choices(
            options = PaperWidth.entries,
            selected = settings.paperWidth,
            label = { if (it == PaperWidth.MM_58) Res.string.printer_paper_58 else Res.string.printer_paper_80 },
            onSelect = { width -> viewModel.update { it.copy(paperWidth = width) } },
        )
        Label(Res.string.printer_font_size)
        Choices(
            options = PrintFontSize.entries,
            selected = settings.fontSize,
            label = {
                when (it) {
                    PrintFontSize.SMALL -> Res.string.printer_font_small
                    PrintFontSize.NORMAL -> Res.string.printer_font_normal
                    PrintFontSize.LARGE -> Res.string.printer_font_large
                    PrintFontSize.EXTRA_LARGE -> Res.string.printer_font_xl
                }
            },
            onSelect = { size -> viewModel.update { it.copy(fontSize = size) } },
        )
        Label(Res.string.printer_font)
        Choices(
            options = PrintFont.entries,
            selected = settings.font,
            label = {
                when (it) {
                    PrintFont.AUTO -> Res.string.printer_font_auto
                    PrintFont.PRINTER -> Res.string.printer_font_builtin
                    PrintFont.SANS -> Res.string.printer_font_sans
                    PrintFont.SERIF -> Res.string.printer_font_serif
                    PrintFont.MONOSPACE -> Res.string.printer_font_mono
                }
            },
            onSelect = { font -> viewModel.update { it.copy(font = font) } },
        )
        Text(
            text = stringResource(
                when (settings.font) {
                    PrintFont.AUTO -> Res.string.printer_font_hint_auto
                    PrintFont.PRINTER -> Res.string.printer_font_hint_builtin
                    else -> Res.string.printer_font_hint_device
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SliderRow(
            label = stringResource(Res.string.printer_chars_per_line),
            valueText = if (settings.customCharsPerLine > 0) {
                settings.customCharsPerLine.toString()
            } else {
                stringResource(Res.string.printer_chars_auto, settings.paperWidth.charsPerLine)
            },
            value = settings.customCharsPerLine.toFloat(),
            range = 0f..MAX_CHARS_PER_LINE.toFloat(),
            onChange = { value ->
                val chars = value.roundToInt().let { if (it < MIN_CHARS_PER_LINE) 0 else it }
                viewModel.update { it.copy(customCharsPerLine = chars) }
            },
        )
        SliderRow(
            label = stringResource(Res.string.printer_line_spacing),
            valueText = settings.lineSpacing.toString(),
            value = settings.lineSpacing.toFloat(),
            range = 0f..MAX_LINE_SPACING.toFloat(),
            onChange = { value -> viewModel.update { it.copy(lineSpacing = value.roundToInt()) } },
        )
        SliderRow(
            label = stringResource(Res.string.printer_density),
            valueText = settings.density.toString(),
            value = settings.density.toFloat(),
            range = 1f..8f,
            steps = 6,
            onChange = { value -> viewModel.update { it.copy(density = value.roundToInt()) } },
        )
        ToggleRow(Res.string.printer_bold_titles, settings.boldTitles) { on -> viewModel.update { it.copy(boldTitles = on) } }
        Field(Res.string.printer_currency, settings.currencySymbol) { text ->
            viewModel.update { it.copy(currencySymbol = text.take(MAX_CURRENCY_LENGTH)) }
        }
    }
}

@Composable
private fun HeaderFooterSection(settings: PrinterSettings, viewModel: PrinterSettingsViewModel) {
    val hf = settings.headerFooter
    fun edit(transform: (PrintHeaderFooter) -> PrintHeaderFooter) =
        viewModel.update { it.copy(headerFooter = transform(it.headerFooter)) }

    SectionCard(title = stringResource(Res.string.printer_header_section)) {
        ToggleRow(Res.string.printer_show_header, hf.showHeader) { on -> edit { it.copy(showHeader = on) } }
        if (hf.showHeader) {
            Field(Res.string.printer_shop_name, hf.shopName) { v -> edit { it.copy(shopName = v) } }
            Field(Res.string.printer_address, hf.addressLines, singleLine = false) { v -> edit { it.copy(addressLines = v) } }
            Field(Res.string.printer_phone, hf.phone, keyboardType = KeyboardType.Phone) { v -> edit { it.copy(phone = v) } }
            Field(Res.string.printer_tax_id, hf.taxId) { v -> edit { it.copy(taxId = v) } }
            Field(Res.string.printer_header_note, hf.headerNote, singleLine = false) { v -> edit { it.copy(headerNote = v) } }
            Label(Res.string.printer_header_alignment)
            Choices(
                options = PrintAlignment.entries,
                selected = hf.headerAlignment,
                label = {
                    when (it) {
                        PrintAlignment.LEFT -> Res.string.printer_align_left
                        PrintAlignment.CENTER -> Res.string.printer_align_center
                        PrintAlignment.RIGHT -> Res.string.printer_align_right
                    }
                },
                onSelect = { align -> edit { it.copy(headerAlignment = align) } },
            )
            ToggleRow(Res.string.printer_bold_shop_name, hf.boldShopName) { on -> edit { it.copy(boldShopName = on) } }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ToggleRow(Res.string.printer_show_footer, hf.showFooter) { on -> edit { it.copy(showFooter = on) } }
        if (hf.showFooter) {
            Field(Res.string.printer_footer_note, hf.footerNote, singleLine = false) { v -> edit { it.copy(footerNote = v) } }
        }
    }
}

@Composable
private fun ImageSection(state: PrinterSettingsUiState, viewModel: PrinterSettingsViewModel) {
    val image = state.settings.image
    SectionCard(title = stringResource(Res.string.printer_image_section)) {
        ToggleRow(Res.string.printer_print_logo, image.printLogo) { on -> viewModel.updateImage { it.copy(printLogo = on) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = viewModel::pickLogo, enabled = !state.isProcessingLogo) {
                Text(stringResource(Res.string.printer_pick_logo))
            }
            if (image.logo != null) {
                TextButton(onClick = viewModel::removeLogo) { Text(stringResource(Res.string.printer_remove_logo)) }
            }
            if (state.isProcessingLogo) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        image.logo?.let { logo ->
            val preview = remember(logo) { logo.toPreviewBitmap() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(bitmap = preview, contentDescription = null, modifier = Modifier.widthIn(max = 280.dp))
            }
        }
        if (image.logo != null && !state.hasLogoSource) {
            Text(
                text = stringResource(Res.string.printer_logo_reload_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val tunable = state.hasLogoSource
        SliderRow(
            label = stringResource(Res.string.printer_logo_width),
            valueText = "${image.logoWidthPercent}%",
            value = image.logoWidthPercent.toFloat(),
            range = 10f..100f,
            enabled = tunable,
            onChange = { v -> viewModel.updateImage { it.copy(logoWidthPercent = v.roundToInt()) } },
        )
        Label(Res.string.printer_dither)
        Choices(
            options = DitherMode.entries,
            selected = image.ditherMode,
            label = {
                when (it) {
                    DitherMode.THRESHOLD -> Res.string.printer_dither_threshold
                    DitherMode.FLOYD_STEINBERG -> Res.string.printer_dither_floyd
                    DitherMode.ATKINSON -> Res.string.printer_dither_atkinson
                    DitherMode.ORDERED -> Res.string.printer_dither_ordered
                }
            },
            enabled = tunable,
            onSelect = { mode -> viewModel.updateImage { it.copy(ditherMode = mode) } },
        )
        if (image.ditherMode == DitherMode.THRESHOLD) {
            SliderRow(
                label = stringResource(Res.string.printer_threshold),
                valueText = image.threshold.toString(),
                value = image.threshold.toFloat(),
                range = 1f..254f,
                enabled = tunable,
                onChange = { v -> viewModel.updateImage { it.copy(threshold = v.roundToInt()) } },
            )
        }
        SliderRow(
            label = stringResource(Res.string.printer_brightness),
            valueText = image.brightness.toString(),
            value = image.brightness.toFloat(),
            range = -100f..100f,
            enabled = tunable,
            onChange = { v -> viewModel.updateImage { it.copy(brightness = v.roundToInt()) } },
        )
        SliderRow(
            label = stringResource(Res.string.printer_contrast),
            valueText = image.contrast.toString(),
            value = image.contrast.toFloat(),
            range = -100f..100f,
            enabled = tunable,
            onChange = { v -> viewModel.updateImage { it.copy(contrast = v.roundToInt()) } },
        )
        if (image.ditherMode != DitherMode.THRESHOLD) {
            SliderRow(
                label = stringResource(Res.string.printer_gray_tone),
                valueText = "${image.gammaPercent}%",
                value = image.gammaPercent.toFloat(),
                range = 50f..300f,
                enabled = tunable,
                onChange = { v -> viewModel.updateImage { it.copy(gammaPercent = v.roundToInt()) } },
            )
        }
        ToggleRow(Res.string.printer_invert, image.invert, enabled = tunable) { on ->
            viewModel.updateImage { it.copy(invert = on) }
        }
    }
}

@Composable
private fun ReceiptExtrasSection(settings: PrinterSettings, viewModel: PrinterSettingsViewModel) {
    SectionCard(title = stringResource(Res.string.printer_receipt_section)) {
        ToggleRow(Res.string.printer_show_qr, settings.showQrOnReceipt) { on -> viewModel.update { it.copy(showQrOnReceipt = on) } }
        if (settings.showQrOnReceipt) {
            Field(Res.string.printer_qr_content, settings.qrContent) { v -> viewModel.update { it.copy(qrContent = v) } }
        }
        ToggleRow(Res.string.printer_show_barcode, settings.showBarcodeOnReceipt) { on ->
            viewModel.update { it.copy(showBarcodeOnReceipt = on) }
        }
        ToggleRow(Res.string.printer_auto_print, settings.autoPrintReceipt) { on -> viewModel.update { it.copy(autoPrintReceipt = on) } }
    }
}

@Composable
private fun OutputSection(settings: PrinterSettings, viewModel: PrinterSettingsViewModel) {
    SectionCard(title = stringResource(Res.string.printer_output_section)) {
        SliderRow(
            label = stringResource(Res.string.printer_copies),
            valueText = settings.copies.toString(),
            value = settings.copies.toFloat(),
            range = 1f..MAX_COPIES.toFloat(),
            steps = MAX_COPIES - 2,
            onChange = { v -> viewModel.update { it.copy(copies = v.roundToInt()) } },
        )
        SliderRow(
            label = stringResource(Res.string.printer_feed_lines),
            valueText = settings.feedLinesAfterPrint.toString(),
            value = settings.feedLinesAfterPrint.toFloat(),
            range = 0f..MAX_FEED_LINES.toFloat(),
            steps = MAX_FEED_LINES - 1,
            onChange = { v -> viewModel.update { it.copy(feedLinesAfterPrint = v.roundToInt()) } },
        )
        ToggleRow(Res.string.printer_auto_cut, settings.autoCut) { on -> viewModel.update { it.copy(autoCut = on) } }
        ToggleRow(Res.string.printer_cash_drawer, settings.openCashDrawer) { on -> viewModel.update { it.copy(openCashDrawer = on) } }
        ToggleRow(Res.string.printer_beep, settings.beepAfterPrint) { on -> viewModel.update { it.copy(beepAfterPrint = on) } }
        ToggleRow(Res.string.printer_auto_reconnect, settings.autoReconnect) { on -> viewModel.update { it.copy(autoReconnect = on) } }
    }
}

// ---------- small building blocks ----------

@Composable
private fun Label(text: StringResource) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    label: (T) -> StringResource,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(stringResource(label(option))) },
            )
        }
    }
}

@Composable
private fun ToggleRow(title: StringResource, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps, enabled = enabled)
    }
}

@Composable
private fun Field(
    label: StringResource,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

private fun PrinterConnectionType.icon() = when (this) {
    PrinterConnectionType.NETWORK -> Icons.Filled.Wifi
    PrinterConnectionType.USB -> Icons.Filled.Usb
    else -> Icons.Filled.Bluetooth
}

/** Draws black runs as rectangles once; cheap enough to run inside `remember`. */
private fun MonoBitmap.toPreviewBitmap(): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), Paint().apply { color = Color.White })
    val ink = Paint().apply { color = Color.Black }
    for (y in 0 until height) {
        var x = 0
        while (x < width) {
            if (isBlack(x, y)) {
                val start = x
                while (x < width && isBlack(x, y)) x++
                canvas.drawRect(Rect(start.toFloat(), y.toFloat(), x.toFloat(), y + 1f), ink)
            } else {
                x++
            }
        }
    }
    return bitmap
}

private const val MIN_CHARS_PER_LINE = 24
private const val MAX_CHARS_PER_LINE = 64
private const val MAX_LINE_SPACING = 60
private const val MAX_COPIES = 5
private const val MAX_FEED_LINES = 8
private const val MAX_CURRENCY_LENGTH = 4
