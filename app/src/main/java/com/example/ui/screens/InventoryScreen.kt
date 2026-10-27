package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Product
import com.example.data.model.Supplier
import com.example.ui.theme.DiscountBadgeAmber
import com.example.ui.theme.LossCoralContainer
import com.example.ui.theme.LossCoralRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ProfitEmeraldContainer
import com.example.viewmodel.GroceryUiState

@Composable
fun InventoryScreen(
    uiState: GroceryUiState,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onQuickUpdateStock: (Product, Long, Double?) -> Unit,
    onAddToCart: (Product) -> Unit,
    onOneTapRestock: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Low Stock", "Discounted", "Produce", "Dairy", "Bakery", "Pantry", "Beverages")

    val filteredProducts = remember(uiState.products, searchQuery, selectedCategory) {
        uiState.products.filter { prod ->
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true) ||
                prod.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Low Stock" -> prod.isLowStock()
                "Discounted" -> prod.discountPercent > 0.0
                else -> prod.category.equals(selectedCategory, ignoreCase = true)
            }
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddProductDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add product") },
                text = { Text("Add Product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("add_product_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search product name, barcode or category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("inventory_search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("category_chip_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inventory_product_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductInventoryCard(
                        product = product,
                        onEdit = {
                            editingProduct = product
                            showAddProductDialog = true
                        },
                        onDelete = { onDeleteProduct(product) },
                        onAdjustStock = { newQty -> onQuickUpdateStock(product, newQty, null) },
                        onAddToCart = { onAddToCart(product) },
                        onOneTapRestock = { onOneTapRestock(product) }
                    )
                }
            }
        }
    }

    if (showAddProductDialog) {
        ProductEditorDialog(
            initialProduct = editingProduct,
            suppliers = uiState.suppliers,
            onDismiss = { showAddProductDialog = false },
            onSave = { saved ->
                onSaveProduct(saved)
                showAddProductDialog = false
            }
        )
    }
}

@Composable
fun ProductVisualThumbnail(
    product: Product,
    modifier: Modifier = Modifier
) {
    val customUri = product.imageUrl.takeIf {
        it.startsWith("content://") || it.startsWith("http://") || it.startsWith("https://") || it.startsWith("file://")
    }
    val fallbackDrawable = when (product.category.lowercase()) {
        "produce" -> R.drawable.ic_cat_produce
        "dairy" -> R.drawable.ic_cat_dairy
        "bakery" -> R.drawable.ic_cat_bakery
        "beverages" -> R.drawable.ic_cat_beverages
        else -> R.drawable.ic_cat_pantry
    }

    Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
        if (customUri != null) {
            AsyncImage(
                model = customUri,
                contentDescription = product.name,
                placeholder = painterResource(id = fallbackDrawable),
                error = painterResource(id = fallbackDrawable),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = fallbackDrawable),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (product.discountPercent > 0.0) {
            Surface(
                color = DiscountBadgeAmber,
                shape = RoundedCornerShape(bottomEnd = 10.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "-${product.discountPercent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductInventoryCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Long) -> Unit,
    onAddToCart: () -> Unit,
    onOneTapRestock: () -> Unit
) {
    val hasDiscount = product.discountPercent > 0.0
    val netPrice = product.effectiveSellingPrice()
    val unitProfit = product.unitProfit()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductVisualThumbnail(
                    product = product,
                    modifier = Modifier.size(78.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${product.category.uppercase()} • #${product.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = if (product.isLowStock()) LossCoralContainer else ProfitEmeraldContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (product.isLowStock()) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Low stock",
                                        tint = LossCoralRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = "${product.stockQuantity} ${product.unit} left",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (product.isLowStock()) LossCoralRed else ProfitEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Price with & without discount + Unit Profit
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${fmt(netPrice)}/${product.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasDiscount) {
                            Text(
                                text = "$${fmt(product.sellingPrice)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "• Cost $${fmt(product.costPrice)} • Profit +$${fmt(unitProfit)} (${product.profitMarginPercent()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unitProfit >= 0) ProfitEmerald else LossCoralRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAdjustStock((product.stockQuantity - 1L).coerceAtLeast(0L)) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("-1")
                    }
                    OutlinedButton(
                        onClick = { onAdjustStock(product.stockQuantity + 5L) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+5 Stock")
                    }
                    if (product.isLowStock()) {
                        TextButton(onClick = onOneTapRestock) {
                            Text("Restock PO", color = LossCoralRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddToCart) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add to POS cart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit product",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete product",
                            tint = LossCoralRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initialProduct: Product?,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "8901${(100000..999999).random()}") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Produce") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pc") }
    var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "") }
    var costPriceText by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "2.00") }
    var sellingPriceText by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "3.50") }
    var discountPercentText by remember { mutableStateOf(initialProduct?.discountPercent?.toString() ?: "0") }
    var stockQuantityText by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "25") }
    var reorderThresholdText by remember { mutableStateOf(initialProduct?.reorderThreshold?.toString() ?: "10") }
    var reorderQuantityText by remember { mutableStateOf(initialProduct?.reorderQuantity?.toString() ?: "30") }
    var supplierName by remember {
        mutableStateOf(initialProduct?.supplierName ?: suppliers.firstOrNull()?.name ?: "GreenValley Organic Farms")
    }

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    val categories = listOf("Produce", "Dairy", "Bakery", "Pantry", "Beverages")

    val costVal = costPriceText.toDoubleOrNull() ?: 0.0
    val sellVal = sellingPriceText.toDoubleOrNull() ?: 0.0
    val discVal = (discountPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val netPreview = sellVal * ((100.0 - discVal) / 100.0)
    val profitPreview = netPreview - costVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProduct == null) "Add Grocery Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / SKU") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("product_barcode_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg/pc)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Product Image URI / Preset") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick product photo"
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("Regular Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountPercentText,
                        onValueChange = { discountPercentText = it },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Live price preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Net Price: $${fmt(netPreview)} • Profit/Unit: ${if (profitPreview >= 0) "+" else ""}$${fmt(profitPreview)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantityText,
                        onValueChange = { stockQuantityText = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reorderThresholdText,
                        onValueChange = { reorderThresholdText = it },
                        label = { Text("Alert Threshold") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reorderQuantityText,
                        onValueChange = { reorderQuantityText = it },
                        label = { Text("Reorder Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && barcode.isNotBlank()) {
                        val base = initialProduct ?: Product()
                        onSave(
                            base.copy(
                                name = name,
                                barcode = barcode,
                                category = category,
                                unit = unit.ifBlank { "pc" },
                                imageUrl = imageUrl,
                                costPrice = costVal.coerceAtLeast(0.0),
                                sellingPrice = sellVal.coerceAtLeast(0.0),
                                discountPercent = discVal,
                                stockQuantity = (stockQuantityText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                                reorderThreshold = (reorderThresholdText.toLongOrNull() ?: 5L).coerceAtLeast(0L),
                                reorderQuantity = (reorderQuantityText.toLongOrNull() ?: 20L).coerceAtLeast(1L),
                                supplierName = supplierName
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_product_button")
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
