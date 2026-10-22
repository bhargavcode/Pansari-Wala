package org.bhargav.pansariwala.i18n

import androidx.compose.runtime.Composable
import org.bhargav.pansariwala.api.MasterProductDto
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.anonymous_customer
import pansariwala.shared.generated.resources.category_fortune_branded
import pansariwala.shared.generated.resources.category_general_grocery
import pansariwala.shared.generated.resources.category_high_value_spices
import pansariwala.shared.generated.resources.category_puja_samagri
import pansariwala.shared.generated.resources.category_standard_spices
import pansariwala.shared.generated.resources.unit_g
import pansariwala.shared.generated.resources.unit_kg
import pansariwala.shared.generated.resources.unit_ltr
import pansariwala.shared.generated.resources.unit_packet
import pansariwala.shared.generated.resources.unit_pcs
import pansariwala.shared.generated.resources.walk_in

/** Product name in the current app language (falls back to English). */
val Product.localizedName: String
    @Composable get() = displayName(LocalAppLocale.current)

val MasterProductDto.localizedName: String
    @Composable get() = displayName(LocalAppLocale.current)

/** Outside composition (ViewModels): the language chosen in settings, English while following the system. */
val Product.appLanguageName: String
    get() = displayName(customAppLocale)

val ProductCategory.labelRes: StringResource
    get() = when (this) {
        ProductCategory.FORTUNE_BRANDED -> Res.string.category_fortune_branded
        ProductCategory.GENERAL_GROCERY -> Res.string.category_general_grocery
        ProductCategory.PUJA_SAMAGRI -> Res.string.category_puja_samagri
        ProductCategory.STANDARD_SPICES -> Res.string.category_standard_spices
        ProductCategory.HIGH_VALUE_SPICES -> Res.string.category_high_value_spices
    }

val ProductUnit.labelRes: StringResource
    get() = when (this) {
        ProductUnit.KG -> Res.string.unit_kg
        ProductUnit.LITRE -> Res.string.unit_ltr
        ProductUnit.GRAM -> Res.string.unit_g
        ProductUnit.PACKET -> Res.string.unit_packet
        ProductUnit.PIECE -> Res.string.unit_pcs
    }

@Composable
fun ProductCategory.localizedName(): String = stringResource(labelRes)

@Composable
fun ProductUnit.localizedLabel(): String = stringResource(labelRes)

/** Stored default customer name when blank; localize for display. */
const val WALK_IN_CUSTOMER_KEY: String = "Walk-in"

/** Stored customer name for stock sold earlier without a bill (recorded from POS). */
const val ANONYMOUS_CUSTOMER_KEY: String = "Anonymous"

@Composable
fun localizeCustomerName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    return when {
        trimmed.isEmpty() || trimmed.equals(WALK_IN_CUSTOMER_KEY, ignoreCase = true) -> stringResource(Res.string.walk_in)
        trimmed.equals(ANONYMOUS_CUSTOMER_KEY, ignoreCase = true) -> stringResource(Res.string.anonymous_customer)
        else -> trimmed
    }
}
