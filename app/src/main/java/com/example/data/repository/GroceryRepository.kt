package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.RestockOrder
import com.example.data.model.ShopExpense
import com.example.data.model.Supplier
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

class GroceryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // Convenience constructor using custom database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

    // --- Real-Time Observation Flows ---

    fun observeProducts(userId: String = requireUserId()): Flow<List<Product>> = flow {
        val path = "users/$userId/products"
        emitAll(
            db.collection("users").document(userId).collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOrders(userId: String = requireUserId()): Flow<List<CustomerOrder>> = flow {
        val path = "users/$userId/orders"
        emitAll(
            db.collection("users").document(userId).collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSuppliers(userId: String = requireUserId()): Flow<List<Supplier>> = flow {
        val path = "users/$userId/suppliers"
        emitAll(
            db.collection("users").document(userId).collection("suppliers")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Supplier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeRestockOrders(userId: String = requireUserId()): Flow<List<RestockOrder>> = flow {
        val path = "users/$userId/restock_orders"
        emitAll(
            db.collection("users").document(userId).collection("restock_orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(RestockOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeExpenses(userId: String = requireUserId()): Flow<List<ShopExpense>> = flow {
        val path = "users/$userId/expenses"
        emitAll(
            db.collection("users").document(userId).collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ShopExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.expenseDateEpochDay }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- Direct Read & Write Operations ---

    suspend fun getProductById(targetUserId: String, productId: String): Result<Product?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("products").document(productId)
        try {
            docRef.get().await().toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertProduct(product: Product): Result<String> = runCatching {
        val uid = requireUserId()
        val isUpdate = product.id.isNotBlank() && product.createdAt != null
        val docId = if (product.id.isNotBlank()) product.id else "prod_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("products").document(docId)
        try {
            if (isUpdate) {
                val updates = mapOf(
                    "name" to product.name.trim().take(120),
                    "barcode" to product.barcode.trim().take(64),
                    "category" to product.category.trim().take(60),
                    "imageUrl" to product.imageUrl.trim().take(500),
                    "unit" to product.unit.trim().ifEmpty { "pc" }.take(30),
                    "costPrice" to product.costPrice.coerceAtLeast(0.0),
                    "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
                    "discountPercent" to product.discountPercent.coerceIn(0.0, 100.0),
                    "stockQuantity" to product.stockQuantity.coerceAtLeast(0L),
                    "reorderThreshold" to product.reorderThreshold.coerceAtLeast(0L),
                    "reorderQuantity" to product.reorderQuantity.coerceAtLeast(1L),
                    "supplierId" to product.supplierId.trim().take(128),
                    "supplierName" to product.supplierName.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                docRef.set(product.toWriteMap(uid, isUpdate = false)).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun adjustProductStock(productId: String, newQuantity: Long, newDiscountPercent: Double? = null): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        try {
            val updates = mutableMapOf<String, Any>(
                "stockQuantity" to newQuantity.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newDiscountPercent != null) {
                updates["discountPercent"] = newDiscountPercent.coerceIn(0.0, 100.0)
            }
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createCheckoutOrder(
        order: CustomerOrder,
        currentProducts: List<Product>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val orderId = if (order.id.isNotBlank()) order.id else "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val orderRef = db.collection("users").document(uid).collection("orders").document(orderId)
        val productMap = currentProducts.associateBy { it.id }

        try {
            val batch = db.batch()
            batch.set(orderRef, order.toWriteMap(uid))

            order.parsedLineItems().forEach { line ->
                val prod = productMap[line.productId]
                if (prod != null) {
                    val prodRef = db.collection("users").document(uid).collection("products").document(prod.id)
                    val updatedStock = (prod.stockQuantity - line.quantity).coerceAtLeast(0L)
                    val updatedSold = prod.totalSoldUnits + line.quantity
                    val updatedRevenue = ((prod.totalRevenue + line.lineTotal) * 100.0).roundToInt() / 100.0
                    val updatedProfit = ((prod.totalProfit + line.lineProfit) * 100.0).roundToInt() / 100.0
                    batch.update(
                        prodRef,
                        mapOf(
                            "stockQuantity" to updatedStock,
                            "totalSoldUnits" to updatedSold,
                            "totalRevenue" to updatedRevenue,
                            "totalProfit" to updatedProfit,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            orderId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            throw e
        }
    }

    suspend fun updateOrderStatus(
        order: CustomerOrder,
        newStatus: String,
        currentProducts: List<Product>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val orderRef = db.collection("users").document(uid).collection("orders").document(order.id)
        val productMap = currentProducts.associateBy { it.id }

        try {
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // If cancelling an active order, restore stock quantities and reverse cumulative sales stats
            if (newStatus == "CANCELLED" && order.status != "CANCELLED") {
                order.parsedLineItems().forEach { line ->
                    val prod = productMap[line.productId]
                    if (prod != null) {
                        val prodRef = db.collection("users").document(uid).collection("products").document(prod.id)
                        val restoredStock = prod.stockQuantity + line.quantity
                        val revertedSold = (prod.totalSoldUnits - line.quantity).coerceAtLeast(0L)
                        val revertedRevenue = (prod.totalRevenue - line.lineTotal).coerceAtLeast(0.0)
                        val revertedProfit = prod.totalProfit - line.lineProfit
                        batch.update(
                            prodRef,
                            mapOf(
                                "stockQuantity" to restoredStock,
                                "totalSoldUnits" to revertedSold,
                                "totalRevenue" to ((revertedRevenue * 100.0).roundToInt() / 100.0),
                                "totalProfit" to ((revertedProfit * 100.0).roundToInt() / 100.0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            throw e
        }
    }

    suspend fun upsertSupplier(supplier: Supplier): Result<String> = runCatching {
        val uid = requireUserId()
        val isUpdate = supplier.id.isNotBlank() && supplier.createdAt != null
        val docId = if (supplier.id.isNotBlank()) supplier.id else "sup_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("suppliers").document(docId)
        try {
            if (isUpdate) {
                docRef.update(
                    mapOf(
                        "name" to supplier.name.trim().take(120),
                        "contactPerson" to supplier.contactPerson.trim().take(120),
                        "phone" to supplier.phone.trim().take(40),
                        "email" to supplier.email.trim().take(120),
                        "category" to supplier.category.trim().take(60),
                        "address" to supplier.address.trim().take(250),
                        "leadTimeDays" to supplier.leadTimeDays.coerceIn(0L, 90L),
                        "notes" to supplier.notes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(supplier.toWriteMap(uid, isUpdate = false)).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSupplier(supplierId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("suppliers").document(supplierId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createRestockOrder(restockOrder: RestockOrder): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = if (restockOrder.id.isNotBlank()) restockOrder.id else "po_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("restock_orders").document(docId)
        try {
            docRef.set(restockOrder.toWriteMap(uid)).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markRestockOrderReceived(
        restockOrder: RestockOrder,
        currentProduct: Product?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val poRef = db.collection("users").document(uid).collection("restock_orders").document(restockOrder.id)
        try {
            val batch = db.batch()
            batch.update(
                poRef,
                mapOf(
                    "status" to "RECEIVED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (currentProduct != null) {
                val prodRef = db.collection("users").document(uid).collection("products").document(currentProduct.id)
                batch.update(
                    prodRef,
                    mapOf(
                        "stockQuantity" to (currentProduct.stockQuantity + restockOrder.quantityOrdered),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, poRef.path)
            throw e
        }
    }

    suspend fun cancelRestockOrder(restockOrder: RestockOrder): Result<Unit> = runCatching {
        val uid = requireUserId()
        val poRef = db.collection("users").document(uid).collection("restock_orders").document(restockOrder.id)
        try {
            poRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, poRef.path)
            throw e
        }
    }

    suspend fun addExpense(expense: ShopExpense): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = if (expense.id.isNotBlank()) expense.id else "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("expenses").document(docId)
        try {
            docRef.set(expense.toWriteMap(uid)).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun seedSampleGroceryStore(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val today = currentEpochDay()
        val batch = db.batch()

        // 1. Sample Suppliers
        val suppliers = listOf(
            Supplier(
                id = "sup_greenvalley",
                userId = uid,
                name = "GreenValley Organic Farms",
                contactPerson = "Elena Martinez",
                phone = "+1 (555) 234-8910",
                email = "orders@greenvalleyfarms.com",
                category = "Produce",
                address = "420 Harvest Lane, Salinas Valley",
                leadTimeDays = 1L,
                notes = "Daily morning delivery for fresh organic produce"
            ),
            Supplier(
                id = "sup_meadowbrook",
                userId = uid,
                name = "MeadowBrook Creamery & Bakery",
                contactPerson = "Marcus Vance",
                phone = "+1 (555) 876-5412",
                email = "supply@meadowbrook.io",
                category = "Dairy",
                address = "88 Dairy Road, Petaluma",
                leadTimeDays = 2L,
                notes = "Cold-chain milk, cheese, eggs, and sourdough"
            ),
            Supplier(
                id = "sup_pantryprime",
                userId = uid,
                name = "PrimePantry Wholesale Foods",
                contactPerson = "Rajesh Patel",
                phone = "+1 (555) 390-1122",
                email = "wholesale@primepantry.com",
                category = "Pantry",
                address = "1900 Logistics Pkwy, Suite 300",
                leadTimeDays = 3L,
                notes = "Grains, olive oil, coffee, and bulk beverages"
            )
        )
        suppliers.forEach { sup ->
            val ref = db.collection("users").document(uid).collection("suppliers").document(sup.id)
            batch.set(ref, sup.toWriteMap(uid, isUpdate = false))
        }

        // 2. Sample Products (including discounted items and low-stock items to demonstrate alerts)
        val products = listOf(
            Product(
                id = "prod_avocado",
                userId = uid,
                name = "Organic Hass Avocados (Bag of 4)",
                barcode = "8901001001",
                category = "Produce",
                imageUrl = "produce",
                unit = "bag",
                costPrice = 3.10,
                sellingPrice = 5.99,
                discountPercent = 10.0,
                stockQuantity = 28L,
                reorderThreshold = 10L,
                reorderQuantity = 30L,
                supplierId = "sup_greenvalley",
                supplierName = "GreenValley Organic Farms",
                totalSoldUnits = 42L,
                totalRevenue = 226.42,
                totalProfit = 96.22
            ),
            Product(
                id = "prod_milk_a2",
                userId = uid,
                name = "Pasture-Raised Whole Milk A2 (1L)",
                barcode = "8901001002",
                category = "Dairy",
                imageUrl = "dairy",
                unit = "bottle",
                costPrice = 2.40,
                sellingPrice = 4.50,
                discountPercent = 0.0,
                stockQuantity = 6L, // Low stock! (<= threshold 12)
                reorderThreshold = 12L,
                reorderQuantity = 36L,
                supplierId = "sup_meadowbrook",
                supplierName = "MeadowBrook Creamery & Bakery",
                totalSoldUnits = 64L,
                totalRevenue = 288.00,
                totalProfit = 134.40
            ),
            Product(
                id = "prod_sourdough",
                userId = uid,
                name = "Artisan Country Sourdough Loaf",
                barcode = "8901001003",
                category = "Bakery",
                imageUrl = "bakery",
                unit = "loaf",
                costPrice = 2.80,
                sellingPrice = 6.20,
                discountPercent = 15.0,
                stockQuantity = 4L, // Low stock! (<= threshold 8)
                reorderThreshold = 8L,
                reorderQuantity = 20L,
                supplierId = "sup_meadowbrook",
                supplierName = "MeadowBrook Creamery & Bakery",
                totalSoldUnits = 38L,
                totalRevenue = 200.26,
                totalProfit = 93.86
            ),
            Product(
                id = "prod_olive_oil",
                userId = uid,
                name = "Cold-Pressed Extra Virgin Olive Oil (750ml)",
                barcode = "8901001004",
                category = "Pantry",
                imageUrl = "pantry",
                unit = "bottle",
                costPrice = 8.50,
                sellingPrice = 15.99,
                discountPercent = 5.0,
                stockQuantity = 19L,
                reorderThreshold = 6L,
                reorderQuantity = 18L,
                supplierId = "sup_pantryprime",
                supplierName = "PrimePantry Wholesale Foods",
                totalSoldUnits = 25L,
                totalRevenue = 379.75,
                totalProfit = 167.25
            ),
            Product(
                id = "prod_strawberries",
                userId = uid,
                name = "Sweet Albion Strawberries (500g)",
                barcode = "8901001005",
                category = "Produce",
                imageUrl = "produce",
                unit = "punnet",
                costPrice = 2.15,
                sellingPrice = 4.80,
                discountPercent = 20.0,
                stockQuantity = 5L, // Low stock! (<= threshold 10)
                reorderThreshold = 10L,
                reorderQuantity = 25L,
                supplierId = "sup_greenvalley",
                supplierName = "GreenValley Organic Farms",
                totalSoldUnits = 50L,
                totalRevenue = 192.00,
                totalProfit = 84.50
            ),
            Product(
                id = "prod_cold_brew",
                userId = uid,
                name = "Single-Origin Oat Milk Cold Brew (330ml)",
                barcode = "8901001006",
                category = "Beverages",
                imageUrl = "beverages",
                unit = "can",
                costPrice = 1.60,
                sellingPrice = 3.50,
                discountPercent = 0.0,
                stockQuantity = 34L,
                reorderThreshold = 10L,
                reorderQuantity = 48L,
                supplierId = "sup_pantryprime",
                supplierName = "PrimePantry Wholesale Foods",
                totalSoldUnits = 55L,
                totalRevenue = 192.50,
                totalProfit = 104.50
            )
        )
        products.forEach { prod ->
            val ref = db.collection("users").document(uid).collection("products").document(prod.id)
            batch.set(ref, prod.toWriteMap(uid, isUpdate = false))
        }

        // 3. Sample Customer Orders across last 5 days
        val sampleOrders = listOf(
            CustomerOrder(
                id = "ord_sample_1",
                userId = uid,
                orderNumber = "ORD-1041",
                customerName = "Sophia Chen",
                customerPhone = "555-0192",
                deliveryAddress = "742 Evergreen Terrace, Apt 4B",
                orderType = "DELIVERY",
                status = "OUT_FOR_DELIVERY",
                paymentMethod = "CARD",
                itemsSummary = "2x Organic Hass Avocados, 2x Pasture-Raised Whole Milk A2",
                items = listOf(
                    OrderLineItem(
                        productId = "prod_avocado",
                        productName = "Organic Hass Avocados (Bag of 4)",
                        barcode = "8901001001",
                        unit = "bag",
                        quantity = 2L,
                        unitCost = 3.10,
                        regularUnitPrice = 5.99,
                        unitPrice = 5.39,
                        discountPercent = 10.0,
                        lineTotal = 10.78,
                        lineProfit = 4.58
                    ).toMap(),
                    OrderLineItem(
                        productId = "prod_milk_a2",
                        productName = "Pasture-Raised Whole Milk A2 (1L)",
                        barcode = "8901001002",
                        unit = "bottle",
                        quantity = 2L,
                        unitCost = 2.40,
                        regularUnitPrice = 4.50,
                        unitPrice = 4.50,
                        discountPercent = 0.0,
                        lineTotal = 9.00,
                        lineProfit = 4.20
                    ).toMap()
                ),
                totalItemsCount = 4L,
                subtotal = 20.98,
                discountTotal = 1.20,
                totalAmount = 19.78,
                totalCost = 11.00,
                netProfit = 8.78,
                orderDateEpochDay = today
            ),
            CustomerOrder(
                id = "ord_sample_2",
                userId = uid,
                orderNumber = "ORD-1040",
                customerName = "Walk-In Customer",
                customerPhone = "",
                deliveryAddress = "",
                orderType = "WALK_IN",
                status = "COMPLETED",
                paymentMethod = "UPI",
                itemsSummary = "2x Cold-Pressed Extra Virgin Olive Oil, 3x Artisan Sourdough",
                items = listOf(
                    OrderLineItem(
                        productId = "prod_olive_oil",
                        productName = "Cold-Pressed Extra Virgin Olive Oil (750ml)",
                        barcode = "8901001004",
                        unit = "bottle",
                        quantity = 2L,
                        unitCost = 8.50,
                        regularUnitPrice = 15.99,
                        unitPrice = 15.19,
                        discountPercent = 5.0,
                        lineTotal = 30.38,
                        lineProfit = 13.38
                    ).toMap(),
                    OrderLineItem(
                        productId = "prod_sourdough",
                        productName = "Artisan Country Sourdough Loaf",
                        barcode = "8901001003",
                        unit = "loaf",
                        quantity = 3L,
                        unitCost = 2.80,
                        regularUnitPrice = 6.20,
                        unitPrice = 5.27,
                        discountPercent = 15.0,
                        lineTotal = 15.81,
                        lineProfit = 7.41
                    ).toMap()
                ),
                totalItemsCount = 5L,
                subtotal = 50.58,
                discountTotal = 4.39,
                totalAmount = 46.19,
                totalCost = 25.40,
                netProfit = 20.79,
                orderDateEpochDay = today
            ),
            CustomerOrder(
                id = "ord_sample_3",
                userId = uid,
                orderNumber = "ORD-1039",
                customerName = "Liam O'Connor",
                customerPhone = "555-0148",
                deliveryAddress = "18 Maple Ave",
                orderType = "DELIVERY",
                status = "COMPLETED",
                paymentMethod = "CASH",
                itemsSummary = "4x Sweet Albion Strawberries, 4x Cold Brew",
                items = listOf(
                    OrderLineItem(
                        productId = "prod_strawberries",
                        productName = "Sweet Albion Strawberries (500g)",
                        barcode = "8901001005",
                        unit = "punnet",
                        quantity = 4L,
                        unitCost = 2.15,
                        regularUnitPrice = 4.80,
                        unitPrice = 3.84,
                        discountPercent = 20.0,
                        lineTotal = 15.36,
                        lineProfit = 6.76
                    ).toMap(),
                    OrderLineItem(
                        productId = "prod_cold_brew",
                        productName = "Single-Origin Oat Milk Cold Brew (330ml)",
                        barcode = "8901001006",
                        unit = "can",
                        quantity = 4L,
                        unitCost = 1.60,
                        regularUnitPrice = 3.50,
                        unitPrice = 3.50,
                        discountPercent = 0.0,
                        lineTotal = 14.00,
                        lineProfit = 7.60
                    ).toMap()
                ),
                totalItemsCount = 8L,
                subtotal = 33.20,
                discountTotal = 3.84,
                totalAmount = 29.36,
                totalCost = 15.00,
                netProfit = 14.36,
                orderDateEpochDay = today - 1L
            ),
            CustomerOrder(
                id = "ord_sample_4",
                userId = uid,
                orderNumber = "ORD-1038",
                customerName = "Walk-In Customer",
                customerPhone = "",
                deliveryAddress = "",
                orderType = "WALK_IN",
                status = "COMPLETED",
                paymentMethod = "CARD",
                itemsSummary = "5x Pasture-Raised Whole Milk A2, 3x Organic Hass Avocados",
                items = listOf(
                    OrderLineItem(
                        productId = "prod_milk_a2",
                        productName = "Pasture-Raised Whole Milk A2 (1L)",
                        barcode = "8901001002",
                        unit = "bottle",
                        quantity = 5L,
                        unitCost = 2.40,
                        regularUnitPrice = 4.50,
                        unitPrice = 4.50,
                        discountPercent = 0.0,
                        lineTotal = 22.50,
                        lineProfit = 10.50
                    ).toMap(),
                    OrderLineItem(
                        productId = "prod_avocado",
                        productName = "Organic Hass Avocados (Bag of 4)",
                        barcode = "8901001001",
                        unit = "bag",
                        quantity = 3L,
                        unitCost = 3.10,
                        regularUnitPrice = 5.99,
                        unitPrice = 5.39,
                        discountPercent = 10.0,
                        lineTotal = 16.17,
                        lineProfit = 6.87
                    ).toMap()
                ),
                totalItemsCount = 8L,
                subtotal = 40.47,
                discountTotal = 1.80,
                totalAmount = 38.67,
                totalCost = 21.30,
                netProfit = 17.37,
                orderDateEpochDay = today - 2L
            )
        )
        sampleOrders.forEach { ord ->
            val ref = db.collection("users").document(uid).collection("orders").document(ord.id)
            batch.set(ref, ord.toWriteMap(uid))
        }

        // 4. Sample Supplier Restock Order
        val sampleRestock = RestockOrder(
            id = "po_sample_1",
            userId = uid,
            supplierId = "sup_meadowbrook",
            supplierName = "MeadowBrook Creamery & Bakery",
            productId = "prod_milk_a2",
            productName = "Pasture-Raised Whole Milk A2 (1L)",
            barcode = "8901001002",
            quantityOrdered = 36L,
            unitCost = 2.40,
            totalCost = 86.40,
            status = "ORDERED"
        )
        val poRef = db.collection("users").document(uid).collection("restock_orders").document(sampleRestock.id)
        batch.set(poRef, sampleRestock.toWriteMap(uid))

        // 5. Sample Shop Operating Expenses
        val sampleExpenses = listOf(
            ShopExpense(
                id = "exp_sample_1",
                userId = uid,
                title = "Cold-Storage & Display Chiller Electricity",
                category = "Utilities",
                amount = 8.50,
                notes = "Daily pro-rated refrigeration power",
                expenseDateEpochDay = today
            ),
            ShopExpense(
                id = "exp_sample_2",
                userId = uid,
                title = "Biodegradable Kraft Carry Bags & Labels",
                category = "Packaging",
                amount = 4.20,
                notes = "POS checkout & delivery packaging",
                expenseDateEpochDay = today - 1L
            )
        )
        sampleExpenses.forEach { exp ->
            val ref = db.collection("users").document(uid).collection("expenses").document(exp.id)
            batch.set(ref, exp.toWriteMap(uid))
        }

        batch.commit().await()
    }
}
