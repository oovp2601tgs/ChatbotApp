import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;

// ===============================
// MAIN ENTRY POINT
// ===============================

public class IntegratedChatbotApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ChatBridge chatBridge = new ChatBridge();
            MultiStoreSystem storeSystem = new MultiStoreSystem();

            // Open buyer window centered
            BuyerChatWindow buyer = new BuyerChatWindow(chatBridge, storeSystem);
            buyer.setSize(1000, 850);
            buyer.setLocationRelativeTo(null);
            buyer.setVisible(true);
        });
    }
}

// ===============================
// ENUMS
// ===============================

enum FoodCategory {
    PADANG("🍛", "Rumah Makan Padang"),
    KOREAN("🍜", "Korean Food"),
    FASTFOOD("🍗", "Fast Food"),
    HEALTHY("🥗", "Healthy Food"),
    WARTEG("🍚", "Warteg / Local Food"),
    DESSERT("🍰", "Dessert"),
    DRINKS("🥤", "Drinks & Beverages");

    public final String emoji;
    public final String displayName;
    FoodCategory(String emoji, String displayName) {
        this.emoji = emoji;
        this.displayName = displayName;
    }
}

enum OrderStatus {
    PENDING("⏳ Pending", new Color(255, 193, 7)),
    ACCEPTED("✅ Accepted", new Color(33, 150, 243)),
    ON_PROCESS("🔄 On Process", new Color(156, 39, 176)),
    DRIVER_ON_WAY("🚚 Driver On Way", new Color(0, 188, 212)),
    COMPLETED("✔️ Completed", new Color(76, 175, 80)),
    BUSY("⏳ Busy", new Color(255, 87, 34)),
    REJECTED("❌ Rejected", new Color(244, 67, 54));

    public final String displayName;
    public final Color color;
    OrderStatus(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }
}

enum MessageType { TEXT, STORE_RECOMMENDATION, SPECIAL_OFFER, ORDER_UPDATE, SYSTEM }

// ===============================
// DATA MODELS
// ===============================

class Seller {
    private final String id;
    private final String name;
    private final FoodCategory category;
    private final double rating;
    private final double distanceKm;
    private final double lat;
    private final double lng;
    private int currentQueueCount;
    private boolean isBusy;
    private List<MenuItem> menu;
    private List<SpecialOffer> promotions;
    private SellerWindow window;

    public Seller(String id, String name, FoodCategory category, double rating, double distanceKm,
                   double lat, double lng) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.distanceKm = distanceKm;
        this.lat = lat;
        this.lng = lng;
        this.menu = new ArrayList<>();
        this.promotions = new ArrayList<>();
        this.currentQueueCount = 0;
        this.isBusy = false;
    }

    public int getEstimatedWaitTime() {
        int base = menu.stream().mapToInt(MenuItem::getCookTimeMinutes).max().orElse(20);
        int queueDelay = currentQueueCount * 10;
        int driverDelay = 5;
        return base + queueDelay + driverDelay;
    }

    // Getters & setters
    public String getId() { return id; }
    public String getName() { return name; }
    public FoodCategory getCategory() { return category; }
    public double getRating() { return rating; }
    public double getDistanceKm() { return distanceKm; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public List<MenuItem> getMenu() { return menu; }
    public List<SpecialOffer> getPromotions() { return promotions; }
    public int getCurrentQueueCount() { return currentQueueCount; }
    public boolean isBusy() { return isBusy; }
    public void setCurrentQueueCount(int count) { currentQueueCount = count; }
    public void setBusy(boolean busy) { isBusy = busy; }
    public SellerWindow getWindow() { return window; }
    public void setWindow(SellerWindow w) { window = w; }
    public String getCategoryDisplay() { return category.emoji + " " + category.displayName; }
}

class MenuItem {
    private final String id;
    private final String name;
    private final int price;
    private final double rating;
    private final int cookTimeMinutes;
    private final Set<String> tags;
    private final String category;

    public MenuItem(String id, String name, int price, double rating, int cookTimeMinutes,
                    String category, String... tags) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.rating = rating;
        this.cookTimeMinutes = cookTimeMinutes;
        this.category = category;
        this.tags = new HashSet<>(Arrays.asList(tags));
    }

    public int getMatchScore(Set<String> queryTags) {
        return (int) queryTags.stream().filter(tags::contains).count() * 10;
    }
    public boolean hasAnyTag(Set<String> queryTags) {
        return queryTags.stream().anyMatch(tags::contains);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public double getRating() { return rating; }
    public int getCookTimeMinutes() { return cookTimeMinutes; }
    public String getCategory() { return category; }
    public Set<String> getTags() { return tags; }
}

class SellerItem {
    public final Seller seller;
    public final MenuItem item;
    public SellerItem(Seller seller, MenuItem item) {
        this.seller = seller;
        this.item = item;
    }
}

class CartItem {
    private SellerItem sellerItem;
    private int quantity;
    public CartItem(SellerItem si, int qty) {
        this.sellerItem = si;
        this.quantity = qty;
    }
    public SellerItem getSellerItem() { return sellerItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { quantity = q; }
    public int getTotal() { return sellerItem.item.getPrice() * quantity; }
}

class SpecialOffer {
    private final String title;
    private final String description;
    private final List<SellerItem> items;
    private final int discountPercent;
    private final int originalPrice;
    private final int offerPrice;

    public SpecialOffer(String title, String description, List<SellerItem> items, int discountPercent) {
        this.title = title;
        this.description = description;
        this.items = items;
        this.discountPercent = discountPercent;
        this.originalPrice = items.stream().mapToInt(si -> si.item.getPrice()).sum();
        this.offerPrice = (int)(originalPrice * (1 - discountPercent / 100.0));
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<SellerItem> getItems() { return items; }
    public int getDiscountPercent() { return discountPercent; }
    public int getOriginalPrice() { return originalPrice; }
    public int getOfferPrice() { return offerPrice; }
    public int getSavings() { return originalPrice - offerPrice; }
}

// Class to represent items grouped by seller for multi-seller orders
class SellerOrderGroup {
    private final Seller seller;
    private final List<CartItem> items;
    private int subtotal;
    
    public SellerOrderGroup(Seller seller, List<CartItem> items) {
        this.seller = seller;
        this.items = new ArrayList<>(items);
        this.subtotal = items.stream().mapToInt(CartItem::getTotal).sum();
    }
    
    public Seller getSeller() { return seller; }
    public List<CartItem> getItems() { return items; }
    public int getSubtotal() { return subtotal; }
}

class Order {
    private static int counter = 1000;
    private final String orderId;
    private final String customerName;
    private final String phone;
    private final String address;
    private final String notes;
    private final List<CartItem> items;
    private final int subtotal;
    private final Seller seller;
    private OrderStatus status;
    private final LocalDateTime orderTime;
    private int estimatedMinutes;
    private List<OrderStatusListener> listeners = new ArrayList<>();

    public Order(String name, String phone, String address, String notes,
                 List<CartItem> items, int subtotal, Seller seller) {
        this.orderId = "ORD-" + (++counter);
        this.customerName = name;
        this.phone = phone;
        this.address = address;
        this.notes = notes;
        this.items = new ArrayList<>(items);
        this.subtotal = subtotal;
        this.seller = seller;
        this.status = OrderStatus.PENDING;
        this.orderTime = LocalDateTime.now();
        this.estimatedMinutes = seller.getEstimatedWaitTime();
    }

    public void addStatusListener(OrderStatusListener l) { listeners.add(l); }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        if (newStatus == OrderStatus.BUSY) estimatedMinutes += 20;
        
        if (newStatus == OrderStatus.COMPLETED) {
            OrderHistoryManager.addCompletedOrder(this);
        }
        
        listeners.forEach(l -> l.onStatusChanged(this));
    }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getNotes() { return notes; }
    public List<CartItem> getItems() { return items; }
    public int getSubtotal() { return subtotal; }
    public Seller getSeller() { return seller; }
    public OrderStatus getStatus() { return status; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public String getFormattedTime() {
        return orderTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

interface OrderStatusListener { void onStatusChanged(Order order); }

// ===============================
// CHAT BRIDGE
// ===============================

class ChatMessage {
    public final String senderName, senderType, message;
    public final MessageType type;
    public final LocalDateTime timestamp;
    public List<SellerItem> sellerItems;
    public SpecialOffer specialOffer;
    public Order order;
    public String targetSellerId;

    public ChatMessage(String senderName, String senderType, String message, MessageType type) {
        this.senderName = senderName;
        this.senderType = senderType;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.targetSellerId = null;
    }
    
    public ChatMessage(String senderName, String senderType, String message, MessageType type, String targetSellerId) {
        this.senderName = senderName;
        this.senderType = senderType;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.targetSellerId = targetSellerId;
    }
    
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

interface ChatListener { void onMessageReceived(ChatMessage message); }

class ChatBridge {
    private final List<ChatMessage> history = new ArrayList<>();
    private final List<ChatListener> listeners = new ArrayList<>();
    private String buyerName = "Customer";
    private Map<String, List<String>> activeChatSessions = new HashMap<>(); // buyer -> list of sellerIds

    public void addListener(ChatListener l) { listeners.add(l); }
    public void removeListener(ChatListener l) { listeners.remove(l); }
    public void setBuyerName(String n) { buyerName = n; }
    public List<ChatMessage> getHistory() { return new ArrayList<>(history); }
    
    public void addActiveChatSession(String buyerId, String sellerId) {
        activeChatSessions.computeIfAbsent(buyerId, k -> new ArrayList<>()).add(sellerId);
    }
    
    public List<String> getActiveChatSessions(String buyerId) {
        return activeChatSessions.getOrDefault(buyerId, new ArrayList<>());
    }

    public void sendFromBuyerToSeller(String msg, String sellerId) {
        dispatch(new ChatMessage(buyerName, "BUYER", msg, MessageType.TEXT, sellerId));
    }
    
    public void sendFromBuyerToAll(String msg) {
        dispatch(new ChatMessage(buyerName, "BUYER", msg, MessageType.TEXT, null));
    }
    
    public void sendFromSeller(String sellerName, String msg) {
        dispatch(new ChatMessage(sellerName, "SELLER", msg, MessageType.TEXT, null));
    }
    
    public void sendFromSellerToBuyer(String sellerName, String msg, String sellerId) {
        dispatch(new ChatMessage(sellerName, "SELLER", msg, MessageType.TEXT, sellerId));
    }
    
    public void sendRecommendations(List<SellerItem> items, String msg) {
        ChatMessage cm = new ChatMessage("System", "SELLER", msg, MessageType.STORE_RECOMMENDATION);
        cm.sellerItems = items;
        dispatch(cm);
    }
    
    public void sendSpecialOffer(SpecialOffer offer) {
        ChatMessage cm = new ChatMessage("System", "SELLER", "🎁 Special Offer!", MessageType.SPECIAL_OFFER);
        cm.specialOffer = offer;
        dispatch(cm);
    }
    
    public void sendOrderUpdate(Order order) {
        ChatMessage cm = new ChatMessage("System", "SELLER",
            "Order " + order.getOrderId() + " → " + order.getStatus().displayName, MessageType.ORDER_UPDATE);
        cm.order = order;
        dispatch(cm);
    }
    
    public void sendSystem(String msg) {
        dispatch(new ChatMessage("System", "SYSTEM", msg, MessageType.SYSTEM));
    }

    private void dispatch(ChatMessage cm) {
        history.add(cm);
        for (ChatListener l : listeners) l.onMessageReceived(cm);
    }
}

// ===============================
// SHOPPING CART (Multi-Seller Support)
// ===============================

class ShoppingCart {
    private final List<CartItem> items = new ArrayList<>();

    public void addItem(SellerItem si, int qty) {
        for (CartItem ci : items) {
            if (ci.getSellerItem().item.getId().equals(si.item.getId()) &&
                ci.getSellerItem().seller.getId().equals(si.seller.getId())) {
                ci.setQuantity(ci.getQuantity() + qty);
                return;
            }
        }
        items.add(new CartItem(si, qty));
    }

    public void removeItem(String itemId, String sellerId) {
        items.removeIf(ci ->
            ci.getSellerItem().item.getId().equals(itemId) &&
            ci.getSellerItem().seller.getId().equals(sellerId));
    }

    public void updateQty(String itemId, String sellerId, int qty) {
        if (qty <= 0) { removeItem(itemId, sellerId); return; }
        for (CartItem ci : items) {
            if (ci.getSellerItem().item.getId().equals(itemId) &&
                ci.getSellerItem().seller.getId().equals(sellerId)) {
                ci.setQuantity(qty); break;
            }
        }
    }

    public List<CartItem> getItems() { return new ArrayList<>(items); }
    public boolean isEmpty() { return items.isEmpty(); }
    public int getTotal() { return items.stream().mapToInt(CartItem::getTotal).sum(); }
    public int getCount() { return items.stream().mapToInt(CartItem::getQuantity).sum(); }
    public void clear() { items.clear(); }

    // Group items by seller for multi-seller checkout
    public Map<Seller, List<CartItem>> getItemsBySeller() {
        Map<Seller, List<CartItem>> grouped = new LinkedHashMap<>();
        for (CartItem ci : items) {
            Seller seller = ci.getSellerItem().seller;
            grouped.computeIfAbsent(seller, k -> new ArrayList<>()).add(ci);
        }
        return grouped;
    }
    
    public List<Seller> getUniqueSellers() {
        return items.stream()
            .map(ci -> ci.getSellerItem().seller)
            .distinct()
            .collect(Collectors.toList());
    }
}

// ===============================
// MULTI-STORE SYSTEM
// ===============================

class MultiStoreSystem {
    private final List<Seller> sellers = new ArrayList<>();
    private final Map<String, String> synonyms = new HashMap<>();

    public MultiStoreSystem() {
        initSellers();
        initSynonyms();
    }

    private void initSellers() {
        // ---- PADANG ----
        Seller padang = new Seller("S001", "Warung Padang Sederhana", FoodCategory.PADANG, 4.7, 0.3, -6.06042450727696, 107.11927167171862);
        padang.getMenu().add(new MenuItem("S001-1", "Nasi Rendang", 18000, 4.8, 40, "food",
            "spicy", "savory", "beef", "indonesian", "padang", "nasi", "rice"));
        padang.getMenu().add(new MenuItem("S001-2", "Nasi Ayam Bakar", 17000, 4.6, 35, "food",
            "savory", "chicken", "indonesian", "padang", "nasi", "rice", "grilled"));
        padang.getMenu().add(new MenuItem("S001-3", "Gulai Ikan", 15000, 4.5, 30, "food",
            "spicy", "fish", "indonesian", "padang", "soup"));
        padang.getMenu().add(new MenuItem("S001-4", "Sayur Nangka", 8000, 4.3, 20, "food",
            "savory", "vegetables", "indonesian", "padang", "vegetarian", "cheap"));

        SellerItem rendang = new SellerItem(padang, padang.getMenu().get(0));
        SellerItem ayam = new SellerItem(padang, padang.getMenu().get(1));
        padang.getPromotions().add(new SpecialOffer("🔥 Padang Combo",
            "Nasi Rendang + Nasi Ayam Bakar", List.of(rendang, ayam), 20));
        sellers.add(padang);

        // ---- KOREAN ----
        Seller korean = new Seller("S002", "Korean Street Food", FoodCategory.KOREAN, 4.6, 1.2, -6.281181190579693, 107.1702099366738);
        korean.getMenu().add(new MenuItem("S002-1", "Tteokbokki", 25000, 4.7, 20, "food",
            "spicy", "sweet", "korean", "street food", "rice cake"));
        korean.getMenu().add(new MenuItem("S002-2", "Korean Fried Chicken", 35000, 4.8, 25, "food",
            "spicy", "sweet", "korean", "chicken", "crispy", "fried"));
        korean.getMenu().add(new MenuItem("S002-3", "Bibimbap", 28000, 4.6, 20, "food",
            "savory", "korean", "rice", "vegetables", "egg", "healthy"));
        korean.getMenu().add(new MenuItem("S002-4", "Ramyeon", 22000, 4.5, 15, "food",
            "spicy", "korean", "noodle", "soup", "hot", "mie"));
        korean.getMenu().add(new MenuItem("S002-5", "Kimchi Fried Rice", 25000, 4.7, 18, "food",
            "spicy", "korean", "rice", "kimchi", "savory"));

        SellerItem kfc = new SellerItem(korean, korean.getMenu().get(1));
        SellerItem tteok = new SellerItem(korean, korean.getMenu().get(0));
        korean.getPromotions().add(new SpecialOffer("🍗 Korean Feast",
            "Fried Chicken + Tteokbokki", List.of(kfc, tteok), 25));
        sellers.add(korean);

        // ---- FAST FOOD ----
        Seller fast = new Seller("S003", "Burger & Pasta Station", FoodCategory.FASTFOOD, 4.4, 0.9, -6.282143627317266, 107.17607760794165);
        fast.getMenu().add(new MenuItem("S003-1", "Beef Burger", 28000, 4.5, 12, "food",
            "savory", "beef", "burger", "western", "cheese", "fast"));
        fast.getMenu().add(new MenuItem("S003-2", "Chicken Burger", 25000, 4.4, 10, "food",
            "savory", "chicken", "burger", "western", "cheese", "fast"));
        fast.getMenu().add(new MenuItem("S003-3", "Carbonara Pasta", 30000, 4.6, 15, "food",
            "savory", "pasta", "western", "creamy", "cheese", "italian"));
        fast.getMenu().add(new MenuItem("S003-4", "French Fries", 15000, 4.3, 8, "food",
            "salty", "savory", "potato", "western", "crispy", "fast", "cheap"));
        fast.getMenu().add(new MenuItem("S003-5", "Aglio Olio", 27000, 4.5, 15, "food",
            "savory", "pasta", "western", "garlic", "italian"));

        SellerItem burger = new SellerItem(fast, fast.getMenu().get(0));
        SellerItem fries = new SellerItem(fast, fast.getMenu().get(3));
        fast.getPromotions().add(new SpecialOffer("🍔 Combo Deal",
            "Beef Burger + French Fries", List.of(burger, fries), 15));
        sellers.add(fast);

        // ---- HEALTHY ----
        Seller healthy = new Seller("S004", "Green Bowl & Salad", FoodCategory.HEALTHY, 4.8, 1.5, -6.294563719538023, 107.1664932375457);
        healthy.getMenu().add(new MenuItem("S004-1", "Quinoa Buddha Bowl", 35000, 4.9, 15, "food",
            "healthy", "vegetarian", "quinoa", "vegetables", "fresh", "organic"));
        healthy.getMenu().add(new MenuItem("S004-2", "Avocado Toast", 28000, 4.7, 8, "food",
            "healthy", "vegetarian", "bread", "avocado", "fresh", "breakfast"));
        healthy.getMenu().add(new MenuItem("S004-3", "Greek Salad", 25000, 4.6, 10, "food",
            "healthy", "vegetarian", "salad", "fresh", "vegetables", "cheese"));
        healthy.getMenu().add(new MenuItem("S004-4", "Smoothie Bowl", 32000, 4.8, 10, "food",
            "healthy", "sweet", "fruit", "fresh", "cold", "vegetarian"));

        sellers.add(healthy);

        // ---- WARTEG ----
        Seller warteg = new Seller("S005", "Warteg Bahagia", FoodCategory.WARTEG, 4.3, 0.2, -6.281666946346757, 107.16368150905166);
        warteg.getMenu().add(new MenuItem("S005-1", "Nasi Goreng", 15000, 4.5, 20, "food",
            "savory", "fried", "rice", "indonesian", "nasi", "cheap", "egg"));
        warteg.getMenu().add(new MenuItem("S005-2", "Nasi Goreng Seafood", 18000, 4.6, 25, "food",
            "savory", "seafood", "fried", "rice", "indonesian", "nasi"));
        warteg.getMenu().add(new MenuItem("S005-3", "Mie Goreng", 13000, 4.4, 18, "food",
            "savory", "fried", "noodle", "indonesian", "mie", "cheap"));
        warteg.getMenu().add(new MenuItem("S005-4", "Soto Ayam", 15000, 4.5, 20, "food",
            "savory", "chicken", "soup", "indonesian", "hot"));
        warteg.getMenu().add(new MenuItem("S005-5", "Tempe Orek", 8000, 4.2, 10, "food",
            "savory", "vegetarian", "indonesian", "cheap", "tempeh"));

        SellerItem ng = new SellerItem(warteg, warteg.getMenu().get(0));
        SellerItem soto = new SellerItem(warteg, warteg.getMenu().get(3));
        warteg.getPromotions().add(new SpecialOffer("🍚 Hemat Combo",
            "Nasi Goreng + Soto Ayam", List.of(ng, soto), 10));
        sellers.add(warteg);

        // ---- DESSERT ----
        Seller dessert = new Seller("S006", "Sweet Dessert House", FoodCategory.DESSERT, 4.9, 1.5, -6.297940068328937, 107.16611183569414);
        dessert.getMenu().add(new MenuItem("S006-1", "Chocolate Lava Cake", 35000, 4.9, 20, "dessert",
            "sweet", "chocolate", "cake", "warm", "rich"));
        dessert.getMenu().add(new MenuItem("S006-2", "Tiramisu", 30000, 4.8, 15, "dessert",
            "sweet", "coffee", "cake", "italian", "creamy", "cold"));
        dessert.getMenu().add(new MenuItem("S006-3", "Strawberry Cheesecake", 32000, 4.9, 15, "dessert",
            "sweet", "fruit", "strawberry", "creamy", "cold"));
        dessert.getMenu().add(new MenuItem("S006-4", "Ice Cream Sundae", 25000, 4.7, 5, "dessert",
            "sweet", "cold", "ice cream", "chocolate", "vanilla", "fast"));

        sellers.add(dessert);

        // ---- DRINKS ----
        Seller drinks = new Seller("S007", "Warung Es Teh Indonesia", FoodCategory.DRINKS, 4.7, 0.5, -6.297843680583663, 107.16217405179022);
        drinks.getMenu().add(new MenuItem("S007-1", "Es Teh Manis", 5000, 4.7, 5, "drink",
            "sweet", "ice", "cold", "tea", "indonesian", "cheap", "refreshing"));
        drinks.getMenu().add(new MenuItem("S007-2", "Es Jeruk", 8000, 4.6, 5, "drink",
            "sweet", "sour", "ice", "cold", "citrus", "refreshing", "cheap"));
        drinks.getMenu().add(new MenuItem("S007-3", "Es Kelapa Muda", 12000, 4.8, 5, "drink",
            "sweet", "ice", "cold", "coconut", "refreshing", "indonesian"));
        drinks.getMenu().add(new MenuItem("S007-4", "Thai Tea", 12000, 4.7, 5, "drink",
            "sweet", "ice", "cold", "tea", "milk", "creamy"));
        drinks.getMenu().add(new MenuItem("S007-5", "Es Kepal Milo", 15000, 4.8, 8, "drink",
            "sweet", "ice", "cold", "chocolate", "milo", "creamy"));

        SellerItem teh = new SellerItem(drinks, drinks.getMenu().get(0));
        SellerItem kelapa = new SellerItem(drinks, drinks.getMenu().get(2));
        drinks.getPromotions().add(new SpecialOffer("🥤 Refreshing Duo",
            "Es Teh Manis + Es Kelapa Muda", List.of(teh, kelapa), 15));
        sellers.add(drinks);
    }

    private void initSynonyms() {
        synonyms.put("manis", "sweet"); synonyms.put("pedas", "spicy");
        synonyms.put("asin", "salty"); synonyms.put("gurih", "savory");
        synonyms.put("asam", "sour"); synonyms.put("es", "cold");
        synonyms.put("dingin", "cold"); synonyms.put("panas", "hot");
        synonyms.put("makanan", "food"); synonyms.put("minuman", "drink");
        synonyms.put("nasi", "rice"); synonyms.put("mie", "noodle");
        synonyms.put("ayam", "chicken"); synonyms.put("sapi", "beef");
        synonyms.put("ikan", "fish"); synonyms.put("sayur", "vegetables");
        synonyms.put("buah", "fruit"); synonyms.put("korea", "korean");
        synonyms.put("indonesia", "indonesian"); synonyms.put("barat", "western");
        synonyms.put("murah", "cheap"); synonyms.put("sehat", "healthy");
        synonyms.put("cepat", "fast"); synonyms.put("goreng", "fried");
        synonyms.put("bakar", "grilled"); synonyms.put("sup", "soup");
    }

    public List<SellerItem> search(String query, Integer maxPrice, Boolean byRating, Boolean bySpeed) {
        Set<String> tags = parseQuery(query.toLowerCase());
        List<SellerItem> results = new ArrayList<>();

        for (Seller s : sellers) {
            for (MenuItem m : s.getMenu()) {
                if (tags.isEmpty() || m.hasAnyTag(tags)) {
                    if (maxPrice != null && m.getPrice() > maxPrice) continue;
                    results.add(new SellerItem(s, m));
                }
            }
        }

        if (byRating != null && byRating) {
            results.sort((a, b) -> Double.compare(b.item.getRating(), a.item.getRating()));
        } else if (bySpeed != null && bySpeed) {
            results.sort(Comparator.comparingInt(a -> a.item.getCookTimeMinutes()));
        } else {
            results.sort((a, b) -> Integer.compare(
                b.item.getMatchScore(tags), a.item.getMatchScore(tags)));
        }

        return results.stream().limit(8).collect(Collectors.toList());
    }

    private Set<String> parseQuery(String query) {
        Set<String> tags = new HashSet<>();
        for (String word : query.split("[\\s,+&/]+")) {
            word = word.trim();
            tags.add(synonyms.getOrDefault(word, word));
        }
        return tags;
    }

    public List<Seller> getSellers() { return new ArrayList<>(sellers); }
    public List<Seller> getSellersByCategory(FoodCategory cat) {
        return sellers.stream().filter(s -> s.getCategory() == cat).collect(Collectors.toList());
    }
    public Seller getSellerById(String id) {
        return sellers.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
    public List<SpecialOffer> getAllOffers() {
        return sellers.stream().flatMap(s -> s.getPromotions().stream()).collect(Collectors.toList());
    }
}

// ===============================
// ORDER HISTORY MANAGER
// ===============================

class OrderHistoryManager {
    private static final List<Order> completedOrders = new ArrayList<>();
    private static final List<OrderHistoryListener> listeners = new ArrayList<>();

    public static void addCompletedOrder(Order order) {
        completedOrders.add(order);
        notifyListeners();
    }

    public static List<Order> getCompletedOrders() {
        return new ArrayList<>(completedOrders);
    }

    public static void addListener(OrderHistoryListener listener) {
        listeners.add(listener);
    }

    private static void notifyListeners() {
        for (OrderHistoryListener l : listeners) {
            l.onOrderHistoryChanged();
        }
    }
}

interface OrderHistoryListener {
    void onOrderHistoryChanged();
}

// ===============================
// SIMPLE MAP WINDOW
// ===============================

class SimpleMapWindow extends JFrame {
    public SimpleMapWindow(Order order) {
        setTitle("🗺️ Delivery Map - " + order.getOrderId());
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 46));
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("🗺️ Delivery Route");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        header.add(title, BorderLayout.WEST);

        MapCanvas mapCanvas = new MapCanvas(order);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel fromLabel = new JLabel("📍 From: " + order.getSeller().getName());
        fromLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        JLabel toLabel = new JLabel("🏠 To: " + order.getAddress());
        toLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel distLabel = new JLabel("📏 Distance: ~" + 
            String.format("%.1f", order.getSeller().getDistanceKm()) + " km");
        distLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel customerLabel = new JLabel("👤 " + order.getCustomerName() + "  📞 " + order.getPhone());
        customerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerLabel.setForeground(Color.GRAY);

        infoPanel.add(fromLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(toLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(distLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        infoPanel.add(customerLabel);

        add(header, BorderLayout.NORTH);
        add(mapCanvas, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class MapCanvas extends JPanel {
    private final Order order;
    private static final int MARKER_SIZE = 28;

    public MapCanvas(Order order) {
        this.order = order;
        setBackground(new Color(230, 240, 255));
        setPreferredSize(new Dimension(700, 450));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2d.setColor(new Color(200, 220, 240));
        for (int i = 0; i < width; i += 40) {
            g2d.drawLine(i, 0, i, height);
        }
        for (int i = 0; i < height; i += 40) {
            g2d.drawLine(0, i, width, i);
        }

        int sellerX = width / 4;
        int sellerY = height / 2;
        double distance = order.getSeller().getDistanceKm();
        int offsetX = (int)(Math.min(distance * 40, width / 2.5));
        int offsetY = (int)((Math.random() - 0.5) * 100);
        int customerX = sellerX + offsetX;
        int customerY = sellerY + offsetY;

        g2d.setColor(new Color(33, 150, 243));
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
            0, new float[]{12, 6}, 0));
        g2d.drawLine(sellerX, sellerY, customerX, customerY);

        drawMarker(g2d, sellerX, sellerY, new Color(255, 152, 0), "🏪");
        g2d.setColor(new Color(50, 50, 80));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.drawString(order.getSeller().getName(), sellerX - 50, sellerY + MARKER_SIZE + 20);

        drawMarker(g2d, customerX, customerY, new Color(76, 175, 80), "🏠");
        g2d.setColor(new Color(50, 50, 80));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        String addr = order.getAddress();
        if (addr.length() > 28) addr = addr.substring(0, 25) + "...";
        g2d.drawString(addr, customerX - 50, customerY + MARKER_SIZE + 20);

        int midX = (sellerX + customerX) / 2;
        int midY = (sellerY + customerY) / 2 - 15;
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(midX - 45, midY - 15, 90, 30, 10, 10);
        g2d.setColor(new Color(33, 150, 243));
        g2d.drawRoundRect(midX - 45, midY - 15, 90, 30, 10, 10);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2d.drawString(String.format("%.1f km", distance), midX - 32, midY + 4);
    }

    private void drawMarker(Graphics2D g2d, int x, int y, Color color, String emoji) {
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillOval(x - MARKER_SIZE/2 + 3, y - MARKER_SIZE/2 + 3, MARKER_SIZE, MARKER_SIZE);
        g2d.setColor(color);
        g2d.fillOval(x - MARKER_SIZE/2, y - MARKER_SIZE/2, MARKER_SIZE, MARKER_SIZE);
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - MARKER_SIZE/2, y - MARKER_SIZE/2, MARKER_SIZE, MARKER_SIZE);
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        g2d.drawString(emoji, x - 9, y + 7);
    }
}

// ===============================
// SELLER WINDOW
// ===============================

class SellerWindow extends JFrame implements OrderStatusListener, ChatListener {
    private final Seller seller;
    private final ChatBridge chatBridge;
    private final JPanel ordersContainer;
    private final List<Order> activeOrders = new ArrayList<>();
    private final JLabel statusLabel;
    private JPanel chatContainer;
    private JScrollPane chatScroll;
    private JTextField msgField;

    public SellerWindow(Seller seller, ChatBridge chatBridge) {
        this.seller = seller;
        this.chatBridge = chatBridge;
        seller.setWindow(this);
        chatBridge.addListener(this);

        setTitle(seller.getCategoryDisplay() + " — " + seller.getName());
        setSize(750, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(250, 250, 252));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 46));
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        JPanel titlePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePane.setOpaque(false);

        JLabel catLabel = new JLabel(seller.getCategory().emoji);
        catLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JLabel nameLabel = new JLabel(seller.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        nameLabel.setForeground(Color.WHITE);
        JLabel ratingLabel = new JLabel(String.format("⭐ %.1f  •  📏 %.1fkm", seller.getRating(), seller.getDistanceKm()));
        ratingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ratingLabel.setForeground(new Color(180, 180, 200));
        titlePane.add(catLabel); titlePane.add(nameLabel); titlePane.add(ratingLabel);

        JPanel rightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPane.setOpaque(false);

        statusLabel = new JLabel("🟢 Open");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        statusLabel.setForeground(new Color(100, 220, 100));

        JButton busyBtn = new JButton("⏳ Toggle Busy");
        busyBtn.setBackground(new Color(255, 152, 0));
        busyBtn.setForeground(Color.WHITE);
        busyBtn.setOpaque(true);
        busyBtn.setBorderPainted(false);
        busyBtn.setFocusPainted(false);
        busyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        busyBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        busyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        busyBtn.addActionListener(e -> {
            seller.setBusy(!seller.isBusy());
            if (seller.isBusy()) {
                statusLabel.setText("🔴 Busy");
                statusLabel.setForeground(new Color(255, 100, 100));
                chatBridge.sendSystem("⚠️ " + seller.getName() + " is currently busy. Orders may take longer.");
            } else {
                statusLabel.setText("🟢 Open");
                statusLabel.setForeground(new Color(100, 220, 100));
                chatBridge.sendSystem("✅ " + seller.getName() + " is back and ready for orders!");
            }
        });

        JButton promoBtn = new JButton("🎁 Send Promo");
        promoBtn.setBackground(new Color(156, 39, 176));
        promoBtn.setForeground(Color.WHITE);
        promoBtn.setOpaque(true);
        promoBtn.setBorderPainted(false);
        promoBtn.setFocusPainted(false);
        promoBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        promoBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        promoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        promoBtn.addActionListener(e -> showPromoDialog());

        rightPane.add(statusLabel); rightPane.add(busyBtn); rightPane.add(promoBtn);
        header.add(titlePane, BorderLayout.WEST);
        header.add(rightPane, BorderLayout.EAST);

        JPanel menuBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        menuBar.setBackground(new Color(240, 240, 248));
        menuBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 230)),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JLabel menuTitle = new JLabel("Menu: ");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuBar.add(menuTitle);
        for (MenuItem mi : seller.getMenu()) {
            JLabel tag = new JLabel(mi.getName() + " Rp" + String.format("%,d", mi.getPrice()));
            tag.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            tag.setForeground(new Color(70, 70, 100));
            tag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)), 
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
            tag.setOpaque(true); tag.setBackground(Color.WHITE);
            menuBar.add(tag);
        }

        ordersContainer = new JPanel();
        ordersContainer.setLayout(new BoxLayout(ordersContainer, BoxLayout.Y_AXIS));
        ordersContainer.setBackground(new Color(248, 248, 252));
        ordersContainer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel ordersTitle = new JLabel("   📋 Incoming Orders");
        ordersTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        ordersTitle.setForeground(new Color(60, 60, 80));
        ordersTitle.setOpaque(true);
        ordersTitle.setBackground(new Color(248, 248, 252));
        ordersTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        ordersContainer.add(ordersTitle);

        JPanel emptyLabel = makeEmptyOrdersLabel();
        ordersContainer.add(emptyLabel);

        JScrollPane ordersScroll = new JScrollPane(ordersContainer);
        ordersScroll.setBorder(null);
        ordersScroll.getVerticalScrollBar().setUnitIncrement(20);

        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 250));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(220, 220, 230)));
        chatScroll.getVerticalScrollBar().setUnitIncrement(20);

        JLabel chatTitle = new JLabel("   💬 Customer Chat");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatTitle.setForeground(new Color(60, 60, 80));
        chatTitle.setOpaque(true);
        chatTitle.setBackground(new Color(240, 240, 250));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        chatContainer.add(chatTitle);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(ordersScroll);
        splitPane.setRightComponent(chatScroll);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(menuBar, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);

        JPanel msgPanel = new JPanel(new BorderLayout(10, 0));
        msgPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 230)),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        msgPanel.setBackground(Color.WHITE);
        msgField = new JTextField();
        msgField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        msgField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        msgField.setToolTipText("Send a message to customer...");
        JButton msgBtn = new JButton("💬 Send");
        msgBtn.setBackground(new Color(33, 150, 243));
        msgBtn.setForeground(Color.WHITE);
        msgBtn.setOpaque(true);
        msgBtn.setBorderPainted(false);
        msgBtn.setFocusPainted(false);
        msgBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        msgBtn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        msgBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ActionListener sendMsg = ae -> {
            String txt = msgField.getText().trim();
            if (!txt.isEmpty()) {
                chatBridge.sendFromSellerToBuyer(seller.getName(), txt, seller.getId());
                msgField.setText("");
            }
        };
        msgField.addActionListener(sendMsg);
        msgBtn.addActionListener(sendMsg);
        msgPanel.add(msgField, BorderLayout.CENTER);
        msgPanel.add(msgBtn, BorderLayout.EAST);

        JPanel bottomStack = new JPanel(new BorderLayout());
        bottomStack.add(menuBar, BorderLayout.NORTH);
        bottomStack.add(msgPanel, BorderLayout.SOUTH);
        add(bottomStack, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatBridge.removeListener(SellerWindow.this);
            }
        });
    }

    private JPanel makeEmptyOrdersLabel() {
        JPanel p = new JPanel();
        p.setBackground(new Color(248, 248, 252));
        p.setBorder(BorderFactory.createEmptyBorder(60, 0, 60, 0));
        JLabel lbl = new JLabel("<html><center>⏳<br><br>Waiting for orders...</center></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lbl.setForeground(new Color(160, 160, 180));
        p.add(lbl);
        return p;
    }

    public void receiveOrder(Order order) {
        activeOrders.add(order);
        seller.setCurrentQueueCount(activeOrders.size());
        order.addStatusListener(this);

        SwingUtilities.invokeLater(() -> {
            if (ordersContainer.getComponentCount() == 2 &&
                ordersContainer.getComponent(1) instanceof JPanel) {
                ordersContainer.remove(1);
            }
            ordersContainer.add(createOrderCard(order));
            ordersContainer.revalidate();
            ordersContainer.repaint();
        });

        chatBridge.sendFromSellerToBuyer(seller.getName(),
            "✅ Order " + order.getOrderId() + " received! " + seller.getName() + " is preparing your food. Est. " +
            order.getEstimatedMinutes() + " minutes.", seller.getId());
    }

    private JPanel createOrderCard(Order order) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 210, 230), 1),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel orderId = new JLabel("🆔 " + order.getOrderId() + "  •  " + order.getFormattedTime());
        orderId.setFont(new Font("Segoe UI", Font.BOLD, 14));
        orderId.setForeground(new Color(100, 100, 130));

        JLabel customer = new JLabel("👤 " + order.getCustomerName() + "  📞 " + order.getPhone());
        customer.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel addr = new JLabel("📍 " + order.getAddress());
        addr.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addr.setForeground(Color.GRAY);

        StringBuilder itemsText = new StringBuilder("<html>");
        for (CartItem ci : order.getItems()) {
            itemsText.append("• ").append(ci.getSellerItem().item.getName())
                .append(" x").append(ci.getQuantity())
                .append(" = Rp ").append(String.format("%,d", ci.getTotal())).append("<br>");
        }
        itemsText.append("</html>");
        JLabel itemsLabel = new JLabel(itemsText.toString());
        itemsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel total = new JLabel("💰 Total: Rp " + String.format("%,d", order.getSubtotal()) +
            "   ⏱ Est: " + order.getEstimatedMinutes() + " min");
        total.setFont(new Font("Segoe UI", Font.BOLD, 15));
        total.setForeground(new Color(33, 150, 243));

        JLabel statusLbl = new JLabel(order.getStatus().displayName);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLbl.setForeground(order.getStatus().color);

        info.add(orderId); info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(customer); info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(addr); info.add(Box.createRigidArea(new Dimension(0, 8)));
        info.add(itemsLabel); info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(total); info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(statusLbl);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        String[][] btnDefs = {
            {"✅ Accept", "33,150,243"},
            {"🔄 On Process", "156,39,176"},
            {"🚚 Driver On Way", "0,188,212"},
            {"✔️ Complete", "76,175,80"},
            {"⏳ Busy", "255,87,34"},
            {"❌ Reject", "244,67,54"},
            {"🗺️ Map", "120,120,120"}
        };
        OrderStatus[] statuses = {
            OrderStatus.ACCEPTED, OrderStatus.ON_PROCESS,
            OrderStatus.DRIVER_ON_WAY, OrderStatus.COMPLETED,
            OrderStatus.BUSY, OrderStatus.REJECTED,
            null
        };

        for (int i = 0; i < btnDefs.length; i++) {
            String[] def = btnDefs[i];
            OrderStatus target = statuses[i];
            String[] rgb = def[1].split(",");
            Color btnColor = new Color(Integer.parseInt(rgb[0].trim()),
                Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));

            JButton btn = new JButton(def[0]);
            btn.setBackground(btnColor);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            btn.setMaximumSize(new Dimension(170, 34));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                if (target != null) {
                    order.updateStatus(target);
                    statusLbl.setText(order.getStatus().displayName);
                    statusLbl.setForeground(order.getStatus().color);
                    chatBridge.sendOrderUpdate(order);
                } else {
                    new SimpleMapWindow(order);
                }
            });

            JButton chatBtn = new JButton("💬 Chat");
            chatBtn.setBackground(new Color(60, 60, 60));
            chatBtn.setForeground(Color.WHITE);
            chatBtn.setOpaque(true);
            chatBtn.setBorderPainted(false);
            chatBtn.setFocusPainted(false);
            chatBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            chatBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            chatBtn.setMaximumSize(new Dimension(170, 34));
            chatBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            chatBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chatBtn.addActionListener(e -> {
                msgField.setText("Regarding your order " + order.getOrderId() + ": ");
                msgField.requestFocus();
            });

            buttons.add(btn);
            buttons.add(Box.createRigidArea(new Dimension(0, 6)));
            if (i == 0) { // After Accept button
                buttons.add(chatBtn);
                buttons.add(Box.createRigidArea(new Dimension(0, 6)));
            }
        }

        card.add(info, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.EAST);

        return card;
    }

    private void showPromoDialog() {
        if (seller.getPromotions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No promotions configured for this seller.");
            return;
        }

        JDialog dlg = new JDialog(this, "📢 Send Promotion", true);
        dlg.setSize(550, 400);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        for (SpecialOffer offer : seller.getPromotions()) {
            JPanel card = new JPanel(new BorderLayout(15, 0));
            card.setBackground(new Color(255, 249, 235));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

            JLabel info = new JLabel(String.format(
                "<html><b style='font-size:14px'>%s</b><br>%s<br>💰 Rp%,d → Rp%,d (Save %d%%)</html>",
                offer.getTitle(), offer.getDescription(),
                offer.getOriginalPrice(), offer.getOfferPrice(), offer.getDiscountPercent()));
            info.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            JButton sendBtn = new JButton("📢 Send");
            sendBtn.setBackground(new Color(255, 152, 0));
            sendBtn.setForeground(Color.WHITE);
            sendBtn.setFocusPainted(false);
            sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            sendBtn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            sendBtn.addActionListener(e -> {
                chatBridge.sendSpecialOffer(offer);
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Promotion sent to customer!");
            });

            card.add(info, BorderLayout.CENTER);
            card.add(sendBtn, BorderLayout.EAST);
            panel.add(card);
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
        }

        JPanel customPane = new JPanel(new BorderLayout(10, 0));
        customPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 220)), "Custom Message"));
        JTextField customField = new JTextField();
        customField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton customSend = new JButton("Send");
        customSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        customSend.addActionListener(e -> {
            String txt = customField.getText().trim();
            if (!txt.isEmpty()) {
                chatBridge.sendFromSellerToBuyer(seller.getName(), "🔥 " + txt, seller.getId());
                dlg.dispose();
            }
        });
        customPane.add(customField, BorderLayout.CENTER);
        customPane.add(customSend, BorderLayout.EAST);
        panel.add(customPane);

        dlg.add(new JScrollPane(panel), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    @Override
    public void onStatusChanged(Order order) {
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        if (message.type == MessageType.SPECIAL_OFFER) {
            if (message.specialOffer != null && !message.specialOffer.getItems().isEmpty()) {
                boolean isOwnOffer = message.specialOffer.getItems().stream()
                    .anyMatch(si -> si.seller.getId().equals(seller.getId()));
                if (!isOwnOffer) return;
            } else {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                addOfferNoteBubble(message);
                scrollChatToBottom();
            });
            return;
        }
        
        if (message.type != MessageType.TEXT) return;
        
        if (message.senderType.equals("BUYER")) {
            if (message.targetSellerId != null && !message.targetSellerId.equals(seller.getId())) {
                return;
            }
        } else if (message.senderType.equals("SELLER")) {
            if (!message.senderName.equals(seller.getName())) return;
        }

        SwingUtilities.invokeLater(() -> {
            addChatBubble(message);
            scrollChatToBottom();
        });
    }

    private void addOfferNoteBubble(ChatMessage msg) {
        SpecialOffer offer = msg.specialOffer;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        row.setBackground(new Color(245, 245, 250));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 6));
        bubble.setBackground(new Color(255, 249, 235));
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel title = new JLabel("📢 Promo Sent: " + offer.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(180, 100, 0));

        JLabel detail = new JLabel(String.format("<html>%s — <b>%d%% off</b>, Rp%,d → Rp%,d</html>",
            offer.getDescription(), offer.getDiscountPercent(),
            offer.getOriginalPrice(), offer.getOfferPrice()));
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel time = new JLabel(msg.getFormattedTime());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(Color.GRAY);

        bubble.add(title, BorderLayout.NORTH);
        bubble.add(detail, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        bubble.setMaximumSize(new Dimension(260, bubble.getPreferredSize().height));
        row.add(bubble);
        row.setMaximumSize(new Dimension(280, bubble.getPreferredSize().height + 20));

        chatContainer.add(row);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void addChatBubble(ChatMessage msg) {
        boolean isBuyer = msg.senderType.equals("BUYER");
        JPanel row = new JPanel(new FlowLayout(isBuyer ? FlowLayout.LEFT : FlowLayout.RIGHT, 12, 6));
        row.setBackground(new Color(245, 245, 250));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 5));
        if (isBuyer) {
            bubble.setBackground(Color.WHITE);
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        } else {
            bubble.setBackground(new Color(230, 245, 230));
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 200, 100)),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        }

        if (isBuyer) {
            JLabel sender = new JLabel(msg.senderName);
            sender.setFont(new Font("Segoe UI", Font.BOLD, 14));
            sender.setForeground(new Color(100, 100, 150));
            bubble.add(sender, BorderLayout.NORTH);
        }

        JLabel text = new JLabel("<html><div style='width:180px; font-size:15px'>" + msg.message + "</div></html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        text.setForeground(new Color(30, 30, 50));

        JLabel time = new JLabel(msg.getFormattedTime());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(Color.GRAY);

        bubble.add(text, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        bubble.setMaximumSize(new Dimension(260, bubble.getPreferredSize().height));
        row.add(bubble);
        row.setMaximumSize(new Dimension(280, bubble.getPreferredSize().height + 14));

        chatContainer.add(row);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void scrollChatToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}

// ===============================
// BUYER WINDOW (Multi-Seller Order Support)
// ===============================

class BuyerChatWindow extends JFrame implements ChatListener {
    private final ChatBridge chatBridge;
    private final MultiStoreSystem storeSystem;
    private final ShoppingCart cart;
    private JPanel chatContainer;
    private JTextField inputField;
    private JLabel cartCountLbl, cartTotalLbl;
    private JPanel cartPanel;
    private JScrollPane chatScroll;
    private JPanel sellerStatusBar;
    private JComboBox<String> activeChatSelector;
    private List<Order> activeOrders = new ArrayList<>();

    public BuyerChatWindow(ChatBridge chatBridge, MultiStoreSystem storeSystem) {
        this.chatBridge = chatBridge;
        this.storeSystem = storeSystem;
        this.cart = new ShoppingCart();
        chatBridge.addListener(this);

        setTitle("🍔 FoodChat — Multi-Seller Food Ordering");
        setSize(1000, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 252));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buildHeader(), BorderLayout.NORTH);
        sellerStatusBar = buildSellerStatusBar();
        topPanel.add(sellerStatusBar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(700);
        split.setResizeWeight(0.68);
        split.setBorder(null);

        // Chat area with seller selector
        JPanel chatAreaPanel = new JPanel(new BorderLayout());
        
        JPanel chatHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        chatHeader.setBackground(new Color(240, 240, 248));
        chatHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        JLabel chatWithLabel = new JLabel("💬 Chat with: ");
        chatWithLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        activeChatSelector = new JComboBox<>();
        activeChatSelector.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        activeChatSelector.setPreferredSize(new Dimension(200, 32));
        activeChatSelector.addItem("💬 General Chat");
        activeChatSelector.addActionListener(e -> refreshChatHistory());
        
        chatHeader.add(chatWithLabel);
        chatHeader.add(activeChatSelector);
        
        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 252));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(20);
        
        chatAreaPanel.add(chatHeader, BorderLayout.NORTH);
        chatAreaPanel.add(chatScroll, BorderLayout.CENTER);
        
        split.setLeftComponent(chatAreaPanel);

        cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBackground(Color.WHITE);
        cartPanel.setBorder(new MatteBorder(0, 1, 0, 0, new Color(220, 220, 235)));
        refreshCart();
        split.setRightComponent(cartPanel);

        add(split, BorderLayout.CENTER);
        add(buildInputBar(), BorderLayout.SOUTH);

        showWelcome();
        setVisible(true);
    }
    
    private void refreshChatHistory() {
        chatContainer.removeAll();
        String selected = (String) activeChatSelector.getSelectedItem();
        if (selected != null && selected.endsWith(" (*)")) {
            // Remove indicator when selected
            String cleanName = selected.substring(0, selected.length() - 4);
            int idx = activeChatSelector.getSelectedIndex();
            activeChatSelector.removeItemAt(idx);
            activeChatSelector.insertItemAt(cleanName, idx);
            activeChatSelector.setSelectedIndex(idx);
            selected = cleanName;
        }
        String targetSellerId = null;
        
        if (selected != null && !selected.equals("💬 General Chat")) {
            // Extract seller ID from selection
            for (Seller s : storeSystem.getSellers()) {
                if (selected != null && selected.startsWith(s.getName())) {
                    targetSellerId = s.getId();
                    break;
                }
            }
        }
        
        // Show messages based on selection
        for (ChatMessage msg : chatBridge.getHistory()) {
            if (targetSellerId == null) {
                // General chat - show messages not targeted to specific seller
                if (msg.targetSellerId == null || msg.senderType.equals("BUYER")) {
                    renderMessage(msg);
                }
            } else {
                // Private chat with specific seller
                if ((msg.senderType.equals("BUYER") && targetSellerId.equals(msg.targetSellerId)) ||
                    (msg.senderType.equals("SELLER") && targetSellerId.equals(msg.targetSellerId)) ||
                    (msg.senderType.equals("SELLER") && msg.senderName.equals(selected))) {
                    renderMessage(msg);
                } else if (msg.senderType.equals("BUYER") && msg.targetSellerId == null) {
                    // Also show buyer's general messages in private chat for context
                    renderMessage(msg);
                }
            }
        }
        
        if (chatContainer.getComponentCount() == 0) {
            JPanel empty = new JPanel();
            empty.setBackground(new Color(245, 245, 252));
            JLabel lbl = new JLabel("<html><center>💬<br><br>No messages yet<br><small>Start chatting!</small></center></html>");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lbl.setForeground(Color.GRAY);
            empty.add(lbl);
            chatContainer.add(empty);
        }
        
        chatContainer.revalidate();
        chatContainer.repaint();
        scrollToBottom();
    }
    
    private void updateChatSelector() {
        activeChatSelector.removeAllItems();
        activeChatSelector.addItem("💬 General Chat");
        
        List<Seller> sellersWithOrders = new ArrayList<>();
        for (Order order : activeOrders) {
            Seller s = order.getSeller();
            if (!sellersWithOrders.contains(s)) {
                sellersWithOrders.add(s);
                activeChatSelector.addItem(s.getName());
            }
        }
        
        // Also add sellers from cart if any
        for (Seller s : cart.getUniqueSellers()) {
            if (!sellersWithOrders.contains(s)) {
                sellersWithOrders.add(s);
                activeChatSelector.addItem(s.getName());
            }
        }
        
        activeChatSelector.revalidate();
        activeChatSelector.repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 22, 38));
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("🍔");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        JLabel title = new JLabel("FoodChat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Multi-Seller Food Ordering");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(150, 150, 180));
        left.add(logo); left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);

        cartCountLbl = new JLabel("🛒 0 items");
        cartCountLbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        cartCountLbl.setForeground(new Color(180, 180, 200));

        cartTotalLbl = new JLabel("Rp 0");
        cartTotalLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cartTotalLbl.setForeground(new Color(100, 220, 100));

        JButton historyBtn = new JButton("📜 History");
        historyBtn.setBackground(new Color(156, 39, 176));
        historyBtn.setForeground(Color.WHITE);
        historyBtn.setOpaque(true);
        historyBtn.setBorderPainted(false);
        historyBtn.setFocusPainted(false);
        historyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        historyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyBtn.addActionListener(e -> new OrderHistoryWindow());

        JButton sellerBtn = new JButton("🏪 Browse Sellers");
        sellerBtn.setBackground(new Color(33, 150, 243));
        sellerBtn.setForeground(Color.WHITE);
        sellerBtn.setOpaque(true);
        sellerBtn.setBorderPainted(false);
        sellerBtn.setFocusPainted(false);
        sellerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sellerBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sellerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sellerBtn.addActionListener(e -> showSellerBrowser());

        right.add(cartCountLbl); right.add(cartTotalLbl); right.add(historyBtn); right.add(sellerBtn);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSellerStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(new Color(16, 16, 28));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JLabel lbl = new JLabel("SELLERS:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(120, 120, 160));
        bar.add(lbl);

        for (Seller s : storeSystem.getSellers()) {
            Color pillBg = s.isBusy() ? new Color(80, 30, 30) : new Color(20, 60, 30);
            Color pillFg = s.isBusy() ? new Color(255, 120, 120) : new Color(100, 220, 120);
            String dot = s.isBusy() ? "🔴 " : "🟢 ";
            JButton pill = new JButton(dot + s.getCategory().emoji + " " + s.getName());
            pill.setFont(new Font("Segoe UI", Font.BOLD, 12));
            pill.setBackground(pillBg);
            pill.setForeground(pillFg);
            pill.setOpaque(true);
            pill.setFocusPainted(false);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(pillFg.darker(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.setToolTipText("Click to focus " + s.getName() + " window");
            pill.addActionListener(e -> openSellerWindow(s));
            bar.add(pill);
        }
        return bar;
    }

    public void refreshSellerStatusBar() {
        if (sellerStatusBar == null) return;
        sellerStatusBar.removeAll();

        JLabel lbl = new JLabel("SELLERS:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(120, 120, 160));
        sellerStatusBar.add(lbl);

        for (Seller s : storeSystem.getSellers()) {
            Color pillBg = s.isBusy() ? new Color(80, 30, 30) : new Color(20, 60, 30);
            Color pillFg = s.isBusy() ? new Color(255, 120, 120) : new Color(100, 220, 120);
            String dot = s.isBusy() ? "🔴 " : "🟢 ";
            JButton pill = new JButton(dot + s.getCategory().emoji + " " + s.getName());
            pill.setFont(new Font("Segoe UI", Font.BOLD, 12));
            pill.setBackground(pillBg);
            pill.setForeground(pillFg);
            pill.setFocusPainted(false);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(pillFg.darker(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.setToolTipText(s.getName() + " — Est. " + s.getEstimatedWaitTime() + " min");
            pill.addActionListener(e -> openSellerWindow(s));
            sellerStatusBar.add(pill);
        }
        sellerStatusBar.revalidate();
        sellerStatusBar.repaint();
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(210, 210, 230)),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        bar.setBackground(Color.WHITE);

        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickPanel.setOpaque(false);
        String[] quickBtns = {"🍛 Padang", "🍜 Korean", "🥗 Healthy", "🍚 Warteg", "🎁 Special Offers"};
        String[] quickQueries = {"padang", "korea", "healthy", "warteg", "special"};
        for (int i = 0; i < quickBtns.length; i++) {
            String q = quickQueries[i];
            JButton qb = new JButton(quickBtns[i]);
            qb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            qb.setBackground(new Color(240, 240, 252));
            qb.setForeground(new Color(30, 30, 120));
            qb.setOpaque(true);
            qb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 225)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            qb.setFocusPainted(false);
            qb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            qb.addActionListener(e -> {
                inputField.setText(q);
                sendMessage();
            });
            quickPanel.add(qb);
        }

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        inputField.setToolTipText("Ask for food or chat with sellers...");
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220)),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("📤 Send");
        sendBtn.setBackground(new Color(33, 150, 243));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setOpaque(true);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        sendBtn.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        bar.add(quickPanel, BorderLayout.NORTH);
        bar.add(inputRow, BorderLayout.CENTER);
        return bar;
    }

    private void showWelcome() {
        JPanel wp = new JPanel();
        wp.setLayout(new BoxLayout(wp, BoxLayout.Y_AXIS));
        wp.setBackground(new Color(245, 245, 252));
        wp.setBorder(BorderFactory.createEmptyBorder(60, 80, 60, 80));
        wp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emoji = new JLabel("🍔🍜🍛🥗");
        emoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        emoji.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel h = new JLabel("Welcome to FoodChat!");
        h.setFont(new Font("Segoe UI", Font.BOLD, 32));
        h.setForeground(new Color(30, 30, 50));
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        String tips = "<html><center><p style='font-size:15px;color:#555'>💬 Smart Chat — Try typing:</p>" +
            "<ul style='text-align:left; font-size:14px'>" +
            "<li>\"<b>Hello</b>\" — Get started</li>" +
            "<li>\"<b>Help</b>\" — See all restaurants & menus</li>" +
            "<li>\"<b>Special offer</b>\" — Today's deals</li>" +
            "<li>\"<b>What's popular?</b>\" — Top-rated items</li>" +
            "<li>\"<b>Nasi goreng under 20k</b>\" — Search with budget</li>" +
            "<li>\"<b>Fastest food</b>\" — Quick delivery</li>" +
            "<li>\"<b>Show Korean food</b>\" — By category</li>" +
            "<li>\"<b>Are sellers open?</b>\" — Check status</li>" +
            "<li>\"<b>Add from multiple sellers</b>\" — Mix and match!</li>" +
            "</ul></center></html>";
        JLabel tipsLabel = new JLabel(tips);
        tipsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tipsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        wp.add(emoji); wp.add(Box.createRigidArea(new Dimension(0, 18)));
        wp.add(h); wp.add(Box.createRigidArea(new Dimension(0, 22)));
        wp.add(tipsLabel);

        chatContainer.add(wp);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        
        String selected = (String) activeChatSelector.getSelectedItem();
        if (selected != null && !selected.equals("💬 General Chat")) {
            // Send to specific seller
            for (Seller s : storeSystem.getSellers()) {
                if (selected.startsWith(s.getName())) {
                    chatBridge.sendFromBuyerToSeller(msg, s.getId());
                    break;
                }
            }
        } else {
            // Send to general chat (all sellers can see)
            chatBridge.sendFromBuyerToAll(msg);
        }
        
        inputField.setText("");
        
        // Only process search queries in general chat
        if (selected == null || selected.equals("💬 General Chat")) {
            processQuery(msg);
        }
    }

    private void processQuery(String query) {
        String lower = query.toLowerCase();
        
        if (lower.matches("(hi|hello|hey|halo|hai|pagi|siang|malam|selamat.*)[!.?]*")) {
            Timer t = new Timer(600, e -> 
                chatBridge.sendFromBuyerToAll("Hello! 👋 Welcome to FoodChat! What can I help you find today? Try: 'nasi goreng', 'korean food', 'special offer', or 'cheap food'"));
            t.setRepeats(false); t.start();
            return;
        }

        if (lower.contains("help") || lower.contains("bantuan") || lower.contains("apa aja") || 
            lower.contains("what can") || lower.contains("list menu") || lower.contains("show menu")) {
            Timer t = new Timer(600, e -> {
                StringBuilder sb = new StringBuilder("<b>📋 All Restaurants & Menus:</b><br><br>");
                for (Seller s : storeSystem.getSellers()) {
                    sb.append("<font color='#2196F3'>🏪 <b>").append(s.getName()).append("</b></font> ")
                      .append("<font color='#FFA000'>(⭐ ").append(s.getRating()).append(")</font><br>");
                    for (MenuItem item : s.getMenu()) {
                        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;• ").append(item.getName())
                          .append(" <font color='#777777'>(Rp ").append(String.format("%,d", item.getPrice())).append(")</font><br>");
                    }
                    sb.append("<br>");
                }
                sb.append("<i>💡 Tip: Type a food name or category to find specific items!</i>");
                chatBridge.sendFromBuyerToAll(sb.toString());
            });
            t.setRepeats(false); t.start();
            return;
        }

        if (lower.matches(".*(thank|terima|makasih).*")) {
            Timer t = new Timer(500, e -> 
                chatBridge.sendFromBuyerToAll("You're welcome! 😊 Anything else?"));
            t.setRepeats(false); t.start();
            return;
        }

        // Check if query matches a specific seller name
        for (Seller s : storeSystem.getSellers()) {
            if (lower.contains(s.getName().toLowerCase())) {
                List<SellerItem> sellerItems = new ArrayList<>();
                for (MenuItem item : s.getMenu()) {
                    sellerItems.add(new SellerItem(s, item));
                }
                Timer t = new Timer(700, e -> {
                    chatBridge.sendRecommendations(sellerItems, "Here is the full menu from " + s.getName() + ":");
                });
                t.setRepeats(false); t.start();
                return;
            }
        }

        if (lower.contains("warteg")) {
            recommendByCategory(FoodCategory.WARTEG);
            return;
        }
        if (lower.contains("padang")) {
            recommendByCategory(FoodCategory.PADANG);
            return;
        }
        if (lower.contains("korean") || lower.contains("korea")) {
            recommendByCategory(FoodCategory.KOREAN);
            return;
        }
        if (lower.contains("healthy") || lower.contains("sehat")) {
            recommendByCategory(FoodCategory.HEALTHY);
            return;
        }
        if (lower.contains("fast") || lower.contains("burger")) {
            recommendByCategory(FoodCategory.FASTFOOD);
            return;
        }
        if (lower.contains("dessert") || lower.contains("sweet")) {
            recommendByCategory(FoodCategory.DESSERT);
            return;
        }
        if (lower.contains("drink") || lower.contains("minum") || lower.contains("teh")) {
            recommendByCategory(FoodCategory.DRINKS);
            return;
        }

        if (lower.contains("special") || lower.contains("offer") || lower.contains("promo")
            || lower.contains("diskon") || lower.contains("discount") || lower.contains("deal")) {
            Timer t = new Timer(800, e -> {
                chatBridge.sendFromBuyerToAll("🎁 Here are today's special offers:");
                Timer t2 = new Timer(400, e2 -> {
                    List<SpecialOffer> offers = storeSystem.getAllOffers();
                    for (SpecialOffer offer : offers) chatBridge.sendSpecialOffer(offer);
                });
                t2.setRepeats(false); t2.start();
            });
            t.setRepeats(false); t.start();
            return;
        }

        if (lower.contains("open") || lower.contains("available") || lower.contains("buka") || 
            lower.contains("tutup") || lower.contains("busy")) {
            Timer t = new Timer(600, e -> {
                StringBuilder sb = new StringBuilder("📊 Seller Status:\n");
                for (Seller s : storeSystem.getSellers()) {
                    String status = s.isBusy() ? "🔴 Busy (~" + s.getEstimatedWaitTime() + " min wait)" 
                                               : "🟢 Open (~" + s.getEstimatedWaitTime() + " min)";
                    sb.append("• ").append(s.getName()).append(": ").append(status).append("\n");
                }
                chatBridge.sendFromBuyerToAll(sb.toString());
            });
            t.setRepeats(false); t.start();
            return;
        }

        if (lower.matches(".*(recommend|suggest|rekomendasi|saranin|what should|apa yang).*") 
            && !lower.contains("food") && query.length() < 50) {
            Timer t = new Timer(700, e -> {
                chatBridge.sendFromBuyerToAll("🤔 What are you in the mood for?\n• Spicy (pedas)\n• Sweet (manis)\n• Healthy (sehat)\n• Fast/Quick (cepat)\n• Cheap (murah)\n\nOr tell me a category: Korean, Padang, Warteg, etc.");
            });
            t.setRepeats(false); t.start();
            return;
        }

        if (lower.matches(".*(popular|favorit|favorite|best seller|terlaris).*")) {
            Timer t = new Timer(700, e -> {
                List<SellerItem> results = storeSystem.search("", null, true, null);
                chatBridge.sendRecommendations(results.stream().limit(5).collect(Collectors.toList()), 
                    "⭐ Top-rated items across all sellers:");
            });
            t.setRepeats(false); t.start();
            return;
        }

        Integer tempMaxPrice = null;
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile(
            "under\\s*(\\d+)k?|<\\s*(\\d+)k?|max\\s*(\\d+)k?|dibawah\\s*(\\d+)k?|budget\\s*(\\d+)k?")
            .matcher(lower);
        if (pm.find()) {
            for (int g = 1; g <= 5; g++) {
                if (pm.group(g) != null) {
                    int val = Integer.parseInt(pm.group(g));
                    tempMaxPrice = lower.contains("k") || val < 500 ? val * 1000 : val;
                    break;
                }
            }
        }
        final Integer maxPrice = tempMaxPrice;

        boolean byRating = lower.contains("rating") || lower.contains("best") || lower.contains("top") 
                        || lower.contains("terbaik");
        boolean bySpeed = lower.contains("fastest") || lower.contains("cepat") || lower.contains("quick") 
                       || lower.contains("tercepat");

        List<SellerItem> results = storeSystem.search(query, maxPrice, byRating ? true : null, bySpeed ? true : null);

        if (!results.isEmpty()) {
            Timer t = new Timer(800, e -> {
                String msg = "Here are" + (maxPrice != null ? " options under Rp " + String.format("%,d", maxPrice) : " some recommendations") + ":";
                chatBridge.sendRecommendations(results, msg);
            });
            t.setRepeats(false); t.start();
        } else {
            // No local results found -> Fallback to AI Auto-Answer Bot
            callGroqAutoAnswer(query);
        }
    }

    // ==========================================
    // AI AUTO-ANSWER BOT (Groq API Placeholder)
    // ==========================================
    private void callGroqAutoAnswer(String query) {
        chatBridge.sendSystem("🤖 AI Assistant is thinking...");
        
        new Thread(() -> {
            try {
                String apiKey = "test_groq_api_key"; // Replace with your actual Groq API key
                String endpoint = "https://api.groq.com/openai/v1/chat/completions";
                
                // Build context from local data
                StringBuilder contextBuilder = new StringBuilder("You are a helpful food ordering assistant for FoodChat. ");
                contextBuilder.append("Here is the real-time menu data for our restaurants:\n");
                for (Seller s : storeSystem.getSellers()) {
                    contextBuilder.append("- ").append(s.getName()).append(" (Rating: ").append(s.getRating()).append("): ");
                    for (MenuItem item : s.getMenu()) {
                        contextBuilder.append(item.getName()).append(" (Rp ").append(item.getPrice()).append("), ");
                    }
                    contextBuilder.append("\n");
                }
                contextBuilder.append("\nUse this data to give specific recommendations. If you suggest something, mention which restaurant it's from.");
                
                String context = contextBuilder.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                String escapedQuery = query.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r");

                String jsonInput = "{" +
                    "\"model\": \"llama-3.1-8b-instant\"," +
                    "\"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"" + context + "\"}," +
                        "{\"role\": \"user\", \"content\": \"" + escapedQuery + "\"}" +
                    "]" +
                "}";

                HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
                    
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    String body = response.body();
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"(,\\s*\"|\\s*})").matcher(body);
                    if (matcher.find()) {
                        String botResponse = matcher.group(1)
                            .replace("\\n", "<br>")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                        
                        SwingUtilities.invokeLater(() -> chatBridge.sendFromBuyerToAll("🤖 [AI Assistant]: " + botResponse));
                    } else {
                        SwingUtilities.invokeLater(() -> chatBridge.sendFromBuyerToAll("🤖 [AI Assistant]: I understood you, but my response got lost. Try again!"));
                    }
                } else {
                    String errorMsg = "Error: " + response.statusCode();
                    if (response.body().contains("api_key")) errorMsg = "Invalid API Key";
                    else if (response.body().contains("model_not_found")) errorMsg = "Model not found";
                    
                    final String finalError = errorMsg;
                    SwingUtilities.invokeLater(() -> chatBridge.sendFromBuyerToAll("🤖 [AI Assistant]: I'm having trouble (" + finalError + "). Please check the API settings!"));
                    System.err.println("Groq API Error: " + response.body());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> chatBridge.sendFromBuyerToAll("🤖 [AI Assistant]: Oops! I lost my connection to the kitchen."));
            }
        }).start();
    }

    private void recommendByCategory(FoodCategory cat) {
        List<Seller> matched = storeSystem.getSellersByCategory(cat);
        
        if (matched.isEmpty()) {
            chatBridge.sendFromBuyerToAll("I couldn't find any " + cat.displayName + " nearby right now.");
        } else {
            StringBuilder sb = new StringBuilder("🔍 Found " + matched.size() + " " + cat.displayName + " options:\n");
            for (Seller s : matched) {
                sb.append("• ").append(s.getName()).append(" (⭐ ").append(s.getRating()).append(")\n");
            }
            sb.append("\nYou can click their name above to see the menu or chat!");
            chatBridge.sendFromBuyerToAll(sb.toString());
            
            // Also suggest all items from these sellers
            List<SellerItem> recs = new ArrayList<>();
            for (Seller s : matched) {
                for (MenuItem item : s.getMenu()) {
                    recs.add(new SellerItem(s, item));
                }
            }
            chatBridge.sendRecommendations(recs, "Here are the full menus for " + cat.displayName + ":");
        }
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            // Only refresh if the message is relevant to current chat selection
            String selected = (String) activeChatSelector.getSelectedItem();
            String targetSellerId = null;
            
            if (selected != null && !selected.equals("💬 General Chat")) {
                for (Seller s : storeSystem.getSellers()) {
                    if (selected.equals(s.getName())) {
                        targetSellerId = s.getId();
                        break;
                    }
                }
            }
            
            boolean shouldRefresh = false;
            if (targetSellerId == null) {
                if (message.targetSellerId == null || message.senderType.equals("BUYER")) {
                    shouldRefresh = true;
                }
            } else {
                if ((message.senderType.equals("BUYER") && targetSellerId.equals(message.targetSellerId)) ||
                    (message.senderType.equals("SELLER") && targetSellerId.equals(message.targetSellerId)) ||
                    (message.senderType.equals("SELLER") && message.targetSellerId == null && 
                     message.senderName != null && message.senderName.equals(selected)) ||
                    (message.senderType.equals("BUYER") && message.targetSellerId == null)) {
                    shouldRefresh = true;
                }
            }
            
            if (shouldRefresh) {
                refreshChatHistory();
            } else if (message.senderType.equals("SELLER") && message.targetSellerId != null) {
                // If we got a private message but it's not the current view, 
                // find the seller and add an indicator
                for (int i = 0; i < activeChatSelector.getItemCount(); i++) {
                    String item = activeChatSelector.getItemAt(i);
                    if (item.contains(message.senderName)) {
                        if (!item.contains("(*)")) {
                            activeChatSelector.removeItemAt(i);
                            activeChatSelector.insertItemAt(item + " (*)", i);
                        }
                        break;
                    }
                }
                
                // Also show a temporary system note in general chat if that's where we are
                if (targetSellerId == null) {
                    addSystemNote(new ChatMessage("System", "SYSTEM", 
                        "🔔 New private message from " + message.senderName + ". Switch chat to view.", MessageType.SYSTEM));
                    scrollToBottom();
                }
            }
            
            if (message.type == MessageType.SYSTEM || message.type == MessageType.ORDER_UPDATE) {
                refreshSellerStatusBar();
            }
            
            if (message.type == MessageType.ORDER_UPDATE && message.order != null) {
                // Add to active orders if not already there
                boolean exists = false;
                for (Order o : activeOrders) {
                    if (o.getOrderId().equals(message.order.getOrderId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    activeOrders.add(message.order);
                    updateChatSelector();
                }
            }
        });
    }

    private void renderMessage(ChatMessage msg) {
        if (msg.type == MessageType.TEXT) {
            addTextBubble(msg);
        } else if (msg.type == MessageType.STORE_RECOMMENDATION) {
            addRecommendationCard(msg);
        } else if (msg.type == MessageType.SPECIAL_OFFER) {
            addOfferCard(msg);
        } else if (msg.type == MessageType.ORDER_UPDATE) {
            addOrderUpdateCard(msg);
        } else if (msg.type == MessageType.SYSTEM) {
            addSystemNote(msg);
        }
    }

    private void addTextBubble(ChatMessage msg) {
        boolean isBuyer = msg.senderType.equals("BUYER");
        JPanel row = new JPanel(new FlowLayout(isBuyer ? FlowLayout.RIGHT : FlowLayout.LEFT, 18, 8));
        row.setBackground(new Color(245, 245, 252));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 6));
        bubble.setBackground(isBuyer ? new Color(33, 150, 243) : Color.WHITE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isBuyer ? new Color(25, 130, 210) : new Color(215, 215, 230)),
            BorderFactory.createEmptyBorder(16, 22, 16, 22)));

        if (!isBuyer) {
            JLabel sender = new JLabel(msg.senderName);
            sender.setFont(new Font("Segoe UI", Font.BOLD, 15));
            sender.setForeground(new Color(33, 150, 243));
            bubble.add(sender, BorderLayout.NORTH);
        }

        JLabel text = new JLabel("<html><div style='width:400px; font-size:16px'>" + msg.message + "</div></html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        text.setForeground(isBuyer ? Color.WHITE : new Color(30, 30, 50));

        JLabel time = new JLabel(msg.getFormattedTime());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(isBuyer ? new Color(200, 230, 255) : Color.GRAY);

        bubble.add(text, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        row.add(bubble);
        row.setMaximumSize(new Dimension(680, bubble.getPreferredSize().height + 26));
        chatContainer.add(row);
    }

    private void addSystemNote(ChatMessage msg) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        row.setBackground(new Color(245, 245, 252));
        JLabel lbl = new JLabel("<html><div style='width:500px; text-align:center'><i style='font-size:13px'>" + msg.message + "</i></div></html>");
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lbl.setForeground(new Color(140, 140, 170));
        row.add(lbl);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatContainer.add(row);
    }

    private void addOrderUpdateCard(ChatMessage msg) {
        Order order = msg.order;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 8));
        row.setBackground(new Color(245, 245, 252));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(new Color(240, 248, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(order.getStatus().color, 2),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel icon = new JLabel(order.getStatus().displayName);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        icon.setForeground(order.getStatus().color);

        JLabel details = new JLabel(String.format("<html><div style='width:400px'><b>Status Update:</b> Order <b>%s</b> is now <b>%s</b>" +
            (order.getStatus() != OrderStatus.COMPLETED ? "<br>⏱ Est. %d minutes" : "<br>✔ Delivered!") +
            "</div></html>", order.getOrderId(), order.getStatus().displayName,
            order.getEstimatedMinutes()));
        details.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        card.add(icon, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        row.add(card);
        row.setMaximumSize(new Dimension(680, 110));
        chatContainer.add(row);
    }

    private void addRecommendationCard(ChatMessage msg) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(245, 245, 252));
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("🏪 " + msg.message);
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setForeground(new Color(40, 40, 70));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(header);
        wrapper.add(Box.createRigidArea(new Dimension(0, 10)));

        for (SellerItem si : msg.sellerItems) {
            wrapper.add(buildItemCard(si));
            wrapper.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height + 30));
        chatContainer.add(wrapper);
    }

    private JPanel buildItemCard(SellerItem si) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 215, 230)),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setMaximumSize(new Dimension(680, 190));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel stripe = new JPanel();
        stripe.setBackground(si.seller.getCategory() == FoodCategory.PADANG ? new Color(255, 152, 0) :
            si.seller.getCategory() == FoodCategory.KOREAN ? new Color(233, 30, 99) :
            si.seller.getCategory() == FoodCategory.FASTFOOD ? new Color(244, 67, 54) :
            si.seller.getCategory() == FoodCategory.HEALTHY ? new Color(76, 175, 80) :
            si.seller.getCategory() == FoodCategory.WARTEG ? new Color(121, 85, 72) :
            si.seller.getCategory() == FoodCategory.DESSERT ? new Color(156, 39, 176) :
            new Color(33, 150, 243));
        stripe.setPreferredSize(new Dimension(6, 0));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel storeLbl = new JLabel(si.seller.getCategoryDisplay() + "  •  " + si.seller.getName());
        storeLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        storeLbl.setForeground(new Color(33, 150, 243));

        JLabel itemLbl = new JLabel(si.item.getName());
        itemLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        itemLbl.setForeground(new Color(22, 22, 38));

        JLabel meta = new JLabel(String.format("Rp %,d  •  ⭐ %.1f  •  ⏱ %d min  •  📏 %.1fkm",
            si.item.getPrice(), si.item.getRating(), si.item.getCookTimeMinutes() + 5,
            si.seller.getDistanceKm()));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        meta.setForeground(Color.GRAY);

        info.add(storeLbl); info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(itemLbl); info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(meta);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("+ Cart");
        addBtn.setBackground(new Color(33, 150, 243));
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> {
            cart.addItem(si, 1);
            refreshCart();
            updateChatSelector();
            JOptionPane.showMessageDialog(this, 
                "✅ Added to cart!\n\nYou can add items from other sellers too!\nYour cart now has items from " + 
                cart.getUniqueSellers().size() + " seller(s).",
                "Added to Cart", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton sellerBtn = new JButton("🏪 Seller");
        sellerBtn.setBackground(new Color(245, 245, 252));
        sellerBtn.setForeground(new Color(30, 30, 120));
        sellerBtn.setOpaque(true);
        sellerBtn.setFocusPainted(false);
        sellerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sellerBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 225)),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        sellerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sellerBtn.addActionListener(e -> openSellerWindow(si.seller));

        JButton mapsBtn = new JButton("\uD83D\uDDFA Maps");
        mapsBtn.setBackground(new Color(33, 150, 243));
        mapsBtn.setForeground(Color.WHITE);
        mapsBtn.setOpaque(true);
        mapsBtn.setBorderPainted(false);
        mapsBtn.setFocusPainted(false);
        mapsBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        mapsBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        mapsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        double _lat = si.seller.getLat();
        double _lng = si.seller.getLng();
        mapsBtn.addActionListener(e -> {
            try {
                String url = "https://www.google.com/maps?q=" + _lat + "," + _lng + "&z=16&t=m";
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Cannot open map: " + ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnPanel.add(addBtn);
        btnPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        btnPanel.add(sellerBtn);
        btnPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        btnPanel.add(mapsBtn);

        card.add(stripe, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }

    private void addOfferCard(ChatMessage msg) {
        SpecialOffer offer = msg.specialOffer;
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(245, 245, 252));
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(new Color(255, 249, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel title = new JLabel("🎁 " + offer.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(new Color(180, 100, 0));

        JLabel desc = new JLabel(offer.getDescription());
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel discount = new JLabel(String.format("🔥 Save %d%% — Rp %,d off!", offer.getDiscountPercent(), offer.getSavings()));
        discount.setFont(new Font("Segoe UI", Font.BOLD, 15));
        discount.setForeground(new Color(220, 80, 30));

        JLabel pricing = new JLabel(String.format("<html><s style='font-size:14px'>Rp %,d</s>  →  <b style='font-size:16px;color:#2e7d32'>Rp %,d</b></html>",
            offer.getOriginalPrice(), offer.getOfferPrice()));
        pricing.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        info.add(title); info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(desc); info.add(Box.createRigidArea(new Dimension(0, 10)));
        info.add(discount); info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(pricing);

        JButton addBtn = new JButton("<html><center>🎁 Add<br>to Cart</center></html>");
        addBtn.setBackground(new Color(255, 152, 0));
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addBtn.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> {
            for (SellerItem si : offer.getItems()) cart.addItem(si, 1);
            refreshCart();
            updateChatSelector();
            JOptionPane.showMessageDialog(this,
                "🎁 " + offer.getTitle() + " added!\nYou save Rp " + String.format("%,d", offer.getSavings()) + "!\n\nYour cart now has items from " + 
                cart.getUniqueSellers().size() + " seller(s).",
                "Deal Added!", JOptionPane.INFORMATION_MESSAGE);
        });

        card.add(info, BorderLayout.CENTER);
        card.add(addBtn, BorderLayout.EAST);
        wrapper.add(card);
        wrapper.setMaximumSize(new Dimension(670, card.getPreferredSize().height + 30));
        chatContainer.add(wrapper);
    }

    private void openSellerWindow(Seller seller) {
        SellerWindow win = seller.getWindow();
        if (win != null && win.isVisible()) {
            win.setState(JFrame.NORMAL);
            win.toFront();
            win.requestFocus();
            return;
        }
        SellerWindow sw = new SellerWindow(seller, chatBridge);
        sw.setSize(750, 750);
        
        // Position relative to buyer window if possible
        Window buyerWin = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (buyerWin != null) {
            sw.setLocation(buyerWin.getX() + buyerWin.getWidth() + 10, buyerWin.getY());
        }
        
        sw.setVisible(true);
    }

    private void showSellerBrowser() {
        JDialog dlg = new JDialog(this, "🏪 Browse Sellers", true);
        dlg.setSize(800, 700);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JLabel hdr = new JLabel("  🏪 All Sellers");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 22));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(22, 22, 38));
        hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        dlg.add(hdr, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 15, 15));
        grid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        grid.setBackground(new Color(245, 245, 252));

        for (Seller s : storeSystem.getSellers()) {
            JPanel card = new JPanel(new BorderLayout(15, 0));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 215, 230)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);

            JLabel cat = new JLabel(s.getCategory().emoji + " " + s.getCategory().displayName);
            cat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cat.setForeground(new Color(120, 120, 160));

            JLabel name = new JLabel(s.getName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 16));

            JLabel meta = new JLabel(String.format("⭐ %.1f  •  📏 %.1fkm  •  ⏱ ~%d min",
                s.getRating(), s.getDistanceKm(), s.getEstimatedWaitTime()));
            meta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            meta.setForeground(Color.GRAY);

            JLabel status = new JLabel(s.isBusy() ? "🔴 Busy" : "🟢 Open");
            status.setFont(new Font("Segoe UI", Font.BOLD, 13));
            status.setForeground(s.isBusy() ? new Color(220, 50, 50) : new Color(50, 180, 50));

            info.add(cat); info.add(name); info.add(meta); info.add(status);

            JButton open = new JButton(s.getWindow() != null && s.getWindow().isVisible() ? "🔍 Focus" : "▶ Open");
            open.setBackground(new Color(33, 150, 243));
            open.setForeground(Color.WHITE);
            open.setOpaque(true);
            open.setBorderPainted(false);
            open.setFocusPainted(false);
            open.setFont(new Font("Segoe UI", Font.BOLD, 13));
            open.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            open.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            open.addActionListener(e -> {
                dlg.dispose();
                openSellerWindow(s);
            });

            card.add(info, BorderLayout.CENTER);
            card.add(open, BorderLayout.EAST);
            grid.add(card);
        }

        dlg.add(new JScrollPane(grid), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void refreshCart() {
        int count = cart.getCount();
        int total = cart.getTotal();
        cartCountLbl.setText("🛒 " + count + " item" + (count != 1 ? "s" : ""));
        cartTotalLbl.setText("Rp " + String.format("%,d", total));

        cartPanel.removeAll();

        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setBackground(new Color(240, 240, 250));
        cartHeader.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel cartTitle = new JLabel("🛒 Your Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JLabel sellerCountLabel = new JLabel(cart.getUniqueSellers().size() + " seller(s)");
        sellerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sellerCountLabel.setForeground(new Color(100, 100, 130));
        
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearBtn.setForeground(new Color(200, 80, 80));
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            if (!cart.isEmpty() && JOptionPane.showConfirmDialog(this,
                "Clear cart?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                cart.clear(); refreshCart(); updateChatSelector();
            }
        });
        
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(cartTitle);
        headerLeft.add(sellerCountLabel);
        
        cartHeader.add(headerLeft, BorderLayout.WEST);
        cartHeader.add(clearBtn, BorderLayout.EAST);
        cartPanel.add(cartHeader, BorderLayout.NORTH);

        if (cart.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setBackground(Color.WHITE);
            JLabel el = new JLabel("<html><center>🛒<br><br>Cart is empty<br><small style='font-size:12px'>Search for food to get started!</small><br><br><small style='font-size:11px'>💡 Tip: You can add items from multiple sellers!</small></center></html>");
            el.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            el.setForeground(new Color(170, 170, 190));
            el.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(el, BorderLayout.CENTER);
            cartPanel.add(empty, BorderLayout.CENTER);
        } else {
            JPanel items = new JPanel();
            items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
            items.setBackground(Color.WHITE);
            items.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            // Group items by seller with visual separators
            Map<Seller, List<CartItem>> groupedItems = cart.getItemsBySeller();
            boolean first = true;
            for (Map.Entry<Seller, List<CartItem>> entry : groupedItems.entrySet()) {
                if (!first) {
                    JSeparator sep = new JSeparator();
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep.setForeground(new Color(200, 200, 220));
                    items.add(sep);
                    items.add(Box.createRigidArea(new Dimension(0, 8)));
                }
                first = false;
                
                JLabel sellerLabel = new JLabel("🏪 " + entry.getKey().getName());
                sellerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                sellerLabel.setForeground(new Color(33, 150, 243));
                sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                items.add(sellerLabel);
                items.add(Box.createRigidArea(new Dimension(0, 6)));
                
                for (CartItem ci : entry.getValue()) {
                    items.add(buildCartRow(ci));
                    items.add(Box.createRigidArea(new Dimension(0, 6)));
                }
                items.add(Box.createRigidArea(new Dimension(0, 4)));
            }

            items.add(Box.createRigidArea(new Dimension(0, 12)));
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            items.add(sep);
            items.add(Box.createRigidArea(new Dimension(0, 12)));

            JPanel totRow = new JPanel(new BorderLayout());
            totRow.setOpaque(false);
            totRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            JLabel tl = new JLabel("TOTAL");
            tl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            JLabel ta = new JLabel("Rp " + String.format("%,d", cart.getTotal()));
            ta.setFont(new Font("Segoe UI", Font.BOLD, 18));
            ta.setForeground(new Color(33, 150, 243));
            totRow.add(tl, BorderLayout.WEST); totRow.add(ta, BorderLayout.EAST);
            items.add(totRow);

            JScrollPane sp = new JScrollPane(items);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            cartPanel.add(sp, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setBackground(Color.WHITE);
            footer.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

            JButton checkout = new JButton("💳 Checkout (" + cart.getUniqueSellers().size() + " seller" + (cart.getUniqueSellers().size() != 1 ? "s" : "") + ")");
            checkout.setBackground(new Color(76, 175, 80));
            checkout.setForeground(Color.WHITE);
            checkout.setOpaque(true);
            checkout.setBorderPainted(false);
            checkout.setFont(new Font("Segoe UI", Font.BOLD, 16));
            checkout.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
            checkout.setFocusPainted(false);
            checkout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            checkout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            checkout.addActionListener(e -> showMultiSellerCheckout());

            footer.add(checkout, BorderLayout.CENTER);
            cartPanel.add(footer, BorderLayout.SOUTH);
        }

        cartPanel.revalidate();
        cartPanel.repaint();
    }

    private JPanel buildCartRow(CartItem ci) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(250, 250, 254));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 225, 240)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(ci.getSellerItem().item.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel store = new JLabel("from " + ci.getSellerItem().seller.getName());
        store.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        store.setForeground(Color.GRAY);

        JLabel price = new JLabel(String.format("Rp %,d × %d", ci.getSellerItem().item.getPrice(), ci.getQuantity()));
        price.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        price.setForeground(Color.GRAY);

        info.add(name); info.add(store); info.add(price);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ctrl.setOpaque(false);

        JButton minus = new JButton("−");
        minus.setPreferredSize(new Dimension(30, 28));
        minus.setFocusPainted(false);
        minus.setFont(new Font("Segoe UI", Font.BOLD, 15));
        minus.addActionListener(e -> {
            cart.updateQty(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId(), ci.getQuantity()-1);
            refreshCart();
            updateChatSelector();
        });

        JLabel qtyLbl = new JLabel(String.valueOf(ci.getQuantity()));
        qtyLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        qtyLbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JButton plus = new JButton("+");
        plus.setPreferredSize(new Dimension(30, 28));
        plus.setFocusPainted(false);
        plus.setFont(new Font("Segoe UI", Font.BOLD, 15));
        plus.addActionListener(e -> {
            cart.updateQty(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId(), ci.getQuantity()+1);
            refreshCart();
            updateChatSelector();
        });

        JButton del = new JButton("🗑");
        del.setPreferredSize(new Dimension(30, 28));
        del.setFocusPainted(false);
        del.setForeground(new Color(200, 80, 80));
        del.addActionListener(e -> {
            cart.removeItem(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId());
            refreshCart();
            updateChatSelector();
        });

        ctrl.add(minus); ctrl.add(qtyLbl); ctrl.add(plus); ctrl.add(del);

        JLabel total = new JLabel("Rp " + String.format("%,d", ci.getTotal()));
        total.setFont(new Font("Segoe UI", Font.BOLD, 14));
        total.setForeground(new Color(33, 150, 243));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(total);
        right.add(Box.createRigidArea(new Dimension(0, 6)));
        right.add(ctrl);

        row.add(info, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void showMultiSellerCheckout() {
        Map<Seller, List<CartItem>> groupedItems = cart.getItemsBySeller();
        if (groupedItems.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "Cart is empty!"); 
            return; 
        }

        JDialog dlg = new JDialog(this, "💳 Multi-Seller Checkout", true);
        dlg.setSize(750, 750);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JLabel hdr = new JLabel("  💳 Checkout - " + groupedItems.size() + " Seller" + (groupedItems.size() != 1 ? "s" : ""));
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 22));
        hdr.setOpaque(true); 
        hdr.setBackground(new Color(22, 22, 38)); 
        hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        dlg.add(hdr, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 6, 10, 6);

        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 15);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Full Name *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField nameF = new JTextField(); nameF.setFont(fieldFont);
        form.add(nameF, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Phone *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField phoneF = new JTextField(); phoneF.setFont(fieldFont);
        form.add(phoneF, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Delivery Address *"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextArea addrA = new JTextArea(3, 25); addrA.setFont(fieldFont); addrA.setLineWrap(true);
        JScrollPane addrS = new JScrollPane(addrA);
        form.add(addrS, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(new JLabel("Notes"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextArea notesA = new JTextArea(2, 25); notesA.setFont(fieldFont); notesA.setLineWrap(true);
        form.add(new JScrollPane(notesA), gbc);

        // Order summary per seller
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(new Color(240, 248, 255));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 255)),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        
        int grandTotal = 0;
        for (Map.Entry<Seller, List<CartItem>> entry : groupedItems.entrySet()) {
            Seller seller = entry.getKey();
            int sellerTotal = entry.getValue().stream().mapToInt(CartItem::getTotal).sum();
            grandTotal += sellerTotal;
            
            JLabel sellerLabel = new JLabel("🏪 " + seller.getName() + " - Rp " + String.format("%,d", sellerTotal));
            sellerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            sellerLabel.setForeground(new Color(33, 150, 243));
            sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            summaryPanel.add(sellerLabel);
            
            StringBuilder itemsText = new StringBuilder();
            for (CartItem ci : entry.getValue()) {
                itemsText.append("  • ").append(ci.getSellerItem().item.getName())
                    .append(" x").append(ci.getQuantity())
                    .append(" = Rp ").append(String.format("%,d", ci.getTotal())).append("\n");
            }
            JLabel itemsLabel = new JLabel("<html>" + itemsText.toString().replace("\n", "<br>") + "</html>");
            itemsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            itemsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            summaryPanel.add(itemsLabel);
            summaryPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        
        JLabel totalLabel = new JLabel("💰 GRAND TOTAL: Rp " + String.format("%,d", grandTotal));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setForeground(new Color(76, 175, 80));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryPanel.add(totalLabel);
        
        form.add(summaryPanel, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        btnRow.setBackground(Color.WHITE);
        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancel.addActionListener(e -> dlg.dispose());
        
        JButton place = new JButton("🚀 Place " + groupedItems.size() + " Order" + (groupedItems.size() != 1 ? "s" : ""));
        place.setBackground(new Color(76, 175, 80));
        place.setForeground(Color.WHITE);
        place.setOpaque(true);
        place.setBorderPainted(false);
        place.setFocusPainted(false);
        place.setFont(new Font("Segoe UI", Font.BOLD, 15));
        place.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        place.addActionListener(e -> {
            String name = nameF.getText().trim();
            String phone = phoneF.getText().trim();
            String addr = addrA.getText().trim();
            if (name.isEmpty() || phone.isEmpty() || addr.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please fill all required fields (*)");
                return;
            }

            // Create separate order for each seller
            List<Order> createdOrders = new ArrayList<>();
            for (Map.Entry<Seller, List<CartItem>> entry : groupedItems.entrySet()) {
                Seller seller = entry.getKey();
                List<CartItem> sellerItems = entry.getValue();
                int sellerTotal = sellerItems.stream().mapToInt(CartItem::getTotal).sum();
                
                Order order = new Order(name, phone, addr, notesA.getText().trim(),
                    sellerItems, sellerTotal, seller);
                createdOrders.add(order);
                
                // Add to active orders list
                activeOrders.add(order);
                
                // Open seller window if not open
                openSellerWindow(seller);
                
                // Send order to seller
                Timer t = new Timer(500, ev -> {
                    if (seller.getWindow() != null) seller.getWindow().receiveOrder(order);
                });
                t.setRepeats(false); 
                t.start();
            }
            
            dlg.dispose();
            cart.clear();
            refreshCart();
            updateChatSelector();
            
            // Build confirmation message
            StringBuilder confirmMsg = new StringBuilder("✅ Orders placed successfully!\n\n");
            for (Order order : createdOrders) {
                confirmMsg.append("📦 ").append(order.getOrderId())
                    .append(" - ").append(order.getSeller().getName())
                    .append(" - Rp ").append(String.format("%,d", order.getSubtotal()))
                    .append("\n");
            }
            confirmMsg.append("\nYou can now chat with each seller individually using the chat selector above!");
            
            JOptionPane.showMessageDialog(this, confirmMsg.toString(), "Orders Confirmed", JOptionPane.INFORMATION_MESSAGE);
            
            chatBridge.sendSystem("🚀 " + createdOrders.size() + " order" + (createdOrders.size() != 1 ? "s" : "") + 
                " placed with " + createdOrders.size() + " seller" + (createdOrders.size() != 1 ? "s" : "") + "!");
        });

        btnRow.add(cancel); 
        btnRow.add(place);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = chatScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

// ===============================
// ORDER HISTORY WINDOW
// ===============================

class OrderHistoryWindow extends JFrame implements OrderHistoryListener {
    private JPanel historyContainer;

    public OrderHistoryWindow() {
        setTitle("📜 Order History");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 46));
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JLabel title = new JLabel("📜 Order History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(OrderHistoryManager.getCompletedOrders().size() + " completed orders");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(title);
        leftPanel.add(subtitle);

        header.add(leftPanel, BorderLayout.WEST);

        historyContainer = new JPanel();
        historyContainer.setLayout(new BoxLayout(historyContainer, BoxLayout.Y_AXIS));
        historyContainer.setBackground(new Color(245, 245, 250));
        historyContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scroll = new JScrollPane(historyContainer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        refreshHistory();
        OrderHistoryManager.addListener(this);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void refreshHistory() {
        historyContainer.removeAll();

        List<Order> orders = OrderHistoryManager.getCompletedOrders();
        if (orders.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            JLabel lbl = new JLabel("<html><center>📭<br><br>No completed orders yet</center></html>");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            lbl.setForeground(Color.GRAY);
            empty.add(lbl);
            historyContainer.add(empty);
        } else {
            List<Order> reversed = new ArrayList<>(orders);
            java.util.Collections.reverse(reversed);
            
            for (Order order : reversed) {
                historyContainer.add(createHistoryCard(order));
                historyContainer.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        historyContainer.revalidate();
        historyContainer.repaint();
    }

    private JPanel createHistoryCard(Order order) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 200), 2),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel orderId = new JLabel("✔️ " + order.getOrderId() + "  •  " + order.getFormattedTime());
        orderId.setFont(new Font("Segoe UI", Font.BOLD, 15));
        orderId.setForeground(new Color(50, 150, 50));

        JLabel seller = new JLabel("🏪 " + order.getSeller().getName());
        seller.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel customer = new JLabel("👤 " + order.getCustomerName() + "  📞 " + order.getPhone());
        customer.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel addr = new JLabel("📍 " + order.getAddress());
        addr.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addr.setForeground(Color.GRAY);

        StringBuilder itemsText = new StringBuilder();
        for (CartItem ci : order.getItems()) {
            itemsText.append(ci.getSellerItem().item.getName())
                .append(" x").append(ci.getQuantity()).append(", ");
        }
        if (itemsText.length() > 2) itemsText.setLength(itemsText.length() - 2);
        JLabel items = new JLabel("🍽️ " + itemsText.toString());
        items.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel total = new JLabel("💰 Rp " + String.format("%,d", order.getSubtotal()));
        total.setFont(new Font("Segoe UI", Font.BOLD, 16));
        total.setForeground(new Color(0, 120, 0));

        info.add(orderId);
        info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(seller);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(customer);
        info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(addr);
        info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(items);
        info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(total);

        JButton mapBtn = new JButton("🗺️ View on Map");
        mapBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mapBtn.setBackground(new Color(33, 150, 243));
        mapBtn.setForeground(Color.WHITE);
        mapBtn.setOpaque(true);
        mapBtn.setBorderPainted(false);
        mapBtn.setFocusPainted(false);
        mapBtn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        mapBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mapBtn.addActionListener(e -> showMapForOrder(order));

        card.add(info, BorderLayout.CENTER);
        card.add(mapBtn, BorderLayout.EAST);

        return card;
    }

    private void showMapForOrder(Order order) {
        new SimpleMapWindow(order);
    }

    @Override
    public void onOrderHistoryChanged() {
        SwingUtilities.invokeLater(this::refreshHistory);
    }
}
}
