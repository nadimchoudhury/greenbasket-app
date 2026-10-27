package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyProfitPoint
import com.example.data.model.Product
import com.example.data.model.ShopExpense
import com.example.ui.theme.DiscountBadgeAmber
import com.example.ui.theme.LossCoralContainer
import com.example.ui.theme.LossCoralRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ProfitEmeraldContainer
import com.example.viewmodel.GroceryUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: GroceryUiState,
    onSeedSampleStore: () -> Unit,
    onOpenScannerCheckout: () -> Unit,
    onOpenSuppliersRestock: () -> Unit,
    onOneTapRestock: (Product) -> Unit,
    onAddExpense: (String, String, Double, String) -> Unit,
    onDeleteExpense: (ShopExpense) -> Unit,
    onSendTestLowStockNotification: () -> Unit
) {
    var showExpenseDialog by remember { mutableStateOf(false) }
    val pl = uiState.profitLossSummary
    val isNetProfit = pl.netProfitOrLoss >= 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner & Quick Store Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hero_store),
                        contentDescription = "Store analytics header",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DAILY PROFIT & STORE OVERVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Today: $${fmt(pl.todayRevenue)} Sales",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Today's Net Profit: ${if (pl.todayNetProfit >= 0) "+" else ""}$${fmt(pl.todayNetProfit)} (${pl.todayOrdersCount} orders)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenScannerCheckout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("hero_pos_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan & Checkout",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Barcode POS")
                            }
                            if (uiState.products.isEmpty()) {
                                OutlinedButton(
                                    onClick = onSeedSampleStore,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("seed_demo_store_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = "Load sample store",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Sample Store")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Low Inventory Alert Banner (with Push Alert trigger & One-Tap Restock)
        if (uiState.lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("low_stock_alert_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LossCoralContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Low inventory warning",
                                    tint = LossCoralRed,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${uiState.lowStockProducts.size} Products Below Reorder Threshold",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LossCoralRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Real-time low stock alert • Tap to send supplier restock PO",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LossCoralRed.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onSendTestLowStockNotification,
                                modifier = Modifier.testTag("push_low_stock_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Push low stock notification",
                                    tint = LossCoralRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.lowStockProducts.take(3).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF450A0A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Stock: ${item.stockQuantity} ${item.unit} (Threshold: ${item.reorderThreshold}) • ${item.supplierName.ifBlank { "Wholesale" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onOneTapRestock(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LossCoralRed,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("one_tap_restock_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Restock",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+${item.reorderQuantity} PO", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (uiState.lowStockProducts.size > 3) {
                            TextButton(
                                onClick = onOpenSuppliersRestock,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "View All ${uiState.lowStockProducts.size} Low-Stock Items →",
                                    color = LossCoralRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Comprehensive Profit & Loss (P&L) Statement Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profit_loss_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Profit & Loss Statement",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Cumulative revenue, COGS, discounts & shop expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (isNetProfit) ProfitEmeraldContainer else LossCoralContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isNetProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = if (isNetProfit) "Net Profit" else "Net Loss",
                                    tint = if (isNetProfit) ProfitEmerald else LossCoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isNetProfit) "NET PROFIT" else "NET LOSS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isNetProfit) ProfitEmerald else LossCoralRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${if (isNetProfit) "+" else "-"}$${fmt(kotlin.math.abs(pl.netProfitOrLoss))}",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (isNetProfit) ProfitEmerald else LossCoralRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PnlMetricTile(
                            label = "Gross Sales",
                            value = "$${fmt(pl.totalRevenue)}",
                            subtext = "Discounts: -$${fmt(pl.totalDiscountsGiven)}",
                            modifier = Modifier.weight(1f)
                        )
                        PnlMetricTile(
                            label = "Product Cost (COGS)",
                            value = "$${fmt(pl.totalCogs)}",
                            subtext = "Gross Margin: $${fmt(pl.grossProfit)}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PnlMetricTile(
                            label = "Shop Expenses",
                            value = "$${fmt(pl.totalExpenses)}",
                            subtext = "${uiState.expenses.size} logged items",
                            modifier = Modifier.weight(1f)
                        )
                        PnlMetricTile(
                            label = "Stock Valuation",
                            value = "$${fmt(pl.inventoryRetailValuation)}",
                            subtext = "Cost: $${fmt(pl.inventoryCostValuation)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Daily Profit Analysis (7-Day Interactive Bar Chart & Breakdown)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_profit_chart_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "7-Day Daily Profit Analysis",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Green = Sales Revenue • Amber/Coral = Net Profit/Loss",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DailyProfitBarChart(
                        points = pl.dailySeries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    pl.dailySeries.reversed().take(4).forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${day.label} (${day.ordersCount} orders)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Sales: $${fmt(day.revenue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Profit: ${if (day.netProfit >= 0) "+" else ""}$${fmt(day.netProfit)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (day.netProfit >= 0) ProfitEmerald else LossCoralRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Top-Selling Items Leaderboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_selling_items_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Top selling items",
                            tint = DiscountBadgeAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Top-Selling Items",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Ranked by units sold & cumulative net profit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.topSellingProducts.isEmpty()) {
                        Text(
                            text = "No sales recorded yet. Load the sample store or complete a POS checkout to see top-selling items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxUnits = (uiState.topSellingProducts.maxOfOrNull { it.totalSoldUnits } ?: 1L).coerceAtLeast(1L)
                        uiState.topSellingProducts.take(5).forEachIndexed { index, product ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (index == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${product.totalSoldUnits} ${product.unit} sold • Revenue $${fmt(product.totalRevenue)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "+$${fmt(product.totalProfit)} profit",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ProfitEmerald
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (product.totalSoldUnits.toFloat() / maxUnits.toFloat()).coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Operating Expenses Section (for accurate P&L calculation)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Operating expenses",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Shop Operating Expenses",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Deducted from gross profit in P&L",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showExpenseDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_expense_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Log Expense",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Expense")
                        }
                    }

                    if (uiState.expenses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.expenses.take(5).forEach { exp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exp.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${exp.category}${if (exp.notes.isNotBlank()) " • ${exp.notes}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "-$${fmt(exp.amount)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = LossCoralRed
                                    )
                                    IconButton(onClick = { onDeleteExpense(exp) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete expense",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { title, cat, amount, notes ->
                onAddExpense(title, cat, amount, notes)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
private fun PnlMetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyProfitBarChart(
    points: List<DailyProfitPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = DiscountBadgeAmber
    val lossColor = LossCoralRed
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (points.isEmpty()) return@Canvas
            val maxVal = points.maxOf { maxOf(it.revenue, kotlin.math.abs(it.netProfit), 10.0) }.toFloat()
            val slotWidth = size.width / points.size
            val barWidth = (slotWidth * 0.28f).coerceAtLeast(8f)

            points.forEachIndexed { idx, pt ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val revHeight = ((pt.revenue.toFloat() / maxVal) * (size.height * 0.88f)).coerceAtLeast(6f)
                val profitRatio = (kotlin.math.abs(pt.netProfit).toFloat() / maxVal).coerceIn(0.04f, 1f)
                val profitHeight = profitRatio * (size.height * 0.88f)

                // Background guide bar
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(centerX - barWidth - 3f, size.height * 0.08f),
                    size = Size(barWidth * 2 + 6f, size.height * 0.88f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Revenue bar (left)
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(centerX - barWidth - 2f, size.height * 0.96f - revHeight),
                    size = Size(barWidth, revHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Net Profit / Loss bar (right)
                drawRoundRect(
                    color = if (pt.netProfit >= 0) secondaryColor else lossColor,
                    topLeft = Offset(centerX + 2f, size.height * 0.96f - profitHeight),
                    size = Size(barWidth, profitHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val categories = listOf("Utilities", "Rent", "Wastage", "Packaging", "Logistics")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Shop Operating Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onSave(title, category, amt, notes)
                    }
                },
                modifier = Modifier.testTag("confirm_save_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)
