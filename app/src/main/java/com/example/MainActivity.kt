package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.GroceryRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PosScannerScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LowStockNotifier
import com.example.viewmodel.GroceryViewModel
import com.example.viewmodel.ScannerWorkflowMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LowStockNotifier.ensureNotificationChannel(this)
        setContent {
            MyApplicationTheme {
                GreenBasketRootApp()
            }
        }
    }
}

enum class GreenBasketTab(
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
) {
    DASHBOARD(R.string.nav_dashboard, Icons.Default.Analytics, "nav_tab_dashboard"),
    INVENTORY(R.string.nav_inventory, Icons.Default.Inventory2, "nav_tab_inventory"),
    POS_SCAN(R.string.nav_pos, Icons.Default.QrCodeScanner, "nav_tab_pos"),
    ORDERS(R.string.nav_orders, Icons.Default.ReceiptLong, "nav_tab_orders"),
    SUPPLIERS(R.string.nav_suppliers, Icons.Default.LocalShipping, "nav_tab_suppliers")
}

@Composable
fun GreenBasketRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        GreenBasketAuthenticatedShell(
            currentUserId = user.uid,
            userDisplayName = user.displayName ?: user.email ?: "Store Manager",
            onSignedOut = { currentUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenBasketAuthenticatedShell(
    currentUserId: String,
    userDisplayName: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(GreenBasketTab.DASHBOARD) }

    val viewModel: GroceryViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GroceryViewModel(GroceryRepository(db, Firebase.auth), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newlyTriggeredLowStock by viewModel.newlyTriggeredLowStock.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS permission on Android 13+ so low-stock push alerts appear
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Dispatch system push notification whenever products drop at or below reorder threshold
    LaunchedEffect(newlyTriggeredLowStock) {
        if (newlyTriggeredLowStock.isNotEmpty()) {
            LowStockNotifier.sendLowStockAlert(context, newlyTriggeredLowStock)
            viewModel.clearNewlyTriggeredLowStock()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val err = uiState.errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    if (currentTab != GreenBasketTab.DASHBOARD) {
        BackHandler {
            currentTab = GreenBasketTab.DASHBOARD
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "GreenBasket",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GreenBasket POS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOut(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignedOut,
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.sign_out)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                GreenBasketTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        GreenBasketTab.POS_SCAN -> uiState.cartItems.sumOf { it.quantity }.toInt()
                        GreenBasketTab.SUPPLIERS -> uiState.lowStockProducts.size
                        GreenBasketTab.ORDERS -> uiState.orders.count { it.status in listOf("PENDING", "PACKED", "OUT_FOR_DELIVERY") }
                        else -> 0
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge { Text("$badgeCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(id = tab.labelRes)
                                )
                            }
                        },
                        label = { Text(stringResource(id = tab.labelRes)) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (currentTab) {
                GreenBasketTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    onSeedSampleStore = { viewModel.seedDemoStore() },
                    onOpenScannerCheckout = {
                        viewModel.setScannerMode(ScannerWorkflowMode.POS_CHECKOUT)
                        currentTab = GreenBasketTab.POS_SCAN
                    },
                    onOpenSuppliersRestock = { currentTab = GreenBasketTab.SUPPLIERS },
                    onOneTapRestock = { prod -> viewModel.createOneTapRestockOrder(prod) },
                    onAddExpense = { title, cat, amt, notes -> viewModel.addShopExpense(title, cat, amt, notes) },
                    onDeleteExpense = { exp -> viewModel.deleteShopExpense(exp) },
                    onSendTestLowStockNotification = {
                        LowStockNotifier.sendLowStockAlert(context, uiState.lowStockProducts)
                    }
                )
                GreenBasketTab.INVENTORY -> InventoryScreen(
                    uiState = uiState,
                    onSaveProduct = { prod -> viewModel.saveProduct(prod) },
                    onDeleteProduct = { prod -> viewModel.deleteProduct(prod) },
                    onQuickUpdateStock = { prod, qty, disc -> viewModel.quickUpdateStock(prod, qty, disc) },
                    onAddToCart = { prod ->
                        viewModel.addToCart(prod)
                    },
                    onOneTapRestock = { prod -> viewModel.createOneTapRestockOrder(prod) }
                )
                GreenBasketTab.POS_SCAN -> PosScannerScreen(
                    uiState = uiState,
                    onSetScannerMode = { mode -> viewModel.setScannerMode(mode) },
                    onBarcodeEntered = { code -> viewModel.onBarcodeScannedOrEntered(code) },
                    onUpdateCartQuantity = { prodId, qty -> viewModel.updateCartItemQuantity(prodId, qty) },
                    onUpdateCartDiscount = { prodId, disc -> viewModel.updateCartItemDiscount(prodId, disc) },
                    onClearCart = { viewModel.clearCart() },
                    onCompleteCheckout = { type, name, phone, addr, payment ->
                        viewModel.completeCheckout(type, name, phone, addr, payment)
                    },
                    onQuickUpdateStock = { prod, qty, disc -> viewModel.quickUpdateStock(prod, qty, disc) }
                )
                GreenBasketTab.ORDERS -> OrdersScreen(
                    uiState = uiState,
                    onAdvanceOrderStatus = { ord, status -> viewModel.advanceOrderStatus(ord, status) }
                )
                GreenBasketTab.SUPPLIERS -> SuppliersScreen(
                    uiState = uiState,
                    onSaveSupplier = { sup -> viewModel.saveSupplier(sup) },
                    onDeleteSupplier = { sup -> viewModel.deleteSupplier(sup) },
                    onOneTapRestock = { prod -> viewModel.createOneTapRestockOrder(prod) },
                    onReceiveRestockOrder = { po -> viewModel.receiveRestockOrder(po) },
                    onCancelRestockOrder = { po -> viewModel.cancelRestockOrder(po) }
                )
            }
        }
    }
}
