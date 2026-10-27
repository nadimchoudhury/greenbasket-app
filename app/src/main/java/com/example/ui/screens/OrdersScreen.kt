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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.ui.theme.DiscountBadgeAmber
import com.example.ui.theme.LossCoralContainer
import com.example.ui.theme.LossCoralRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ProfitEmeraldContainer
import com.example.viewmodel.GroceryUiState

@Composable
fun OrdersScreen(
    uiState: GroceryUiState,
    onAdvanceOrderStatus: (CustomerOrder, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Delivery", "Walk-In", "Active", "Completed", "Cancelled")

    val filteredOrders = remember(uiState.orders, selectedFilter) {
        uiState.orders.filter { ord ->
            when (selectedFilter) {
                "All" -> true
                "Delivery" -> ord.orderType == "DELIVERY"
                "Walk-In" -> ord.orderType == "WALK_IN"
                "Active" -> ord.status in listOf("PENDING", "PACKED", "OUT_FOR_DELIVERY")
                "Completed" -> ord.status == "COMPLETED"
                "Cancelled" -> ord.status == "CANCELLED"
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    modifier = Modifier.testTag("orders_filter_${filter.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("customer_orders_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "No matching customer orders",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Create walk-in POS receipts or customer delivery orders from the POS & Scan tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredOrders, key = { it.id }) { order ->
                    CustomerOrderCard(
                        order = order,
                        onUpdateStatus = { nextStatus -> onAdvanceOrderStatus(order, nextStatus) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderCard(
    order: CustomerOrder,
    onUpdateStatus: (String) -> Unit
) {
    val isCancelled = order.status == "CANCELLED"
    val isCompleted = order.status == "COMPLETED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
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
                        imageVector = if (order.orderType == "DELIVERY") Icons.Default.DeliveryDining else Icons.Default.Storefront,
                        contentDescription = order.orderType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${order.orderNumber} • ${order.customerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.orderType} • Paid via ${order.paymentMethod}${if (order.customerPhone.isNotBlank()) " • ${order.customerPhone}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = when (order.status) {
                        "COMPLETED" -> ProfitEmeraldContainer
                        "CANCELLED" -> LossCoralContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = order.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (order.status) {
                            "COMPLETED" -> ProfitEmerald
                            "CANCELLED" -> LossCoralRed
                            else -> DiscountBadgeAmber
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (order.deliveryAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Delivery to: ${order.deliveryAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = order.itemsSummary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total: $${fmt(order.totalAmount)} (Saved $${fmt(order.discountTotal)})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isCancelled) "Stock Restored" else "Net Profit: +$${fmt(order.netProfit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCancelled) LossCoralRed else ProfitEmerald
                        )
                    }
                }
            }

            // Order Progression Actions (unless already terminal CANCELLED or COMPLETED)
            if (!isCancelled && !isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onUpdateStatus("CANCELLED") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("cancel_order_${order.id}")
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel Order", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel & Restore Stock")
                    }

                    val nextAction = when (order.status) {
                        "PENDING" -> "PACKED" to "Mark Packed"
                        "PACKED" -> "OUT_FOR_DELIVERY" to "Send Delivery"
                        else -> "COMPLETED" to "Mark Completed"
                    }

                    Button(
                        onClick = { onUpdateStatus(nextAction.first) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("advance_order_${order.id}")
                    ) {
                        Icon(
                            imageVector = if (nextAction.first == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Inventory2,
                            contentDescription = nextAction.second,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(nextAction.second)
                    }
                }
            }
        }
    }
}
