package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val barcode: String = "",
    val category: String = "Produce",
    val imageUrl: String = "",
    val unit: String = "pc",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val stockQuantity: Long = 0L,
    val reorderThreshold: Long = 10L,
    val reorderQuantity: Long = 25L,
    val supplierId: String = "",
    val supplierName: String = "",
    val totalSoldUnits: Long = 0L,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveSellingPrice(): Double {
        val factor = (100.0 - discountPercent.coerceIn(0.0, 100.0)) / 100.0
        return ((sellingPrice * factor) * 100.0).roundToInt() / 100.0
    }

    fun unitDiscountAmount(): Double {
        return ((sellingPrice - effectiveSellingPrice()) * 100.0).roundToInt() / 100.0
    }

    fun unitProfit(): Double {
        return ((effectiveSellingPrice() - costPrice) * 100.0).roundToInt() / 100.0
    }

    fun profitMarginPercent(): Double {
        val netPrice = effectiveSellingPrice()
        if (netPrice <= 0.0) return 0.0
        return (((netPrice - costPrice) / netPrice) * 1000.0).roundToInt() / 10.0
    }

    fun isLowStock(): Boolean = stockQuantity <= reorderThreshold

    fun isOutOfStock(): Boolean = stockQuantity <= 0L

    fun toWriteMap(uid: String, isUpdate: Boolean): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim().take(120),
            "barcode" to barcode.trim().take(64),
            "category" to category.trim().take(60),
            "imageUrl" to imageUrl.trim().take(500),
            "unit" to unit.trim().ifEmpty { "pc" }.take(30),
            "costPrice" to costPrice.coerceAtLeast(0.0),
            "sellingPrice" to sellingPrice.coerceAtLeast(0.0),
            "discountPercent" to discountPercent.coerceIn(0.0, 100.0),
            "stockQuantity" to stockQuantity.coerceAtLeast(0L),
            "reorderThreshold" to reorderThreshold.coerceAtLeast(0L),
            "reorderQuantity" to reorderQuantity.coerceAtLeast(1L),
            "supplierId" to supplierId.trim().take(128),
            "supplierName" to supplierName.trim().take(120),
            "totalSoldUnits" to totalSoldUnits.coerceAtLeast(0L),
            "totalRevenue" to totalRevenue.coerceAtLeast(0.0),
            "totalProfit" to totalProfit,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isUpdate) {
            base["createdAt"] = createdAt ?: FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            base["createdAt"] = createdAt
        }
        return base
    }
}

data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val unit: String = "pc",
    val quantity: Long = 1L,
    val unitCost: Double = 0.0,
    val regularUnitPrice: Double = 0.0,
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val lineTotal: Double = 0.0,
    val lineProfit: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "barcode" to barcode,
        "unit" to unit,
        "quantity" to quantity,
        "unitCost" to unitCost,
        "regularUnitPrice" to regularUnitPrice,
        "unitPrice" to unitPrice,
        "discountPercent" to discountPercent,
        "lineTotal" to lineTotal,
        "lineProfit" to lineProfit
    )

    companion object {
        fun fromMap(map: Map<*, *>): OrderLineItem {
            return OrderLineItem(
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                barcode = map["barcode"] as? String ?: "",
                unit = map["unit"] as? String ?: "pc",
                quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
                unitCost = (map["unitCost"] as? Number)?.toDouble() ?: 0.0,
                regularUnitPrice = (map["regularUnitPrice"] as? Number)?.toDouble() ?: 0.0,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                discountPercent = (map["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                lineTotal = (map["lineTotal"] as? Number)?.toDouble() ?: 0.0,
                lineProfit = (map["lineProfit"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@IgnoreExtraProperties
data class CustomerOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val customerName: String = "Walk-In Customer",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val orderType: String = "WALK_IN", // WALK_IN | DELIVERY
    val status: String = "COMPLETED", // PENDING | PACKED | OUT_FOR_DELIVERY | COMPLETED | CANCELLED
    val paymentMethod: String = "CASH", // CASH | CARD | UPI
    val itemsSummary: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalItemsCount: Long = 1L,
    val subtotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val orderDateEpochDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedLineItems(): List<OrderLineItem> = items.map { OrderLineItem.fromMap(it) }

    fun toWriteMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "orderNumber" to orderNumber.trim().take(60),
        "customerName" to customerName.trim().ifEmpty { "Walk-In Customer" }.take(120),
        "customerPhone" to customerPhone.trim().take(40),
        "deliveryAddress" to deliveryAddress.trim().take(300),
        "orderType" to orderType,
        "status" to status,
        "paymentMethod" to paymentMethod,
        "itemsSummary" to itemsSummary.trim().ifEmpty { "1 item" }.take(1000),
        "items" to items,
        "totalItemsCount" to totalItemsCount.coerceAtLeast(1L),
        "subtotal" to subtotal.coerceAtLeast(0.0),
        "discountTotal" to discountTotal.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.0),
        "totalCost" to totalCost.coerceAtLeast(0.0),
        "netProfit" to netProfit,
        "orderDateEpochDay" to orderDateEpochDay.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Supplier(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val category: String = "Produce",
    val address: String = "",
    val leadTimeDays: Long = 2L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(uid: String, isUpdate: Boolean): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim().take(120),
            "contactPerson" to contactPerson.trim().take(120),
            "phone" to phone.trim().take(40),
            "email" to email.trim().take(120),
            "category" to category.trim().take(60),
            "address" to address.trim().take(250),
            "leadTimeDays" to leadTimeDays.coerceIn(0L, 90L),
            "notes" to notes.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isUpdate) {
            base["createdAt"] = createdAt ?: FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            base["createdAt"] = createdAt
        }
        return base
    }
}

@IgnoreExtraProperties
data class RestockOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val quantityOrdered: Long = 10L,
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val status: String = "ORDERED", // ORDERED | RECEIVED | CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "supplierId" to supplierId.trim().take(128),
        "supplierName" to supplierName.trim().ifEmpty { "Direct Wholesale" }.take(120),
        "productId" to productId.trim().take(128),
        "productName" to productName.trim().take(120),
        "barcode" to barcode.trim().take(64),
        "quantityOrdered" to quantityOrdered.coerceAtLeast(1L),
        "unitCost" to unitCost.coerceAtLeast(0.0),
        "totalCost" to totalCost.coerceAtLeast(0.0),
        "status" to status,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ShopExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Utilities",
    val amount: Double = 0.0,
    val notes: String = "",
    val expenseDateEpochDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().take(120),
        "category" to category.trim().take(60),
        "amount" to amount.coerceAtLeast(0.01),
        "notes" to notes.trim().take(300),
        "expenseDateEpochDay" to expenseDateEpochDay.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class CartItem(
    val product: Product,
    val quantity: Long = 1L,
    val customDiscountPercent: Double? = null
) {
    val activeDiscountPercent: Double
        get() = (customDiscountPercent ?: product.discountPercent).coerceIn(0.0, 100.0)

    val effectiveUnitPrice: Double
        get() {
            val factor = (100.0 - activeDiscountPercent) / 100.0
            return ((product.sellingPrice * factor) * 100.0).roundToInt() / 100.0
        }

    val lineSubtotal: Double
        get() = ((product.sellingPrice * quantity) * 100.0).roundToInt() / 100.0

    val lineTotal: Double
        get() = ((effectiveUnitPrice * quantity) * 100.0).roundToInt() / 100.0

    val lineDiscount: Double
        get() = ((lineSubtotal - lineTotal) * 100.0).roundToInt() / 100.0

    val lineCost: Double
        get() = ((product.costPrice * quantity) * 100.0).roundToInt() / 100.0

    val lineProfit: Double
        get() = ((lineTotal - lineCost) * 100.0).roundToInt() / 100.0
}

data class DailyProfitPoint(
    val epochDay: Long,
    val label: String,
    val revenue: Double,
    val cogs: Double,
    val expenses: Double,
    val discountsGiven: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val ordersCount: Int
)

data class ProfitLossSummary(
    val totalRevenue: Double = 0.0,
    val totalCogs: Double = 0.0,
    val totalDiscountsGiven: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfitOrLoss: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val todayOrdersCount: Int = 0,
    val inventoryCostValuation: Double = 0.0,
    val inventoryRetailValuation: Double = 0.0,
    val dailySeries: List<DailyProfitPoint> = emptyList()
)
