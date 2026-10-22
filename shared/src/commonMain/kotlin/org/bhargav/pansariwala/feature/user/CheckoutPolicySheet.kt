package org.bhargav.pansariwala.feature.user

import org.bhargav.pansariwala.designsystem.AppBottomSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.checkout_policy_agree_pay
import pansariwala.shared.generated.resources.checkout_policy_checkbox
import pansariwala.shared.generated.resources.checkout_policy_contact_title
import pansariwala.shared.generated.resources.checkout_policy_fresh_title
import pansariwala.shared.generated.resources.checkout_policy_headline
import pansariwala.shared.generated.resources.checkout_policy_point_contact_shop
import pansariwala.shared.generated.resources.checkout_policy_point_fresh
import pansariwala.shared.generated.resources.checkout_policy_point_no_refund
import pansariwala.shared.generated.resources.checkout_policy_point_review
import pansariwala.shared.generated.resources.checkout_policy_review_cart
import pansariwala.shared.generated.resources.checkout_policy_review_title
import pansariwala.shared.generated.resources.checkout_policy_shop_fallback
import pansariwala.shared.generated.resources.checkout_policy_subtitle
import pansariwala.shared.generated.resources.checkout_policy_title

/**
 * Shown before payment: the user must tick the acknowledgement that orders are final
 * (no return/refund via the app) and issues are settled directly with the shop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPolicySheet(
    shopName: String?,
    payableLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var agreed by rememberSaveable { mutableStateOf(false) }
    val shop = shopName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.checkout_policy_shop_fallback)
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.checkout_policy_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(Res.string.checkout_policy_subtitle, shop),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PolicyPoint(
                        Icons.Default.Inventory2,
                        stringResource(Res.string.checkout_policy_fresh_title),
                        stringResource(Res.string.checkout_policy_point_fresh),
                    )
                    PolicyPoint(
                        Icons.Default.Checklist,
                        stringResource(Res.string.checkout_policy_review_title),
                        stringResource(Res.string.checkout_policy_point_review),
                    )
                    PolicyPoint(
                        Icons.Default.SupportAgent,
                        stringResource(Res.string.checkout_policy_contact_title),
                        stringResource(Res.string.checkout_policy_point_contact_shop, shop),
                    )
                }
            }
            Surface(
                color = POLICY_WARNING_CONTAINER,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, POLICY_WARNING_BORDER),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = POLICY_WARNING_CONTENT, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(Res.string.checkout_policy_headline),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = POLICY_WARNING_CONTENT,
                            )
                            Text(
                                stringResource(Res.string.checkout_policy_point_no_refund),
                                style = MaterialTheme.typography.bodySmall,
                                color = POLICY_WARNING_CONTENT,
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = agreed, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.checkout_policy_checkbox),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = POLICY_WARNING_CONTENT,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                UserPrimaryButton(
                    text = stringResource(Res.string.checkout_policy_agree_pay, payableLabel),
                    onClick = onConfirm,
                    enabled = agreed,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.checkout_policy_review_cart))
                }
            }
        }
    }
}

@Composable
private fun PolicyPoint(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val POLICY_WARNING_CONTAINER = Color(0xFFFFEBEE)
private val POLICY_WARNING_BORDER = Color(0xFFEF9A9A)
private val POLICY_WARNING_CONTENT = Color(0xFFB71C1C)
