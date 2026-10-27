package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.DailyProfitPoint
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.RestockOrder
import com.example.data.model.ShopExpense
import com.example.data.model.Supplier
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ScannerWorkflowMode {
    POS_CHECKOUT,
    SECURE_INVENTORY_UPDATE
}

data class GroceryUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val orders: List<CustomerOrder> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val restockOrders: List<RestockOrder> = emptyList(),
    val expenses: List<ShopExpense> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val topSellingProducts: List<Product> = emptyList(),
    val profitLossSummary: ProfitLossSummary = ProfitLossSummary(),
    val cartItems: List<CartItem> = emptyList(),
    val scannerMode: ScannerWorkflowMode = ScannerWorkflowMode.POS_CHECKOUT,
    val scannedProductForStockUpdate: Product? = null,
    val lastScannedBanner: String? = null,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
)

class GroceryViewModel(
    private val repository: GroceryRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _scannerMode = MutableStateFlow(ScannerWorkflowMode.POS_CHECKOUT)
    private val _scannedProductIdForUpdate = MutableStateFlow<String?>(null)
    private val _lastScannedBanner = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val alertedLowStockIds = mutableSetOf<String>()
    private val _newlyTriggeredLowStock = MutableStateFlow<List<Product>>(emptyList())
    val newlyTriggeredLowStock: StateFlow<List<Product>> = _newlyTriggeredLowStock.asStateFlow()

    private val productsFlow = repository.observeProducts(currentUserId)
        .catch { e ->
            _errorMessage.value = e.localizedMessage ?: "Failed to load products"
            emit(emptyList())
        }

    private val ordersFlow = repository.observeOrders(currentUserId)
        .catch { e ->
            _errorMessage.value = e.localizedMessage ?: "Failed to load orders"
            emit(emptyList())
        }

    private val suppliersFlow = repository.observeSuppliers(currentUserId)
        .catch { e ->
            _errorMessage.value = e.localizedMessage ?: "Failed to load suppliers"
            emit(emptyList())
        }

    private val restockFlow = repository.observeRestockOrders(currentUserId)
        .catch { e ->
            _errorMessage.value = e.localizedMessage ?: "Failed to load restock orders"
            emit(emptyList())
        }

    private val expensesFlow = repository.observeExpenses(currentUserId)
        .catch { e ->
            _errorMessage.value = e.localizedMessage ?: "Failed to load expenses"
            emit(emptyList())
        }

    val uiState: StateFlow<GroceryUiState> = combine(
        combine(productsFlow, ordersFlow, suppliersFlow) { p, o, s -> Triple(p, o, s) },
        combine(restockFlow, expensesFlow) { r, e -> Pair(r, e) },
        combine(_cartItems, _scannerMode, _scannedProductIdForUpdate) { c, m, sid -> Triple(c, m, sid) },
        combine(_lastScannedBanner, _snackbarMessage, _errorMessage) { b, sb, err -> Triple(b, sb, err) }
    ) { (products, orders, suppliers), (restockOrders, expenses), (cart, mode, scannedId), (banner, snack, error) ->
        val lowStock = products.filter { it.isLowStock() }
        val newAlerts = lowStock.filter { alertedLowStockIds.add(it.id) }
        if (newAlerts.isNotEmpty()) {
            _newlyTriggeredLowStock.value = newAlerts
        }
        // Remove products that were replenished above threshold so they can re-alert if depleted again
        val replenishedIds = products.filter { !it.isLowStock() }.map { it.id }.toSet()
        alertedLowStockIds.removeAll(replenishedIds)

        val topSellers = products
            .sortedWith(compareByDescending<Product> { it.totalSoldUnits }.thenByDescending { it.totalProfit })
            .take(8)

        val summary = computeProfitLossSummary(products, orders, expenses)

        // Keep cart product snapshots fresh
        val productMap = products.associateBy { it.id }
        val refreshedCart = cart.mapNotNull { item ->
            val latest = productMap[item.product.id] ?: item.product
            item.copy(product = latest)
        }

        GroceryUiState(
            isLoading = false,
            products = products,
            orders = orders,
            suppliers = suppliers,
            restockOrders = restockOrders,
            expenses = expenses,
            lowStockProducts = lowStock,
            topSellingProducts = topSellers,
            profitLossSummary = summary,
            cartItems = refreshedCart,
            scannerMode = mode,
            scannedProductForStockUpdate = scannedId?.let { productMap[it] },
            lastScannedBanner = banner,
            snackbarMessage = snack,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = GroceryUiState(isLoading = true)
    )

    fun clearNewlyTriggeredLowStock() {
        _newlyTriggeredLowStock.value = emptyList()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setScannerMode(mode: ScannerWorkflowMode) {
        _scannerMode.value = mode
    }

    fun selectProductForStockUpdate(product: Product?) {
        _scannedProductIdForUpdate.value = product?.id
    }

    // --- Barcode Lookup & Action ---

    fun onBarcodeScannedOrEntered(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return
        val currentProducts = uiState.value.products
        val matched = currentProducts.firstOrNull {
            it.barcode.equals(code, ignoreCase = true) ||
                it.name.equals(code, ignoreCase = true)
        }
        if (matched == null) {
            _snackbarMessage.value = "No product found with barcode \"$code\""
            return
        }

        when (_scannerMode.value) {
            ScannerWorkflowMode.POS_CHECKOUT -> {
                addToCart(matched)
                _lastScannedBanner.value = "Added ${matched.name} ($${String.format(Locale.US, "%.2f", matched.effectiveSellingPrice())})"
            }
            ScannerWorkflowMode.SECURE_INVENTORY_UPDATE -> {
                _scannedProductIdForUpdate.value = matched.id
                _lastScannedBanner.value = "Selected ${matched.name} (${matched.stockQuantity} ${matched.unit} in stock)"
            }
        }
    }

    // --- POS Cart Actions ---

    fun addToCart(product: Product, quantityDelta: Long = 1L) {
        if (product.stockQuantity <= 0L) {
            _snackbarMessage.value = "${product.name} is out of stock!"
            return
        }
        _cartItems.update { current ->
            val existingIndex = current.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val item = current[existingIndex]
                val nextQty = (item.quantity + quantityDelta).coerceAtMost(product.stockQuantity)
                current.toMutableList().apply {
                    this[existingIndex] = item.copy(product = product, quantity = nextQty)
                }
            } else {
                current + CartItem(product = product, quantity = quantityDelta.coerceAtMost(product.stockQuantity))
            }
        }
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Long) {
        _cartItems.update { current ->
            if (newQuantity <= 0L) {
                current.filterNot { it.product.id == productId }
            } else {
                current.map { item ->
                    if (item.product.id == productId) {
                        val capped = newQuantity.coerceAtMost(item.product.stockQuantity.coerceAtLeast(1L))
                        item.copy(quantity = capped)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun updateCartItemDiscount(productId: String, discountPercent: Double) {
        _cartItems.update { current ->
            current.map { item ->
                if (item.product.id == productId) {
                    item.copy(customDiscountPercent = discountPercent.coerceIn(0.0, 100.0))
                } else {
                    item
                }
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun completeCheckout(
        orderType: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String
    ) {
        val currentCart = uiState.value.cartItems
        if (currentCart.isEmpty()) {
            _snackbarMessage.value = "Cart is empty"
            return
        }
        viewModelScope.launch {
            val lineItems = currentCart.map { item ->
                OrderLineItem(
                    productId = item.product.id,
                    productName = item.product.name,
                    barcode = item.product.barcode,
                    unit = item.product.unit,
                    quantity = item.quantity,
                    unitCost = item.product.costPrice,
                    regularUnitPrice = item.product.sellingPrice,
                    unitPrice = item.effectiveUnitPrice,
                    discountPercent = item.activeDiscountPercent,
                    lineTotal = item.lineTotal,
                    lineProfit = item.lineProfit
                )
            }
            val subtotal = ((currentCart.sumOf { it.lineSubtotal }) * 100.0).roundToInt() / 100.0
            val discountTotal = ((currentCart.sumOf { it.lineDiscount }) * 100.0).roundToInt() / 100.0
            val totalAmount = ((currentCart.sumOf { it.lineTotal }) * 100.0).roundToInt() / 100.0
            val totalCost = ((currentCart.sumOf { it.lineCost }) * 100.0).roundToInt() / 100.0
            val netProfit = ((totalAmount - totalCost) * 100.0).roundToInt() / 100.0
            val totalUnits = currentCart.sumOf { it.quantity }
            val summary = currentCart.joinToString(", ") { "${it.quantity}x ${it.product.name}" }.take(950)
            val orderNum = "ORD-${(1000..9999).random()}"
            val initialStatus = if (orderType == "DELIVERY") "PENDING" else "COMPLETED"
            val epochDay = System.currentTimeMillis() / 86_400_000L

            val order = CustomerOrder(
                userId = currentUserId,
                orderNumber = orderNum,
                customerName = customerName.ifBlank {
                    if (orderType == "DELIVERY") "Delivery Customer" else "Walk-In Customer"
                },
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                orderType = orderType,
                status = initialStatus,
                paymentMethod = paymentMethod,
                itemsSummary = summary,
                items = lineItems.map { it.toMap() },
                totalItemsCount = totalUnits,
                subtotal = subtotal,
                discountTotal = discountTotal,
                totalAmount = totalAmount,
                totalCost = totalCost,
                netProfit = netProfit,
                orderDateEpochDay = epochDay
            )

            val result = repository.createCheckoutOrder(order, uiState.value.products)
            if (result.isSuccess) {
                _cartItems.value = emptyList()
                _snackbarMessage.value = "Order $orderNum completed! Net Profit: $${String.format(Locale.US, "%.2f", netProfit)}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Checkout failed"
            }
        }
    }

    // --- Product & Inventory Operations ---

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.upsertProduct(product)
            if (result.isSuccess) {
                _snackbarMessage.value = if (product.id.isBlank()) {
                    "Added ${product.name} to catalog"
                } else {
                    "Updated ${product.name}"
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save product"
            }
        }
    }

    fun quickUpdateStock(product: Product, newQuantity: Long, newDiscountPercent: Double? = null) {
        viewModelScope.launch {
            val result = repository.adjustProductStock(product.id, newQuantity, newDiscountPercent)
            if (result.isSuccess) {
                _snackbarMessage.value = "Stock for ${product.name} updated to $newQuantity ${product.unit}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Stock update failed"
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.deleteProduct(product.id)
            if (result.isSuccess) {
                _snackbarMessage.value = "Deleted ${product.name}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete product"
            }
        }
    }

    // --- Order Lifecycle Operations ---

    fun advanceOrderStatus(order: CustomerOrder, nextStatus: String) {
        viewModelScope.launch {
            val result = repository.updateOrderStatus(order, nextStatus, uiState.value.products)
            if (result.isSuccess) {
                _snackbarMessage.value = if (nextStatus == "CANCELLED") {
                    "Order ${order.orderNumber} cancelled and stock restored"
                } else {
                    "Order ${order.orderNumber} marked as $nextStatus"
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update order"
            }
        }
    }

    // --- Supplier & Restock Operations ---

    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val result = repository.upsertSupplier(supplier)
            if (result.isSuccess) {
                _snackbarMessage.value = "Saved supplier ${supplier.name}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save supplier"
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val result = repository.deleteSupplier(supplier.id)
            if (result.isSuccess) {
                _snackbarMessage.value = "Removed supplier ${supplier.name}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete supplier"
            }
        }
    }

    fun createOneTapRestockOrder(product: Product, customQuantity: Long = product.reorderQuantity) {
        viewModelScope.launch {
            val qty = customQuantity.coerceAtLeast(1L)
            val totalCost = ((product.costPrice * qty) * 100.0).roundToInt() / 100.0
            val supplierName = product.supplierName.ifBlank {
                uiState.value.suppliers.firstOrNull { it.category.equals(product.category, ignoreCase = true) }?.name
                    ?: "Primary Wholesale Supplier"
            }
            val supplierId = product.supplierId.ifBlank {
                uiState.value.suppliers.firstOrNull { it.name == supplierName }?.id ?: ""
            }
            val po = RestockOrder(
                userId = currentUserId,
                supplierId = supplierId,
                supplierName = supplierName,
                productId = product.id,
                productName = product.name,
                barcode = product.barcode,
                quantityOrdered = qty,
                unitCost = product.costPrice,
                totalCost = totalCost,
                status = "ORDERED"
            )
            val result = repository.createRestockOrder(po)
            if (result.isSuccess) {
                _snackbarMessage.value = "Restock order sent to $supplierName for $qty ${product.unit} of ${product.name}"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create restock order"
            }
        }
    }

    fun receiveRestockOrder(restockOrder: RestockOrder) {
        viewModelScope.launch {
            val matchingProduct = uiState.value.products.firstOrNull { it.id == restockOrder.productId }
            val result = repository.markRestockOrderReceived(restockOrder, matchingProduct)
            if (result.isSuccess) {
                _snackbarMessage.value = "Received +${restockOrder.quantityOrdered} units of ${restockOrder.productName} into stock!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to receive restock order"
            }
        }
    }

    fun cancelRestockOrder(restockOrder: RestockOrder) {
        viewModelScope.launch {
            val result = repository.cancelRestockOrder(restockOrder)
            if (result.isSuccess) {
                _snackbarMessage.value = "Restock order cancelled"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to cancel restock order"
            }
        }
    }

    // --- Operating Expenses ---

    fun addShopExpense(title: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val epochDay = System.currentTimeMillis() / 86_400_000L
            val expense = ShopExpense(
                userId = currentUserId,
                title = title,
                category = category,
                amount = amount,
                notes = notes,
                expenseDateEpochDay = epochDay
            )
            val result = repository.addExpense(expense)
            if (result.isSuccess) {
                _snackbarMessage.value = "Logged expense: $title ($${String.format(Locale.US, "%.2f", amount)})"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to log expense"
            }
        }
    }

    fun deleteShopExpense(expense: ShopExpense) {
        viewModelScope.launch {
            repository.deleteExpense(expense.id)
            _snackbarMessage.value = "Removed expense ${expense.title}"
        }
    }

    fun seedDemoStore() {
        viewModelScope.launch {
            val result = repository.seedSampleGroceryStore()
            if (result.isSuccess) {
                _snackbarMessage.value = "Sample grocery store catalog, orders & suppliers synced!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to load sample store"
            }
        }
    }

    private fun computeProfitLossSummary(
        products: List<Product>,
        orders: List<CustomerOrder>,
        expenses: List<ShopExpense>
    ): ProfitLossSummary {
        val activeOrders = orders.filter { it.status != "CANCELLED" }
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        val dateFormatter = SimpleDateFormat("MMM d", Locale.US)

        val totalRevenue = activeOrders.sumOf { it.totalAmount }
        val totalCogs = activeOrders.sumOf { it.totalCost }
        val totalDiscounts = activeOrders.sumOf { it.discountTotal }
        val grossProfit = totalRevenue - totalCogs
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfitOrLoss = grossProfit - totalExpenses

        val todayOrders = activeOrders.filter { it.orderDateEpochDay == todayEpochDay }
        val todayExpenses = expenses.filter { it.expenseDateEpochDay == todayEpochDay }.sumOf { it.amount }
        val todayRev = todayOrders.sumOf { it.totalAmount }
        val todayNet = todayOrders.sumOf { it.netProfit } - todayExpenses

        val inventoryCostVal = products.sumOf { it.costPrice * it.stockQuantity }
        val inventoryRetailVal = products.sumOf { it.effectiveSellingPrice() * it.stockQuantity }

        val dailyPoints = (6 downTo 0).map { offset ->
            val day = todayEpochDay - offset
            val dayOrders = activeOrders.filter { it.orderDateEpochDay == day }
            val dayExpenses = expenses.filter { it.expenseDateEpochDay == day }.sumOf { it.amount }
            val rev = dayOrders.sumOf { it.totalAmount }
            val cogs = dayOrders.sumOf { it.totalCost }
            val disc = dayOrders.sumOf { it.discountTotal }
            val gross = rev - cogs
            val net = gross - dayExpenses
            val label = if (offset == 0) {
                "Today"
            } else {
                dateFormatter.format(Date(day * 86_400_000L))
            }
            DailyProfitPoint(
                epochDay = day,
                label = label,
                revenue = rev,
                cogs = cogs,
                expenses = dayExpenses,
                discountsGiven = disc,
                grossProfit = gross,
                netProfit = net,
                ordersCount = dayOrders.size
            )
        }

        return ProfitLossSummary(
            totalRevenue = totalRevenue,
            totalCogs = totalCogs,
            totalDiscountsGiven = totalDiscounts,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfitOrLoss = netProfitOrLoss,
            todayRevenue = todayRev,
            todayNetProfit = todayNet,
            todayOrdersCount = todayOrders.size,
            inventoryCostValuation = inventoryCostVal,
            inventoryRetailValuation = inventoryRetailVal,
            dailySeries = dailyPoints
        )
    }
}
