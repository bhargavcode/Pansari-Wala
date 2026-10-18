package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.api.toDto
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.feature.catalog.VariantCard
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.variant_add_variant_title
import pansariwala.shared.generated.resources.variant_current_label

/** Adds one variant to [product] from billing; a variant-less product also names its current pack. */
@Composable
internal fun PosAddVariantDialog(
    product: Product,
    onSave: (draft: ProductVariantDto, currentLabel: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(product.id) { mutableStateOf(ProductVariantDto(name = "", price = product.sellingPrice)) }
    var currentLabel by remember(product.id) { mutableStateOf("") }
    val needsCurrentLabel = product.variants.isEmpty()
    val siblings = remember(product) { product.variants.map { it.toDto() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.variant_add_variant_title, product.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsCurrentLabel) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = { currentLabel = it },
                        label = { Text(stringResource(Res.string.variant_current_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                VariantCard(
                    slot = "pos-${product.id}",
                    variant = draft,
                    onChange = { draft = it },
                    onRemove = null,
                    onMoveUp = null,
                    onPickImage = null,
                    siblings = siblings,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(draft, currentLabel) },
                enabled = draft.name.isNotBlank() && (!needsCurrentLabel || currentLabel.isNotBlank()),
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
