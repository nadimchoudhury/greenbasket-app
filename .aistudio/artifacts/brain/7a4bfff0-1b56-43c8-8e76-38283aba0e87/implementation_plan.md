# GreenBasket POS & Grocery Inventory Manager

GreenBasket is an all-in-one Android point-of-sale (POS), inventory control, and financial analytics application built for grocery shop owners. It synchronizes product stock, barcode checkouts, customer orders, supplier restocks, and daily profit-loss metrics in real time across multiple devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Product Decisions from Phase 1**
> - **Dual Checkout & Order Fulfillment**: Supports both fast in-store walk-in POS checkouts and customer pickup/delivery order management with status tracking.
> - **Dual-Mode Barcode Workflow**: Combines a live CameraX barcode scanner with an instant manual barcode/SKU keypad and searchable product picker—allowing seamless operation even on devices or emulators without physical barcode cameras.
> - **Per-Product Reorder Thresholds & One-Tap Restocking**: Each product defines its own custom low-stock alert threshold and linked supplier, enabling instant low-stock notifications and one-tap purchase order creation.

> [!NOTE]
> **Cloud Sync & Notification Strategy**
> - **Real-Time Multi-Device Sync**: Powered by **Firebase Firestore** and **Google Sign-In** (via Jetpack Credential Manager) so inventory updates, sales, and supplier restocks reflect live across all shop terminals.
> - **Low-Inventory Push Alerts**: Triggered immediately via Android System Notifications (`NotificationCompat` + `POST_NOTIFICATIONS` runtime permission) whenever real-time Firestore stock listeners detect an item falling at or below its custom reorder threshold. (If background Firebase Cloud Messaging server push is ever needed when all devices are offline, it can be enabled manually in the Firebase Console).

---

## 1. Overview & Core Concept

- **What It Does**: Provides grocery store owners and cashiers with a unified workspace to manage visual product catalogs (with cost price, regular price, and percentage/flat discounts), process barcode-assisted checkouts, track customer delivery orders, analyze daily profit and loss, identify top-selling items, and manage supplier restocking.
- **Target Audience / Persona**: Independent grocery shop owners, store managers, and counter cashiers who need fast checkout ergonomics alongside clear financial visibility and multi-device stock accuracy.
- **Key Value**: Eliminates stockouts and manual bookkeeping errors by linking every POS checkout, customer order, and supplier restock directly to real-time inventory quantities and automated profit-margin calculations.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Store Initialization**:
   - Users sign in via Google Sign-In (`CredentialManager`). New stores include an optional one-tap **"Load Sample Grocery Catalog"** action on empty state to immediately explore realistic produce, dairy, pantry items, suppliers, and sales history.
2. **Visual Inventory & Discount Management**:
   - Browse stock via visual product cards showing product imagery, category tags, current stock badge, cost price, selling price, active discount badge (e.g., `15% OFF` or net discounted price), profit margin per unit, and custom low-stock threshold.
   - Filter by category (*All, Produce, Dairy, Bakery, Pantry, Beverages, Low Stock*) or search by name/barcode.
3. **Barcode Scanner & Quick POS Checkout / Secure Restock**:
   - Open the **POS & Scanner** tab to toggle between **Checkout Mode** and **Quick Restock Mode**.
   - Scan via the live **CameraX viewfinder** or use the **Manual Barcode Entry & Quick Lookup** bar to add items to the cart or increment stock securely.
   - Apply item discounts, select Walk-In vs. Customer Delivery order, choose payment method (*Cash, Card, UPI/Wallet*), and complete checkout—automatically deducting stock and logging revenue, cost of goods sold (COGS), and net profit.
4. **Customer Orders Management**:
   - Track walk-in receipts and customer delivery/pickup orders across lifecycle stages (*Pending → Packed → Out for Delivery → Completed* or *Cancelled* with automatic stock restoration).
5. **Profit-Loss, Daily Profit Analysis & Top-Selling Items**:
   - View interactive financial cards and Compose Canvas charts displaying Gross Revenue, Total Cost (COGS), Discount Impact, Operating Expenses, and Net Profit/Loss.
   - Inspect a 7-day / 30-day Daily Profit & Loss trend bar chart and a ranked **Top-Selling Items** leaderboard by units sold and net profit generated.
6. **Supplier Dashboard & Low-Inventory Alert Center**:
   - Receive real-time Android push notifications and an in-app alert banner whenever an item's stock drops to or below its custom reorder threshold.
   - Manage supplier directory profiles (contact person, phone, lead time, supplied categories) and trigger **One-Tap Restock Orders** that automatically replenish product stock when marked *Received*.

### Visual Identity & Theme

- **Aesthetic Direction**: **Fresh Market Editorial & Precision Retail** — warm, tactile organic tones paired with high-contrast financial clarity for fast counter legibility.
- **Color Palette & Mood**:
  - **Primary (`#1B5E3A` Deep Botanical Green)**: Conveys freshness, growth, and profitable operations.
  - **Secondary (`#D97706` Warm Harvest Amber)**: Highlights discounts, promotional badges, and top-selling ranks.
  - **Tertiary (`#0284C7` Crisp Slate Blue)**: Used for supplier orders, delivery statuses, and analytics accents.
  - **Semantic Status Tokens**: Emerald Green (`#15803D`) for net profit, Coral Red (`#DC2626`) for net loss / critical stockouts, and Warm Cream (`#FAF8F5` light) / Deep Charcoal (`#111714` dark) surface hierarchy.
- **Typography & Hierarchy**:
  - **Display & Headlines (`Plus Jakarta Sans` via `font-util`)**: Bold, geometric numerals and headers for currency totals, profit KPIs, and product titles.
  - **Body & SKU Monospace (`Inter` & `JetBrains Mono` via `font-util`)**: Crisp legibility for dense order tables, barcode strings, and unit calculations.
- **Component Styling & Layout**:
  - Material Design 3 adaptive navigation with bottom `NavigationBar` (5 destinations: *Dashboard, Inventory, POS & Scan, Orders, Suppliers*) and `WindowInsets.navigationBars` safe-area padding.
  - Overlapping hero summary cards with subtle radial gradient backdrops and custom Compose `Canvas` charts for daily profit-loss bars and category sales breakdowns.
- **Interactive Feedback & Motion**:
  - Spring-physics cart badge animations, instant barcode scan confirmation haptics/snackbars, and smooth animated filter chips.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid CameraX + Manual Barcode Lookup Engine**
  - *Chosen Approach*: Provide a live CameraX viewfinder with barcode frame overlay alongside an integrated manual barcode keypad and instant SKU autocomplete selector.
  - *Why*: Guarantees 100% usability on both physical Android phones with cameras and browser-streamed emulators or tablets without camera hardware.
  - *Alternatives Considered*: Camera-only scanning was rejected because users without a physical camera barcode setup would be blocked from testing and daily manual operations.
- **Decision 2: Real-Time Multi-Device Sync with Atomic Firestore Transactions**
  - *Chosen Approach*: Use Firebase Firestore (`R.string.firestore_database_id`) with real-time `.snapshots()` flows and atomic batch/transaction writes when completing checkouts or receiving supplier restocks.
  - *Why*: Ensures that when a cashier completes an order on Device A, the stock count, low-stock alert state, and daily profit dashboard update simultaneously on the manager's Device B without race conditions.
  - *Alternatives Considered*: Device-local SQLite/Room was rejected because the user requires real-time synchronization across multiple devices.
- **Decision 3: Custom Per-Product Thresholds with Local Notification Dispatch**
  - *Chosen Approach*: Store `reorderThreshold` and `reorderQuantity` per product in Firestore; monitor live snapshot deltas to fire native Android high-priority notifications (`NotificationManagerCompat`) and populate a dedicated Low-Stock Restock queue.
  - *Why*: Gives immediate actionable alerts on every active device with a direct "Restock from Supplier" action button, without requiring external cloud function deployment.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge M3)                       │
│  ┌───────────────────────────┐       ┌───────────────────────────────────┐  │
│  │   AuthGate / SignInScreen │──────▶│    GreenBasketAppShell (Nav)      │  │
│  │ (Google CredentialManager)│       │  + LowStockNotificationObserver   │  │
│  └───────────────────────────┘       └─────────────────┬─────────────────┘  │
└────────────────────────────────────────────────────────┼────────────────────┘
                                                         │
       ┌──────────────────┬──────────────────┬───────────┴──────┬─────────────┐
       ▼                  ▼                  ▼                  ▼             ▼
┌─────────────┐   ┌──────────────┐   ┌──────────────┐   ┌────────────┐ ┌─────────────┐
│  Dashboard  │   │  Inventory   │   │ POS & Barcode│   │  Customer  │ │  Suppliers  │
│ & Analytics │   │   Catalog    │   │   Scanner    │   │   Orders   │ │ & Restock   │
│  - P&L Card │   │ - Stock Grid │   │ - CameraX UI │   │ - Walk-in  │ │ - Directory │
│  - Daily Bar│   │ - Discounts  │   │ - Manual Code│   │ - Delivery │ │ - 1-Tap PO  │
│  - Top Items│   │ - Add/Edit   │   │ - Cart/Stock │   │ - Statuses │ │ - Expenses  │
└──────┬──────┘   └──────┬───────┘   └──────┬───────┘   └─────┬──────┘ └──────┬──────┘
       │                 │                  │                 │               │
       └─────────────────┴──────────────────┼─────────────────┴───────────────┘
                                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     GroceryViewModel (StateFlow + UDF)                      │
│  - Computed Daily P&L, Discounted Price Math, Top Sellers & Alert Diffing   │
└───────────────────────────────────────────┬─────────────────────────────────┘
                                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│            GroceryRepository (FirebaseFirestore Custom Database ID)         │
│  users/{uid}/products | orders | suppliers | restockOrders | expenses       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

All Firestore documents are scoped under `/users/{userId}/...` (enforcing strict authenticated multi-device access for the store account) with `@DocumentId`, default property values, and `@ServerTimestamp`:

1. **`Product`**:
   - `id`, `userId`, `name`, `barcode`, `category`, `imageUrl`, `unit` (e.g., `kg`, `pc`, `pack`, `L`), `costPrice` (Double), `sellingPrice` (Double), `discountPercent` (Double, 0–100), `stockQuantity` (Long), `reorderThreshold` (Long), `reorderQuantity` (Long), `supplierId`, `supplierName`, `totalSoldUnits` (Long), `totalRevenue` (Double), `totalProfit` (Double), `createdAt`, `updatedAt`.
2. **`CustomerOrder`** (Walk-in POS & Customer Delivery Orders):
   - `id`, `userId`, `orderNumber`, `customerName`, `customerPhone`, `deliveryAddress`, `orderType` (`WALK_IN` | `DELIVERY`), `status` (`PENDING` | `PACKED` | `OUT_FOR_DELIVERY` | `COMPLETED` | `CANCELLED`), `paymentMethod`, `items` (list of line items with `productId`, `productName`, `barcode`, `quantity`, `unitCost`, `unitPrice`, `discountPercent`, `lineTotal`, `lineProfit`), `subtotal`, `discountTotal`, `totalAmount`, `totalCost`, `netProfit`, `orderDateEpochDay`, `createdAt`, `updatedAt`.
3. **`Supplier`**:
   - `id`, `userId`, `name`, `contactPerson`, `phone`, `email`, `category`, `address`, `notes`, `createdAt`, `updatedAt`.
4. **`RestockOrder`** (Supplier Purchase Orders):
   - `id`, `userId`, `supplierId`, `supplierName`, `productId`, `productName`, `barcode`, `quantityOrdered`, `unitCost`, `totalCost`, `status` (`ORDERED` | `RECEIVED` | `CANCELLED`), `createdAt`, `updatedAt`.
5. **`ShopExpense`** (For comprehensive Profit-Loss calculation):
   - `id`, `userId`, `title`, `category` (*Utilities, Rent, Wastage, Packaging, Logistics*), `amount`, `expenseDateEpochDay`, `createdAt`, `updatedAt`.

### Interactive Component & State Mapping

- **Product Stock & Discount Editor**: Validates non-negative cost/selling prices and `0..100` discount percentages; previews effective discounted selling price and unit profit margin live before saving to Firestore.
- **Dual Barcode Scanner & POS Cart**:
  - Requests `CAMERA` permission via `rememberLauncherForActivityResult` when opening the live CameraX scanner; always keeps the manual barcode text box and quick-chip selector active right below the scanner.
  - Entering or scanning a barcode in **Checkout Mode** increments the item in the active POS cart (checking available `stockQuantity`); in **Secure Inventory Update Mode**, it opens a quick quantity adjustment sheet (`+Restock`, `Set Exact Count`, or `Record Spoilage`).
- **Checkout & Order Lifecycle**:
  - Submitting a cart executes an atomic Firestore batch write that creates the `CustomerOrder` document and decrements `stockQuantity` (while incrementing `totalSoldUnits`, `totalRevenue`, and `totalProfit`) on each purchased `Product`.
  - Cancelling an unfulfilled order atomically restores the deducted `stockQuantity`.
- **Daily Profit & Loss + Sales Reporting**:
  - Dynamically aggregates completed orders and shop expenses by day (`orderDateEpochDay`), computing **Gross Sales**, **Total Discounts Given**, **COGS**, **Operating Expenses**, and **Net Profit / Loss**, rendered in an interactive daily bar chart and top-selling item ranking.
- **Low-Inventory Push Notification Engine**:
  - Requests `POST_NOTIFICATIONS` permission on Android 13+; observes real-time `Product` snapshots and dispatches a native notification whenever `stockQuantity <= reorderThreshold`, linking directly to one-tap supplier restock creation.
