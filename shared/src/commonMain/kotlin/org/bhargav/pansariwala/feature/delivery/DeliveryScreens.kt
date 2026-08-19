package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariLinkButton
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.domain.model.DeliveryOfferStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.ImagePicker
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_accept
import pansariwala.shared.generated.resources.action_attach_photo
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_cancel_job
import pansariwala.shared.generated.resources.action_deliver
import pansariwala.shared.generated.resources.action_delivery_offers
import pansariwala.shared.generated.resources.action_pickup
import pansariwala.shared.generated.resources.action_reject
import pansariwala.shared.generated.resources.action_remove_photo
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.action_submit_pickup
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.action_verify_otp
import pansariwala.shared.generated.resources.deliver_otp_title
import pansariwala.shared.generated.resources.field_address
import pansariwala.shared.generated.resources.field_email
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_otp
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.field_plate_photo
import pansariwala.shared.generated.resources.field_vehicle_photo
import pansariwala.shared.generated.resources.field_vehicle_reg
import pansariwala.shared.generated.resources.offer_already_taken
import pansariwala.shared.generated.resources.offer_payout
import pansariwala.shared.generated.resources.partner_accepted
import pansariwala.shared.generated.resources.partner_delivered
import pansariwala.shared.generated.resources.partner_earnings
import pansariwala.shared.generated.resources.partner_login_prompt
import pansariwala.shared.generated.resources.partner_login_subtitle
import pansariwala.shared.generated.resources.partner_login_title
import pansariwala.shared.generated.resources.partner_register_title
import pansariwala.shared.generated.resources.partner_signup_prompt
import pansariwala.shared.generated.resources.partner_today
import pansariwala.shared.generated.resources.photo_one
import pansariwala.shared.generated.resources.photo_two
import pansariwala.shared.generated.resources.pickup_photos_hint
import pansariwala.shared.generated.resources.pickup_photos_title
import pansariwala.shared.generated.resources.shop_distance

@Composable
fun PartnerLoginScreen(
    onVerified: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: PartnerLoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(
            title = stringResource(Res.string.partner_login_title),
            onBack = null,
        )
        Text(stringResource(Res.string.partner_login_subtitle))
        OutlinedTextField(state.phone, viewModel::setPhone, label = { Text(stringResource(Res.string.field_phone)) }, modifier = Modifier.fillMaxWidth())
        if (state.step == 0) {
            Button(onClick = viewModel::sendOtp, enabled = !state.loading && state.phone.length >= 10) {
                Text(stringResource(Res.string.action_send_otp))
            }
        } else {
            OutlinedTextField(state.otp, viewModel::setOtp, label = { Text(stringResource(Res.string.field_otp)) }, modifier = Modifier.fillMaxWidth())
            state.hint?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = { viewModel.verify(onVerified) },
                enabled = !state.loading && state.otp.length >= 4,
            ) { Text(stringResource(Res.string.action_verify_otp)) }
            PansariLinkButton(
                onClick = viewModel::sendOtp,
                enabled = !state.loading,
                text = stringResource(Res.string.action_resend_otp),
            )
        }
        PansariLinkButton(onClick = onSignUp, enabled = !state.loading, text = stringResource(Res.string.partner_signup_prompt))
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        if (state.loading) CircularProgressIndicator()
    }
}

@Composable
fun PartnerRegisterScreen(
    onVerified: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: PartnerRegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PansariTopBar(
            title = stringResource(Res.string.partner_register_title),
            onBack = null,
        )
        if (state.step == 0) {
            OutlinedTextField(state.name, viewModel::setName, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.email, viewModel::setEmail, label = { Text(stringResource(Res.string.field_email)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.address, viewModel::setAddress, label = { Text(stringResource(Res.string.field_address)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.phone, viewModel::setPhone, label = { Text(stringResource(Res.string.field_phone)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.vehicleReg, viewModel::setVehicleReg, label = { Text(stringResource(Res.string.field_vehicle_reg)) }, modifier = Modifier.fillMaxWidth())
            PhotoAttachRow(
                title = stringResource(Res.string.field_plate_photo),
                attached = state.platePhoto.isNotBlank(),
                onAttach = viewModel::attachPlate,
                onClear = viewModel::clearPlate,
            )
            PhotoAttachRow(
                title = stringResource(Res.string.field_vehicle_photo),
                attached = state.vehiclePhoto.isNotBlank(),
                onAttach = viewModel::attachVehicle,
                onClear = viewModel::clearVehicle,
            )
            Button(onClick = viewModel::save, enabled = !state.loading) { Text(stringResource(Res.string.action_save)) }
        } else {
            state.hint?.let { Text(it.asString()) }
            OutlinedTextField(state.otp, viewModel::setOtp, label = { Text(stringResource(Res.string.field_otp)) }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { viewModel.verify(onVerified) },
                enabled = !state.loading && state.otp.length >= 4,
            ) { Text(stringResource(Res.string.action_verify_otp)) }
            PansariLinkButton(onClick = viewModel::save, enabled = !state.loading, text = stringResource(Res.string.action_resend_otp))
        }
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        PansariLinkButton(onClick = onSignIn, enabled = !state.loading, text = stringResource(Res.string.partner_login_prompt))
    }
}

@Composable
fun PartnerDashboardScreen(
    onDelivered: () -> Unit,
    onAccepted: () -> Unit,
    onEarnings: () -> Unit,
    onIncoming: () -> Unit,
    viewModel: PartnerDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dash = state.dash
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.partner_today), fontWeight = FontWeight.Bold)
            TextButton(onClick = viewModel::loadToday) { Text(stringResource(Res.string.partner_today)) }
        }
        SectionCard(title = stringResource(Res.string.partner_delivered), modifier = Modifier.clickable(onClick = onDelivered)) {
            Text("${dash?.deliveredCount ?: 0}")
        }
        SectionCard(title = stringResource(Res.string.partner_accepted), modifier = Modifier.clickable(onClick = onAccepted)) {
            Text("${dash?.acceptedCount ?: 0}")
        }
        SectionCard(title = stringResource(Res.string.partner_earnings), modifier = Modifier.clickable(onClick = onEarnings)) {
            Text("₹${dash?.earnings ?: 0.0}")
        }
        Button(onClick = onIncoming, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.action_delivery_offers)) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun IncomingOfferScreen(
    offerId: String?,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
    viewModel: OfferViewModel = koinViewModel(),
) {
    LaunchedEffect(offerId) { viewModel.load(offerId) }
    val offer by viewModel.offer.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PansariTopBar(
            title = stringResource(Res.string.action_delivery_offers),
            onBack = onBack,
        )
        if (message == "taken" || offer?.status == DeliveryOfferStatus.TAKEN_BY_OTHER) {
            Text(stringResource(Res.string.offer_already_taken), style = MaterialTheme.typography.titleLarge)
        } else if (offer != null) {
            val o = offer!!
            Text(o.shop.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.shop_distance, ((o.shopDistanceKm * 10).toInt() / 10.0).toString()))
            Text(stringResource(Res.string.offer_payout, o.payoutInr.toInt().toString()), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(o.dropAddress)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.accept(onAccepted) }) { Text(stringResource(Res.string.action_accept)) }
                TextButton(onClick = { viewModel.reject(); onBack() }) { Text(stringResource(Res.string.action_reject)) }
            }
        }
    }
}

@Composable
fun JobsListScreen(
    delivered: Boolean,
    onPickup: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: JobsViewModel = koinViewModel(),
) {
    LaunchedEffect(delivered) {
        if (delivered) {
            val start = org.bhargav.pansariwala.util.AppClock.startOfTodayMillis()
            viewModel.loadDelivered(start, start + org.bhargav.pansariwala.util.MILLIS_PER_DAY)
        } else viewModel.loadAccepted()
    }
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val pickupError by viewModel.error.collectAsStateWithLifecycle()
    var otpFor by remember { mutableStateOf<String?>(null) }
    var otp by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PansariTopBar(
            title = stringResource(if (delivered) Res.string.partner_delivered else Res.string.partner_accepted),
            onBack = onBack,
        )
        pickupError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        jobs.forEach { job ->
            SectionCard(title = job.shopName ?: job.id) {
                Text(job.id)
                if (!delivered) {
                    Row {
                        TextButton(onClick = { viewModel.cancel(job.id) }) { Text(stringResource(Res.string.action_cancel_job)) }
                        if (job.status.name == "PARTNER_ACCEPTED") {
                            Button(onClick = { onPickup(job.id) }) { Text(stringResource(Res.string.action_pickup)) }
                        } else {
                            Button(onClick = { otpFor = job.id }) { Text(stringResource(Res.string.action_deliver)) }
                        }
                    }
                }
            }
        }
    }
    if (otpFor != null) {
        AlertDialog(
            onDismissRequest = { otpFor = null },
            title = { Text(stringResource(Res.string.deliver_otp_title)) },
            text = { OutlinedTextField(otp, { otp = it }, label = { Text(stringResource(Res.string.field_otp)) }) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deliver(otpFor!!, otp) { otpFor = null }
                }) { Text(stringResource(Res.string.action_deliver)) }
            },
            dismissButton = { TextButton(onClick = { otpFor = null }) { Text(stringResource(Res.string.action_back)) } },
        )
    }
}

@Composable
fun PickupScreen(
    orderId: String,
    onDone: () -> Unit,
    viewModel: JobsViewModel = koinViewModel(),
    imagePicker: ImagePicker = koinInject(),
) {
    var one by remember { mutableStateOf("") }
    var two by remember { mutableStateOf("") }
    val pickupError by viewModel.error.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.pickup_photos_title), fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.pickup_photos_hint))
        pickupError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        PhotoAttachRow(
            title = stringResource(Res.string.photo_one),
            attached = one.isNotBlank(),
            onAttach = { scope.launch { imagePicker.pickImage()?.let { one = it.base64 } } },
            onClear = { one = "" },
        )
        PhotoAttachRow(
            title = stringResource(Res.string.photo_two),
            attached = two.isNotBlank(),
            onAttach = { scope.launch { imagePicker.pickImage()?.let { two = it.base64 } } },
            onClear = { two = "" },
        )
        Button(
            onClick = { viewModel.pickup(orderId, one, two, onDone) },
            enabled = one.isNotBlank() && two.isNotBlank(),
        ) { Text(stringResource(Res.string.action_submit_pickup)) }
    }
}

@Composable
private fun PhotoAttachRow(
    title: String,
    attached: Boolean,
    onAttach: () -> Unit,
    onClear: () -> Unit,
) {
    if (attached) {
        val removeLabel = stringResource(Res.string.action_remove_photo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            IconButton(
                onClick = onClear,
                modifier = Modifier.semantics { contentDescription = removeLabel },
            ) {
                Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        TextButton(onClick = onAttach) {
            Text("$title · ${stringResource(Res.string.action_attach_photo)}")
        }
    }
}
