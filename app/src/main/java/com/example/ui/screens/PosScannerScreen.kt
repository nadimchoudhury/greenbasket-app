package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.theme.DiscountBadgeAmber
import com.example.ui.theme.LossCoralRed
import com.example.ui.theme.ProfitEmerald
import com.example.viewmodel.GroceryUiState
import com.example.viewmodel.ScannerWorkflowMode

@Composable
fun PosScannerScreen(
    uiState: GroceryUiState,
    onSetScannerMode: (ScannerWorkflowMode) -> Unit,
    onBarcodeEntered: (String) -> Unit,
    onUpdateCartQuantity: (String, Long) -> Unit,
    onUpdateCartDiscount: (String, Double) -> Unit,
    onClearCart: () -> Unit,
    onCompleteCheckout: (String, String, String, String, String) -> Unit,
    onQuickUpdateStock: (Product, Long, Double?) -> Unit
) {
    val context = LocalContext.current
    var manualBarcode by remember { mutableStateOf("") }
    var showCameraPreview by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        showCameraPreview = granted
    }

    // Checkout form state
    var orderType by remember { mutableStateOf("WALK_IN") } // WALK_IN | DELIVERY
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH | CARD | UPI

    // Secure Inventory Update form state
    val selectedStockProduct = uiState.scannedProductForStockUpdate ?: uiState.products.firstOrNull()
    var exactStockText by remember(selectedStockProduct?.id, selectedStockProduct?.stockQuantity) {
        mutableStateOf(selectedStockProduct?.stockQuantity?.toString() ?: "0")
    }
    var discountOverrideText by remember(selectedStockProduct?.id, selectedStockProduct?.discountPercent) {
        mutableStateOf(selectedStockProduct?.discountPercent?.toString() ?: "0")
    }

    val cartSubtotal = uiState.cartItems.sumOf { it.lineSubtotal }
    val cartDiscount = uiState.cartItems.sumOf { it.lineDiscount }
    val cartTotal = uiState.cartItems.sumOf { it.lineTotal }
    val cartProfit = uiState.cartItems.sumOf { it.lineProfit }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pos_scanner_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Mode Switcher: POS Checkout vs Secure Inventory Update
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSetScannerMode(ScannerWorkflowMode.POS_CHECKOUT) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.scannerMode == ScannerWorkflowMode.POS_CHECKOUT) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (uiState.scannerMode == ScannerWorkflowMode.POS_CHECKOUT) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_pos_checkout_button")
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = "POS Checkout", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("POS Checkout")
                        }

                        Button(
                            onClick = { onSetScannerMode(ScannerWorkflowMode.SECURE_INVENTORY_UPDATE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.scannerMode == ScannerWorkflowMode.SECURE_INVENTORY_UPDATE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (uiState.scannerMode == ScannerWorkflowMode.SECURE_INVENTORY_UPDATE) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_inventory_update_button")
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = "Stock Update", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stock Update")
                        }
                    }
                }
            }
        }

        // 2. Hybrid CameraX Barcode Scanner + Manual Barcode Entry Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Barcode scanner",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Barcode Scanner & Manual Entry",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Use live camera or manual code / quick-tap SKU below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (showCameraPreview) {
                                    showCameraPreview = false
                                } else if (hasCameraPermission) {
                                    showCameraPreview = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_camera_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Toggle camera scanner",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showCameraPreview) "Hide Camera" else "Live Camera")
                        }
                    }

                    if (showCameraPreview && hasCameraPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CameraBarcodePreviewBox(
                            products = uiState.products,
                            onSimulateScanProduct = { prod -> onBarcodeEntered(prod.barcode) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual Barcode / SKU Keypad Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualBarcode,
                            onValueChange = { manualBarcode = it },
                            label = { Text("Enter Barcode / SKU or Product Name") },
                            leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = "Manual code") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )
                        Button(
                            onClick = {
                                onBarcodeEntered(manualBarcode)
                                manualBarcode = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("submit_manual_barcode_button")
                        ) {
                            Text(if (uiState.scannerMode == ScannerWorkflowMode.POS_CHECKOUT) "Add" else "Lookup")
                        }
                    }

                    if (uiState.lastScannedBanner != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Scanned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.lastScannedBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Quick-Tap Barcode Chips (For rapid checkout without camera hardware)
                    if (uiState.products.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Quick Barcode Tap (No Camera Needed):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.products, key = { it.id }) { prod ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onBarcodeEntered(prod.barcode) },
                                    label = {
                                        Text(
                                            text = "${prod.name.take(18)} (#${prod.barcode}) • $${fmt(prod.effectiveSellingPrice())}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_scan_chip_${prod.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3A. POS Checkout Cart & Order Completion
        if (uiState.scannerMode == ScannerWorkflowMode.POS_CHECKOUT) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Checkout Cart (${uiState.cartItems.sumOf { it.quantity }} items)",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (uiState.cartItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearCart,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.cartItems.isEmpty()) {
                            Text(
                                text = "Scan a barcode, enter a code manually, or tap any quick product chip above to add items to the checkout cart.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.cartItems.forEach { cartItem ->
                                PosCartLineRow(
                                    item = cartItem,
                                    onQuantityChange = { nextQty ->
                                        onUpdateCartQuantity(cartItem.product.id, nextQty)
                                    },
                                    onDiscountChange = { disc ->
                                        onUpdateCartDiscount(cartItem.product.id, disc)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fulfillment Type: Walk-In POS vs Customer Delivery
                            Text(
                                text = "Order Fulfillment Type",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = orderType == "WALK_IN",
                                    onClick = { orderType = "WALK_IN" },
                                    label = { Text("In-Store Walk-In POS") }
                                )
                                FilterChip(
                                    selected = orderType == "DELIVERY",
                                    onClick = { orderType = "DELIVERY" },
                                    label = { Text("Customer Delivery Order") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text(if (orderType == "DELIVERY") "Customer Name *" else "Customer Name (Opt)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("Phone") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (orderType == "DELIVERY") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = deliveryAddress,
                                    onValueChange = { deliveryAddress = it },
                                    label = { Text("Delivery Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Payment Method",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CASH", "CARD", "UPI").forEach { method ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { paymentMethod = method },
                                        label = { Text(method) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Gross Subtotal:", style = MaterialTheme.typography.bodyMedium)
                                        Text("$${fmt(cartSubtotal)}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Discount Saved:", style = MaterialTheme.typography.bodyMedium)
                                        Text("-$${fmt(cartDiscount)}", style = MaterialTheme.typography.bodyMedium, color = DiscountBadgeAmber)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Estimated Order Net Profit:", style = MaterialTheme.typography.bodySmall)
                                        Text("+$${fmt(cartProfit)}", style = MaterialTheme.typography.labelMedium, color = ProfitEmerald)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Net Payable Total:", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "$${fmt(cartTotal)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onCompleteCheckout(
                                        orderType,
                                        customerName,
                                        customerPhone,
                                        deliveryAddress,
                                        paymentMethod
                                    )
                                    customerName = ""
                                    customerPhone = ""
                                    deliveryAddress = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("complete_checkout_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = "Complete checkout")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Checkout ($${fmt(cartTotal)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 3B. Secure Inventory Update Mode
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Secure Inventory & Discount Update",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Scan or tap any product barcode above to verify & update physical stock counts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (selectedStockProduct == null) {
                            Text("No product available. Add a product or load the sample catalog first.")
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${selectedStockProduct.name} (#${selectedStockProduct.barcode})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Current Stock: ${selectedStockProduct.stockQuantity} ${selectedStockProduct.unit} • Reorder Alert Threshold: ${selectedStockProduct.reorderThreshold} ${selectedStockProduct.unit}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Regular: $${fmt(selectedStockProduct.sellingPrice)} • Discount: ${selectedStockProduct.discountPercent.toInt()}% • Net: $${fmt(selectedStockProduct.effectiveSellingPrice())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val updated = selectedStockProduct.stockQuantity + 10L
                                        exactStockText = updated.toString()
                                        onQuickUpdateStock(selectedStockProduct, updated, null)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+10 Restock")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val updated = selectedStockProduct.stockQuantity + 25L
                                        exactStockText = updated.toString()
                                        onQuickUpdateStock(selectedStockProduct, updated, null)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+25 Restock")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val updated = (selectedStockProduct.stockQuantity - 1L).coerceAtLeast(0L)
                                        exactStockText = updated.toString()
                                        onQuickUpdateStock(selectedStockProduct, updated, null)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("-1 Spoilage")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = exactStockText,
                                    onValueChange = { exactStockText = it },
                                    label = { Text("Exact Stock Count") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = discountOverrideText,
                                    onValueChange = { discountOverrideText = it },
                                    label = { Text("Discount (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val newQty = (exactStockText.toLongOrNull() ?: selectedStockProduct.stockQuantity).coerceAtLeast(0L)
                                    val newDisc = (discountOverrideText.toDoubleOrNull() ?: selectedStockProduct.discountPercent).coerceIn(0.0, 100.0)
                                    onQuickUpdateStock(selectedStockProduct, newQty, newDisc)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("commit_secure_stock_update_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Save Verified Inventory Update")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraBarcodePreviewBox(
    products: List<Product>,
    onSimulateScanProduct: (Product) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1510))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (ignored: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Reticle overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 210.dp, height = 90.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFF4ADE80))
            ) {}
        }

        if (products.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSimulateScanProduct(products.random()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Capture barcode", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capture Barcode in Frame")
            }
        }
    }
}

@Composable
private fun PosCartLineRow(
    item: CartItem,
    onQuantityChange: (Long) -> Unit,
    onDiscountChange: (Double) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${fmt(item.effectiveUnitPrice)}/${item.product.unit} (Disc: ${item.activeDiscountPercent.toInt()}%) • Line: $${fmt(item.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(0.0, 5.0, 10.0, 20.0).forEach { disc ->
                        Surface(
                            color = if (item.activeDiscountPercent.toInt() == disc.toInt()) {
                                DiscountBadgeAmber
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(24.dp),
                            onClick = { onDiscountChange(disc) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "${disc.toInt()}% off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.activeDiscountPercent.toInt() == disc.toInt()) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantityChange(item.quantity - 1L) }) {
                    Icon(
                        imageVector = if (item.quantity <= 1L) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "Decrease quantity",
                        tint = if (item.quantity <= 1L) LossCoralRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onQuantityChange(item.quantity + 1L) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase quantity"
                    )
                }
            }
        }
    }
}
