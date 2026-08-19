package org.bhargav.pansariwala.settings

import org.jetbrains.compose.resources.StringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.cancel_reason_customer_request
import pansariwala.shared.generated.resources.cancel_reason_duplicate
import pansariwala.shared.generated.resources.cancel_reason_out_of_stock
import pansariwala.shared.generated.resources.cancel_reason_other
import pansariwala.shared.generated.resources.cancel_reason_payment_issue

enum class CancelOrderReason(val labelRes: StringResource) {
    CUSTOMER_REQUEST(Res.string.cancel_reason_customer_request),
    OUT_OF_STOCK(Res.string.cancel_reason_out_of_stock),
    PAYMENT_ISSUE(Res.string.cancel_reason_payment_issue),
    DUPLICATE(Res.string.cancel_reason_duplicate),
    OTHER(Res.string.cancel_reason_other),
}
