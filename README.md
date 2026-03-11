# 📚 YOUR CODE EXPLAINED — IntegratedChatbotApp.java

**Version:** v2.0 (Order History + Map)  
**Total Lines:** 2,546  
**Java Version Required:** 17+

---

## 🎯 WHAT THIS APP DOES

A **multi-window food ordering system** where:
- **1 Buyer** chats with multiple sellers
- **7 Sellers** (Padang, Korean, Fast Food, Healthy, Warteg, Dessert, Drinks)
- Real-time chat between buyer ↔ all sellers
- Order tracking with status updates
- Order history with delivery map visualization

---

## 🏗️ ARCHITECTURE OVERVIEW

```
Main App
├── BuyerChatWindow (1 window)
│   ├── Chat area (left)
│   └── Shopping cart (right)
│
└── SellerWindow (7 windows, auto-opened)
    ├── Orders panel (left)
    └── Chat panel (right)
```

**When you run it:**
```
java IntegratedChatbotApp

Opens automatically:
┌─────────────┐  ┌──────┬──────┐
│   Buyer     │  │ S1   │ S2   │
│   Window    │  ├──────┼──────┤
│             │  │ S3   │ S4   │
│ (left side) │  ├──────┼──────┤
│             │  │ S5   │ S6   │
└─────────────┘  └──────┴──────┘
                 │ S7   │      │
                 └──────┴──────┘
                 (right, tiled 2×4)
```

---

## 📦 COMPONENTS BREAKDOWN

### **1. Main Entry Point** (Lines 17-52)

```java
public class IntegratedChatbotApp {
    public static void main(String[] args) {
        // Creates ChatBridge (messaging hub)
        // Creates MultiStoreSystem (7 sellers)
        // Opens Buyer window (780×900, left side)
        // Opens 7 Seller windows (670×420 each, tiled right)
    }
}
```

**What happens:**
1. System Look & Feel applied
2. ChatBridge created (all messages go through here)
3. MultiStoreSystem initialized (loads all sellers + menus)
4. Buyer window opens at (0, 0)
5. Loop creates 7 seller windows in a 2-column grid

---

### **2. Enums** (Lines 58-92)

#### **FoodCategory** (7 categories)
```java
PADANG   → 🍛 Rumah Makan Padang
KOREAN   → 🍜 Korean Food
FASTFOOD → 🍗 Fast Food
HEALTHY  → 🥗 Healthy Food
WARTEG   → 🍚 Warteg / Local Food
DESSERT  → 🍰 Dessert
DRINKS   → 🥤 Drinks & Beverages
```

Each has: emoji + display name

#### **OrderStatus** (7 states)
```java
PENDING       → ⏳ Yellow
ACCEPTED      → ✅ Blue
ON_PROCESS    → 🔄 Purple
DRIVER_ON_WAY → 🚚 Cyan
COMPLETED     → ✔️ Green
BUSY          → ⏳ Orange
REJECTED      → ❌ Red
```

Each has: display name + color for UI

#### **MessageType** (5 types)
```java
TEXT                  → Regular chat message
STORE_RECOMMENDATION  → Item suggestions
SPECIAL_OFFER         → Promo cards
ORDER_UPDATE          → Status changes
SYSTEM                → System notifications
```

---

### **3. Data Models** (Lines 98-288)

#### **Seller** (Lines 98-143)
Represents a restaurant/store.

**Fields:**
```java
- id: "S001", "S002", etc.
- name: "Warung Padang Sederhana"
- category: FoodCategory.PADANG
- rating: 4.7 (out of 5)
- distanceKm: 0.3 (from customer)
- menu: List<MenuItem>
- promotions: List<SpecialOffer>
- currentQueueCount: 0 (active orders)
- isBusy: false
- window: SellerWindow reference
```

**Key Method:**
```java
getEstimatedWaitTime() {
    base = max cook time from menu (e.g., 40 min)
    queueDelay = currentQueueCount × 10 min
    driverDelay = 5 min
    return base + queueDelay + driverDelay
}
```

#### **MenuItem** (Lines 145-179)
Individual food item.

**Fields:**
```java
- id: "S001-1"
- name: "Nasi Rendang"
- price: 18000 (in Rupiah)
- rating: 4.8
- cookTimeMinutes: 40
- category: "food"
- tags: Set<String> {"spicy", "beef", "indonesian", ...}
```

**Tag Matching:**
Used for search. Example:
```java
Query: "spicy beef under 20k"
Tags: {"spicy", "beef", "indonesian"}
Match score: 20 (10 per matching tag)
```

#### **Order** (Lines 231-288)
Customer order.

**Fields:**
```java
- orderId: "ORD-1001" (auto-generated)
- customerName: "John Doe"
- phone: "08123456789"
- address: "Jababeka, Cikarang"
- notes: "Extra spicy please"
- items: List<CartItem>
- subtotal: 65000
- seller: Seller object
- status: OrderStatus.PENDING
- orderTime: LocalDateTime
- estimatedMinutes: 35
```

**Key Feature:**
```java
updateStatus(OrderStatus newStatus) {
    this.status = newStatus;
    if (newStatus == BUSY) estimatedMinutes += 20;
    if (newStatus == COMPLETED) {
        OrderHistoryManager.addCompletedOrder(this);  // Auto-save
    }
    notifyListeners(); // Update UI
}
```

---

### **4. Chat System** (Lines 295-360)

#### **ChatBridge** (Central Hub)
All messages flow through here.

**Methods:**
```java
sendFromBuyer(msg)              → Buyer sends message
sendFromSeller(sellerName, msg) → Seller replies
sendRecommendations(items, msg) → Show item cards
sendSpecialOffer(offer)         → Show promo card
sendOrderUpdate(order)          → Order status changed
sendSystem(msg)                 → System notification
```

**Listeners:**
All windows register as listeners to get real-time updates.

#### **ChatMessage**
```java
- senderName: "Customer" or "Seller Name"
- senderType: "BUYER" or "SELLER"
- message: "I want nasi goreng"
- type: MessageType.TEXT
- timestamp: 14:30
```

---

### **5. Shopping Cart** (Lines 362-414)

**Operations:**
```java
addItem(sellerItem, quantity)
removeItem(itemId, sellerId)
updateQty(itemId, sellerId, newQty)
getTotal()
getCount()
clear()
getPrimarySeller() → which seller has most items
```

**Smart Features:**
- Merges duplicate items (same item from same seller)
- Tracks which seller has most items
- Calculates total on-the-fly

---

### **6. MultiStoreSystem** (Lines 416-611)

**The Brain** — manages all sellers, menus, search.

#### **Initialization:**
```java
initializeStores() {
    Creates 7 sellers:
    S001: Warung Padang Sederhana (4.7★, 0.3km)
    S002: Korean Street Food (4.6★, 1.2km)
    S003: Burger & Pasta Station (4.4★, 0.9km)
    S004: Green Bowl & Salad (4.8★, 1.5km)
    S005: Warteg Bahagia (4.3★, 0.2km)
    S006: Sweet Dessert House (4.9★, 1.5km)
    S007: Warung Es Teh Indonesia (4.7★, 0.5km)
}

initializeMenus() {
    Each seller gets 4-5 menu items with:
    - Name, price, rating
    - Cook time
    - Search tags (spicy, sweet, cold, etc.)
}

initializeSynonyms() {
    Indonesian ↔ English:
    "manis" → "sweet"
    "pedas" → "spicy"
    "murah" → "cheap"
    etc.
}
```

#### **Search Engine:**
```java
search(query, maxPrice, byRating, bySpeed) {
    1. Parse query → extract tags
    2. Match tags with menu items
    3. Filter by price if specified
    4. Sort by rating or cook time if requested
    5. Return top 8 results
}
```

**Example:**
```
Query: "nasi goreng under 20k"
→ Tags: {rice, fried, nasi, goreng}
→ Price filter: ≤ 20,000
→ Results: All nasi goreng items under 20k, sorted by match score
```

---

### **7. Order History System** (Lines 613-641)

#### **OrderHistoryManager** (Static Singleton)
```java
- completedOrders: List<Order> (static)
- listeners: List<OrderHistoryListener>

addCompletedOrder(order) {
    completedOrders.add(order);
    notifyListeners(); // Update history window if open
}
```

**Auto-triggered when:**
```java
Order.updateStatus(COMPLETED) 
→ OrderHistoryManager.addCompletedOrder(this)
→ History window refreshes
```

---

### **8. Map System** (Lines 645-802)

#### **SimpleMapWindow**
Visual delivery route display.

**Shows:**
- Grid background (map)
- 🏪 Orange marker → Seller location (left)
- 🏠 Green marker → Customer address (right)
- Blue dashed line → Route
- Distance label on route
- Info panel: from/to/distance/phone

#### **MapCanvas** (Custom Graphics2D)
```java
paintComponent(Graphics g) {
    1. Draw grid background
    2. Calculate positions:
       - Seller at 1/4 from left
       - Customer offset by distance × 40px
    3. Draw route line (dashed blue)
    4. Draw markers (circles with emoji)
    5. Draw distance label
}
```

**Position Math:**
```
Seller X = width / 4
Customer X = sellerX + (distance_km × 40)
Customer Y = sellerY + random offset (±40)
```

---

### **9. SellerWindow** (Lines 808-1283)

Each seller gets their own window.

**Layout:**
```
┌────────────────────────────────────┐
│ 🍛 Warung Padang  ⭐4.7  📏0.3km  │
│ 🟢 Open  [⏳ Toggle Busy] [🎁]    │
├──────────────┬─────────────────────┤
│              │                     │
│ 📋 Orders    │  💬 Customer Chat   │
│              │                     │
│ [Order Card] │  [Buyer: Hi!]       │
│ [Order Card] │  [Seller: Hello!]   │
│              │                     │
├──────────────┴─────────────────────┤
│ Menu: [Item1] [Item2] [Item3]...   │
│ [Message input] [💬 Send]          │
└────────────────────────────────────┘
```

**Features:**
- **Left:** Order cards with status buttons
- **Right:** Live chat with buyer
- **Toggle Busy:** Adds 20 min to estimates
- **Send Promo:** Broadcasts special offers
- **Status Buttons:** ✅ Accept → 🔄 Process → 🚚 Driver → ✔️ Complete

**Order Card Buttons:**
```java
✅ Accept       → Blue
🔄 On Process   → Purple
🚚 Driver On Way → Cyan
✔️ Complete     → Green (saves to history)
⏳ Busy         → Orange (+20 min)
❌ Reject       → Red
🗺️ Map         → Gray (opens map window)
```

---

### **10. BuyerChatWindow** (Lines 1285-2388)

Main customer interface.

**Layout:**
```
┌─────────────────────────────────────────┐
│ 🍔 FoodChat  🛒 3 items  Rp 65,000      │
│ [📜 History] [🏪 Browse Sellers]        │
├─────────────────────────────────────────┤
│ SELLERS: [🟢🍛 Padang] [🟢🍜 Korean]... │
├────────────────────┬────────────────────┤
│                    │                    │
│   Chat Area        │   Shopping Cart    │
│   [Messages]       │   [Items]          │
│                    │   [Totals]         │
│                    │   [💳 Checkout]    │
│                    │                    │
├────────────────────┴────────────────────┤
│ [🍛 Padang] [🍜 Korean] [Quick filters] │
│ [Message input] [📤 Send]               │
└─────────────────────────────────────────┘
```

**Key Features:**

#### **Auto-Response System** (Lines 1329-1465)
```java
processQuery(query) {
    if (query.contains("hello"))    → greeting
    if (query.contains("help"))     → show menu tips
    if (query.contains("special"))  → show offers
    if (query.contains("open"))     → seller status
    if (query.contains("popular"))  → top-rated items
    
    // Smart search with filters:
    if (query.contains("under 20k")) → maxPrice = 20000
    if (query.contains("fastest"))   → sort by cook time
    if (query.contains("best"))      → sort by rating
    
    results = storeSystem.search(query, filters);
    → Send recommendation cards
}
```

**Example Flows:**

**Flow 1: Greeting**
```
User: "hi"
→ Timer 600ms
→ System: "Hello! 👋 Try: 'nasi goreng', 'korean food', 'special offer'"
```

**Flow 2: Search**
```
User: "nasi goreng under 20k"
→ Parse: tags={nasi, goreng, rice, fried}, maxPrice=20000
→ Search: 5 results found
→ Timer 800ms
→ System: "Here are options under Rp 20,000:"
→ Shows item cards with [+ Add to Cart] buttons
```

**Flow 3: Special Offer**
```
User: "special offer"
→ Timer 800ms
→ System: "🎁 Here are today's special offers:"
→ Shows combo deals (e.g., "Save 20%! Rp 10,000 off")
```

#### **Seller Status Bar** (Lines 1175-1210)
Pills showing all sellers with status:
```
🟢 🍛 Warung Padang  🟢 🍜 Korean  🔴 🍗 Burger (Busy)
```
- Click pill → focus that seller window
- Green = Open, Red = Busy
- Auto-updates when sellers toggle busy

#### **Checkout Flow** (Lines 2231-2388)
```
1. Click "💳 Checkout"
2. Dialog opens with form:
   - Name *
   - Phone *
   - Delivery Address *
   - Notes (optional)
   - Order Summary (seller, items, total, est. time)
3. Validate fields
4. Create order → send to seller
5. Seller window receives order
6. Clear cart
7. Chat notification: "🚀 Order ORD-1001 placed!"
```

---

### **11. OrderHistoryWindow** (Lines 2390-2546)

Shows all completed orders.

**Layout:**
```
┌────────────────────────────────────┐
│ 📜 Order History  (5 completed)    │
├────────────────────────────────────┤
│ ┌────────────────────────────────┐ │
│ │ ✔️ ORD-1001  15:30             │ │
│ │ 🏪 Korean Street Food          │ │
│ │ 👤 John Doe  📞 08123456789    │ │
│ │ 📍 Jababeka, Cikarang          │ │
│ │ 🍽️ Tteokbokki x2, Ramyeon x1  │ │
│ │ 💰 Rp 65,000   [🗺️ View Map]  │ │
│ └────────────────────────────────┘ │
│ ... (more orders)                  │
└────────────────────────────────────┘
```

**Features:**
- Newest orders first (reversed list)
- Each card has "🗺️ View on Map" button
- Auto-refreshes when new orders complete
- Empty state: "📭 No completed orders yet"

**Click Map Button → Opens SimpleMapWindow**

---

## 🔄 COMPLETE USER FLOW EXAMPLE

### **Scenario: Order Korean Food**

```
1. App launches
   → Buyer window + 7 seller windows open

2. Buyer types: "korean fried chicken"
   → processQuery analyzes
   → Searches menu with tags {korean, fried, chicken}
   → Finds: "Korean Fried Chicken - Rp 35,000 ⭐4.8"
   → Shows card with [+ Add to Cart] button

3. Buyer clicks "Add to Cart"
   → ShoppingCart.addItem(item, 1)
   → Cart updates: "🛒 1 item, Rp 35,000"

4. Buyer clicks "💳 Checkout"
   → Dialog opens
   → Fills: Name, Phone, Address
   → Clicks "🚀 Place Order"

5. Order created
   → Order(name, phone, addr, items, total, Korean seller)
   → Status: PENDING
   → Sent to Korean seller's window

6. Korean seller window receives order
   → Order card appears in left panel
   → Shows: customer info, items, total, status buttons

7. Seller clicks "✅ Accept"
   → Status → ACCEPTED (blue)
   → Chat notification to buyer
   → Estimated time shown

8. Seller clicks "🔄 On Process"
   → Status → ON_PROCESS (purple)
   → Buyer sees update

9. Seller clicks "🚚 Driver On Way"
   → Status → DRIVER_ON_WAY (cyan)

10. Seller clicks "✔️ Complete"
    → Status → COMPLETED (green)
    → Order auto-saved to OrderHistoryManager
    → History window refreshes (if open)

11. Buyer clicks "📜 History"
    → OrderHistoryWindow opens
    → Shows completed order
    → Click "🗺️ View on Map"
    → Map shows route from Korean seller to customer address
```

---

## 🎨 COLOR SCHEME

**Status Colors:**
```
Yellow  → Pending
Blue    → Accepted
Purple  → Processing
Cyan    → On the way
Green   → Completed
Orange  → Busy
Red     → Rejected
```

**UI Colors:**
```
Dark Header    → #1E1E2E (30,30,46)
Chat Buyer     → #2196F3 (Blue)
Chat Seller    → White
Cart Green     → #4CAF50 (76,175,80)
Map Seller     → #FF9800 (Orange)
Map Customer   → #4CAF50 (Green)
```

---

## 📊 DATA STRUCTURE

**In Memory:**
```
ChatBridge
├── messageHistory: List<ChatMessage>
└── listeners: List<ChatListener>

MultiStoreSystem
├── sellers: List<Seller> (7 sellers)
├── storeMenus: Map<sellerId, List<MenuItem>>
└── synonyms: Map<indonesian, english>

OrderHistoryManager (static)
└── completedOrders: List<Order>

BuyerChatWindow
└── cart: ShoppingCart
    └── items: List<CartItem>

SellerWindow (×7)
└── activeOrders: List<Order>
```

---

## 🧠 SMART FEATURES

### **1. Tag-Based Search**
```
Query: "spicy cheap korean"
→ Tags: {spicy, cheap, korean}
→ Matches all items with ANY of these tags
→ Sorts by number of matching tags
```

### **2. Synonym Translation**
```
"manis murah"
→ Translated: {sweet, cheap}
→ Matches English-tagged items
```

### **3. Price Parsing**
```
"under 20k"     → maxPrice = 20,000
"< 15k"         → maxPrice = 15,000
"max 25k"       → maxPrice = 25,000
"budget 10k"    → maxPrice = 10,000
```

### **4. Dynamic Time Estimation**
```
Base: 40 min (longest cook time in menu)
Queue: 2 orders × 10 min = 20 min
Driver: 5 min
Total: 65 minutes
```

### **5. Real-Time Sync**
All windows listen to ChatBridge:
```
Buyer sends message
→ ChatBridge.sendFromBuyer()
→ All 7 SellerWindows get onMessageReceived()
→ All show message in chat panel
```

---

## 🔧 TECHNICAL DETAILS

**Language:** Java 17+  
**GUI:** Swing (AWT + javax.swing)  
**Graphics:** Graphics2D (for map)  
**Concurrency:** Timer (for delayed responses)  
**Patterns:**
- Observer (ChatListener, OrderStatusListener)
- Singleton (OrderHistoryManager)
- MVC (data models separate from UI)

**Key Imports:**
```java
java.awt.*              → Graphics, Color, Layout
java.awt.event.*        → ActionListener
java.time.*             → LocalDateTime
java.util.*             → List, Map, Set
java.util.stream.*      → Lambda operations
javax.swing.*           → JFrame, JPanel, etc.
javax.swing.border.*    → Borders
```

---

## 📈 STATISTICS

**Lines of Code:** 2,546  
**Classes:** 17  
**Enums:** 3  
**Interfaces:** 2  
**Sellers:** 7  
**Menu Items:** ~35 total (5 per seller avg)  
**Windows:** 8 (1 buyer + 7 sellers)  
**Features:**
- ✅ Multi-window GUI
- ✅ Real-time chat
- ✅ Smart search engine
- ✅ Shopping cart
- ✅ Order tracking
- ✅ Order history
- ✅ Map visualization
- ✅ Status management

---

## 🚀 HOW TO RUN

```bash
# Compile
javac IntegratedChatbotApp.java

# Run
java IntegratedChatbotApp
```

**What happens:**
1. 8 windows open automatically
2. Buyer window on left (full height)
3. 7 seller windows tiled on right (2 columns)
4. Start chatting to test!

---

## 🎯 MISSING FEATURES (vs Latest Version)

Your code does NOT have:
- ❌ Payment methods (DANA, OVO, BCA, etc.)
- ❌ Agentic AI auto-responses
- ❌ AI toggle button

**To get the latest version with these features, use the file I just provided (2,719 lines).**

---

**That's your complete code explanation!** 🎉

Any specific part you want me to explain in more detail?
