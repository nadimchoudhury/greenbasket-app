package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.RestockOrder
import com.example.data.model.Supplier
import com.example.ui.theme.DiscountBadgeAmber
import com.example.ui.theme.LossCoralContainer
import com.example.ui.theme.LossCoralRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ProfitEmeraldContainer
import com.example.viewmodel.GroceryUiState

@Composable
fun SuppliersScreen(
    uiState: GroceryUiState,
    onSaveSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (Supplier) -> Unit,
    onOneTapRestock: (Product) -> Unit,
    onReceiveRestockOrder: (RestockOrder) -> Unit,
    onCancelRestockOrder: (RestockOrder) -> Unit
) {
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showSupplierDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingSupplier = null
                    showSupplierDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add supplier") },
                text = { Text("Add Supplier") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("add_supplier_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .testTag("suppliers_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. One-Tap Low-Inventory Restock Queue
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Low-Inventory Reorder Queue (${uiState.lowStockProducts.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Products at or below their custom reorder threshold • Tap to dispatch supplier PO",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.lowStockProducts.isEmpty()) {
                            Text(
                                text = "All products are healthy and stocked above their custom reorder thresholds.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfitEmerald
                            )
                        } else {
                            uiState.lowStockProducts.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Stock: ${prod.stockQuantity} / ${prod.reorderThreshold} ${prod.unit} • Supplier: ${prod.supplierName.ifBlank { "Default Wholesale" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LossCoralRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onOneTapRestock(prod) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("supplier_queue_restock_${prod.id}")
                                    ) {
                                        Text("Order +${prod.reorderQuantity}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Supplier Purchase Orders (Restock Orders)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Supplier Restock Orders (${uiState.restockOrders.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Mark incoming supplier shipments as Received to automatically replenish stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.restockOrders.isEmpty()) {
                            Text(
                                text = "No supplier purchase orders created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.restockOrders.forEach { po ->
                                RestockOrderRow(
                                    po = po,
                                    onReceive = { onReceiveRestockOrder(po) },
                                    onCancel = { onCancelRestockOrder(po) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // 3. Supplier Directory Header
            item {
                Text(
                    text = "Wholesale Suppliers Directory (${uiState.suppliers.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(uiState.suppliers, key = { it.id }) { supplier ->
                SupplierDirectoryCard(
                    supplier = supplier,
                    onEdit = {
                        editingSupplier = supplier
                        showSupplierDialog = true
                    },
                    onDelete = { onDeleteSupplier(supplier) }
                )
            }
        }
    }

    if (showSupplierDialog) {
        SupplierEditorDialog(
            initialSupplier = editingSupplier,
            onDismiss = { showSupplierDialog = false },
            onSave = { saved ->
                onSaveSupplier(saved)
                showSupplierDialog = false
            }
        )
    }
}

@Composable
private fun RestockOrderRow(
    po: RestockOrder,
    onReceive: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${po.productName} (+${po.quantityOrdered} units)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supplier: ${po.supplierName} • Total PO Cost: $${fmt(po.totalCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = when (po.status) {
                        "RECEIVED" -> ProfitEmeraldContainer
                        "CANCELLED" -> LossCoralContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = po.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (po.status) {
                            "RECEIVED" -> ProfitEmerald
                            "CANCELLED" -> LossCoralRed
                            else -> DiscountBadgeAmber
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (po.status == "ORDERED") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onReceive,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("receive_po_${po.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Receive stock", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Received (+Stock)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierDirectoryCard(
    supplier: Supplier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier_card_${supplier.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = supplier.name,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${supplier.category} • Lead time: ${supplier.leadTimeDays} day(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit supplier")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete supplier", tint = LossCoralRed)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${supplier.contactPerson} • ${supplier.phone}${if (supplier.email.isNotBlank()) " • ${supplier.email}" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (supplier.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supplier.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SupplierEditorDialog(
    initialSupplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (Supplier) -> Unit
) {
    var name by remember { mutableStateOf(initialSupplier?.name ?: "") }
    var contactPerson by remember { mutableStateOf(initialSupplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(initialSupplier?.phone ?: "") }
    var email by remember { mutableStateOf(initialSupplier?.email ?: "") }
    var category by remember { mutableStateOf(initialSupplier?.category ?: "Produce") }
    var leadTimeText by remember { mutableStateOf(initialSupplier?.leadTimeDays?.toString() ?: "2") }
    var notes by remember { mutableStateOf(initialSupplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSupplier == null) "Add Wholesale Supplier" else "Edit Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Company Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = leadTimeText,
                        onValueChange = { leadTimeText = it },
                        label = { Text("Lead Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Primary Category (Produce/Dairy/Pantry)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Delivery & Payment Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && contactPerson.isNotBlank() && phone.isNotBlank()) {
                        val base = initialSupplier ?: Supplier()
                        onSave(
                            base.copy(
                                name = name,
                                contactPerson = contactPerson,
                                phone = phone,
                                email = email,
                                category = category.ifBlank { "General" },
                                leadTimeDays = (leadTimeText.toLongOrNull() ?: 2L).coerceIn(0L, 90L),
                                notes = notes
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_supplier_button")
            ) {
                Text("Save Supplier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
