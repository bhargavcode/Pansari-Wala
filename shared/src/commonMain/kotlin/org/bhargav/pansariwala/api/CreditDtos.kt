package org.bhargav.pansariwala.api

import kotlinx.serialization.Serializable

/** A customer the shop gave goods on credit (udhaar), identified by mobile number. */
@Serializable
data class BorrowerDto(
    val phone: String,
    val name: String = "",
    val photoUrl: String? = null,
    val address: String = "",
    val notes: String = "",
    val outstanding: Double = 0.0,
    val totalCredit: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val orderCount: Int = 0,
    val lastActivityAt: Long = 0L,
    val createdAt: Long = 0L,
)

@Serializable
data class CreditDayDto(
    val dayStartMs: Long,
    val credit: Double = 0.0,
    val repaid: Double = 0.0,
)

@Serializable
data class CreditShareDto(
    val phone: String,
    val name: String = "",
    val amount: Double = 0.0,
)

@Serializable
data class BorrowerReportDto(
    val totalOutstanding: Double = 0.0,
    val creditInRange: Double = 0.0,
    val repaidInRange: Double = 0.0,
    val borrowerCount: Int = 0,
    val shares: List<CreditShareDto> = emptyList(),
    val days: List<CreditDayDto> = emptyList(),
    val borrowers: List<BorrowerDto> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class CreditTxnItemDto(
    val name: String,
    val variantLabel: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "",
    val lineTotal: Double = 0.0,
)

@Serializable
data class CreditTxnDto(
    val id: String,
    val shopId: String,
    val shopName: String = "",
    val phone: String,
    /** CREDIT = goods bought on udhaar, REPAYMENT = money paid back. */
    val type: String,
    val amount: Double,
    /** SUCCESS / PENDING / FAILED; only SUCCESS repayments reduce the balance. */
    val status: String = "SUCCESS",
    val paymentMode: String = "",
    val receivedBy: String = "",
    val processedBy: String = "",
    /** POS or USER_APP. */
    val source: String = "POS",
    val orderId: String? = null,
    val items: List<CreditTxnItemDto> = emptyList(),
    val note: String = "",
    val createdAt: Long = 0L,
    /** App repayments: CASHFREE or UPI_DIRECT. */
    val gateway: String = "",
    val paymentRef: String? = null,
    val failureReason: String = "",
)

@Serializable
data class BorrowerDetailDto(
    val borrower: BorrowerDto,
    val transactions: List<CreditTxnDto> = emptyList(),
    /** Set when the request was paged (`page` query param); otherwise every row is returned. */
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class BorrowerUpdateRequest(
    val name: String = "",
    val address: String = "",
    val notes: String = "",
    val photoUrl: String? = null,
)

@Serializable
data class RepaymentRequest(
    val amount: Double,
    val paymentMode: String,
    val receivedBy: String = "",
    val note: String = "",
)

@Serializable
data class MyCreditShopDto(
    val shopId: String,
    val shopName: String = "",
    val shopImageUrl: String? = null,
    val shopAddress: String = "",
    val outstanding: Double = 0.0,
    val totalCredit: Double = 0.0,
    val totalRepaid: Double = 0.0,
    val lastActivityAt: Long = 0L,
)

@Serializable
data class MyCreditSummaryDto(
    val totalOutstanding: Double = 0.0,
    val shops: List<MyCreditShopDto> = emptyList(),
)

@Serializable
data class MyCreditShopDetailDto(
    val shop: MyCreditShopDto,
    val transactions: List<CreditTxnDto> = emptyList(),
)

@Serializable
data class MyRepayRequest(val amount: Double)

@Serializable
data class MyRepayResponse(
    val txn: CreditTxnDto,
    val upiId: String = "",
    val payeeName: String = "",
    /** Ready `upi://pay` link from the active gateway; when blank the app builds one from [upiId]. */
    val payUri: String = "",
    val gateway: String = "",
)

@Serializable
data class MyRepayStatusRequest(val success: Boolean)
