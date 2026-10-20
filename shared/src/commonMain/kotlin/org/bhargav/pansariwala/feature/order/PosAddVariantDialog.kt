package org.bhargav.pansariwala.feature.order

import org.bhargav.pansariwala.designsystem.AppBottomSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.variant_add_variant_title, product.name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(
                    onClick = { onSave(draft, currentLabel) },
                    enabled = draft.name.isNotBlank() && (!needsCurrentLabel || currentLabel.isNotBlank()),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}
