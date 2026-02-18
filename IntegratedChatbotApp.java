import java.awt.*;
import java.awt.event.*;
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

            // Open buyer window on the left
            BuyerChatWindow buyer = new BuyerChatWindow(chatBridge, storeSystem);
            buyer.setLocation(0, 0);
            buyer.setSize(780, 900);

            // Auto-open ALL seller windows, tiled on the right side
            List<Seller> sellers = storeSystem.getSellers();
            int sellerW = 670;
            int sellerH = 420;
            int startX = 790;
            int cols = 2;

            for (int i = 0; i < sellers.size(); i++) {
                Seller s = sellers.get(i);
                int col = i % cols;
                int row = i / cols;
                int x = startX + col * (sellerW + 6);
                int y = row * (sellerH + 6);
                SellerWindow sw = new SellerWindow(s, chatBridge);
                sw.setSize(sellerW, sellerH);
                sw.setLocation(x, y);
                sw.setVisible(true);
            }
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
    private int currentQueueCount;
    private boolean isBusy;
    private List<MenuItem> menu;
    private List<SpecialOffer> promotions;
    private SellerWindow window;

    public Seller(String id, String name, FoodCategory category, double rating, double distanceKm) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.distanceKm = distanceKm;
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
        listeners.forEach(l -> l.onStatusChanged(this));
    }

    // Getters
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

    public ChatMessage(String senderName, String senderType, String message, MessageType type) {
        this.senderName = senderName;
        this.senderType = senderType;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
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

    public void addListener(ChatListener l) { listeners.add(l); }
    public void setBuyerName(String n) { buyerName = n; }
    public List<ChatMessage> getHistory() { return new ArrayList<>(history); }

    public void sendFromBuyer(String msg) {
        dispatch(new ChatMessage(buyerName, "BUYER", msg, MessageType.TEXT));
    }
    public void sendFromSeller(String sellerName, String msg) {
        dispatch(new ChatMessage(sellerName, "SELLER", msg, MessageType.TEXT));
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
// SHOPPING CART
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

    public Seller getPrimarySeller() {
        if (items.isEmpty()) return null;
        Map<String, Long> counts = items.stream()
            .collect(Collectors.groupingBy(ci -> ci.getSellerItem().seller.getId(), Collectors.counting()));
        String topId = counts.entrySet().stream()
            .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (topId == null) return null;
        return items.stream()
            .map(ci -> ci.getSellerItem().seller)
            .filter(s -> s.getId().equals(topId))
            .findFirst().orElse(null);
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
        Seller padang = new Seller("S001", "Warung Padang Sederhana", FoodCategory.PADANG, 4.7, 0.3);
        padang.getMenu().add(new MenuItem("S001-1", "Nasi Rendang", 18000, 4.8, 40, "food",
            "spicy", "savory", "beef", "indonesian", "padang", "nasi", "rice"));
        padang.getMenu().add(new MenuItem("S001-2", "Nasi Ayam Bakar", 17000, 4.6, 35, "food",
            "savory", "chicken", "indonesian", "padang", "nasi", "rice", "grilled"));
        padang.getMenu().add(new MenuItem("S001-3", "Gulai Ikan", 15000, 4.5, 30, "food",
            "spicy", "fish", "indonesian", "padang", "soup"));
        padang.getMenu().add(new MenuItem("S001-4", "Sayur Nangka", 8000, 4.3, 20, "food",
            "savory", "vegetables", "indonesian", "padang", "vegetarian", "cheap"));

        // Padang promos
        SellerItem rendang = new SellerItem(padang, padang.getMenu().get(0));
        SellerItem ayam = new SellerItem(padang, padang.getMenu().get(1));
        padang.getPromotions().add(new SpecialOffer("🔥 Padang Combo",
            "Nasi Rendang + Nasi Ayam Bakar", List.of(rendang, ayam), 20));
        sellers.add(padang);

        // ---- KOREAN ----
        Seller korean = new Seller("S002", "Korean Street Food", FoodCategory.KOREAN, 4.6, 1.2);
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
        Seller fast = new Seller("S003", "Burger & Pasta Station", FoodCategory.FASTFOOD, 4.4, 0.9);
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
        Seller healthy = new Seller("S004", "Green Bowl & Salad", FoodCategory.HEALTHY, 4.8, 1.5);
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
        Seller warteg = new Seller("S005", "Warteg Bahagia", FoodCategory.WARTEG, 4.3, 0.2);
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
        Seller dessert = new Seller("S006", "Sweet Dessert House", FoodCategory.DESSERT, 4.9, 1.5);
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
        Seller drinks = new Seller("S007", "Warung Es Teh Indonesia", FoodCategory.DRINKS, 4.7, 0.5);
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

    public SellerWindow(Seller seller, ChatBridge chatBridge) {
        this.seller = seller;
        this.chatBridge = chatBridge;
        seller.setWindow(this);
        chatBridge.addListener(this);  // Listen to all chat messages

        setTitle(seller.getCategoryDisplay() + " — " + seller.getName());
        setSize(750, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(250, 250, 252));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 46));
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel titlePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePane.setOpaque(false);

        JLabel catLabel = new JLabel(seller.getCategory().emoji);
        catLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel nameLabel = new JLabel(seller.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(Color.WHITE);
        JLabel ratingLabel = new JLabel(String.format("⭐ %.1f  •  📏 %.1fkm", seller.getRating(), seller.getDistanceKm()));
        ratingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ratingLabel.setForeground(new Color(180, 180, 200));
        titlePane.add(catLabel); titlePane.add(nameLabel); titlePane.add(ratingLabel);

        JPanel rightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPane.setOpaque(false);

        statusLabel = new JLabel("🟢 Open");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(new Color(100, 220, 100));

        JButton busyBtn = new JButton("⏳ Toggle Busy");
        busyBtn.setBackground(new Color(255, 152, 0));
        busyBtn.setForeground(Color.WHITE);
        busyBtn.setFocusPainted(false);
        busyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        busyBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
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
        promoBtn.setFocusPainted(false);
        promoBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        promoBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        promoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        promoBtn.addActionListener(e -> showPromoDialog());

        rightPane.add(statusLabel); rightPane.add(busyBtn); rightPane.add(promoBtn);
        header.add(titlePane, BorderLayout.WEST);
        header.add(rightPane, BorderLayout.EAST);

        // Menu quick-view
        JPanel menuBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        menuBar.setBackground(new Color(240, 240, 248));
        menuBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 230)),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        JLabel menuTitle = new JLabel("Menu: ");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        menuBar.add(menuTitle);
        for (MenuItem mi : seller.getMenu()) {
            JLabel tag = new JLabel(mi.getName() + " Rp" + String.format("%,d", mi.getPrice()));
            tag.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            tag.setForeground(new Color(70, 70, 100));
            tag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            tag.setOpaque(true); tag.setBackground(Color.WHITE);
            menuBar.add(tag);
        }

        // Orders panel
        ordersContainer = new JPanel();
        ordersContainer.setLayout(new BoxLayout(ordersContainer, BoxLayout.Y_AXIS));
        ordersContainer.setBackground(new Color(248, 248, 252));
        ordersContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel ordersTitle = new JLabel("   📋 Incoming Orders");
        ordersTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        ordersTitle.setForeground(new Color(60, 60, 80));
        ordersTitle.setOpaque(true);
        ordersTitle.setBackground(new Color(248, 248, 252));
        ordersTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        ordersContainer.add(ordersTitle);

        JPanel emptyLabel = makeEmptyOrdersLabel();
        ordersContainer.add(emptyLabel);

        JScrollPane ordersScroll = new JScrollPane(ordersContainer);
        ordersScroll.setBorder(null);
        ordersScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Chat container
        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 250));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(220, 220, 230)));
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel chatTitle = new JLabel("   💬 Customer Chat");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chatTitle.setForeground(new Color(60, 60, 80));
        chatTitle.setOpaque(true);
        chatTitle.setBackground(new Color(240, 240, 250));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        chatContainer.add(chatTitle);

        // Split: orders LEFT (60%), chat RIGHT (40%)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(ordersScroll);
        splitPane.setRightComponent(chatScroll);
        splitPane.setDividerLocation(420);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(menuBar, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);

        // Messenger panel
        JPanel msgPanel = new JPanel(new BorderLayout(8, 0));
        msgPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 230)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        msgPanel.setBackground(Color.WHITE);
        JTextField msgField = new JTextField();
        msgField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        msgField.setToolTipText("Send a message to customer...");
        JButton msgBtn = new JButton("💬 Send");
        msgBtn.setBackground(new Color(33, 150, 243));
        msgBtn.setForeground(Color.WHITE);
        msgBtn.setFocusPainted(false);
        msgBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        msgBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        msgBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ActionListener sendMsg = ae -> {
            String txt = msgField.getText().trim();
            if (!txt.isEmpty()) {
                chatBridge.sendFromSeller(seller.getName(), txt);
                msgField.setText("");
            }
        };
        msgField.addActionListener(sendMsg);
        msgBtn.addActionListener(sendMsg);
        msgPanel.add(msgField, BorderLayout.CENTER);
        msgPanel.add(msgBtn, BorderLayout.EAST);

        // Re-add bottom using a container
        JPanel bottomStack = new JPanel(new BorderLayout());
        bottomStack.add(menuBar, BorderLayout.NORTH);
        bottomStack.add(msgPanel, BorderLayout.SOUTH);
        add(bottomStack, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        // Note: setVisible is called by the launcher after positioning
    }

    private JPanel makeEmptyOrdersLabel() {
        JPanel p = new JPanel();
        p.setBackground(new Color(248, 248, 252));
        p.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));
        JLabel lbl = new JLabel("<html><center>⏳<br><br>Waiting for orders...</center></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(new Color(160, 160, 180));
        p.add(lbl);
        return p;
    }

    public void receiveOrder(Order order) {
        activeOrders.add(order);
        seller.setCurrentQueueCount(activeOrders.size());
        order.addStatusListener(this);

        SwingUtilities.invokeLater(() -> {
            // Remove empty label if first
            if (ordersContainer.getComponentCount() == 2 &&
                ordersContainer.getComponent(1) instanceof JPanel) {
                ordersContainer.remove(1);
            }
            ordersContainer.add(createOrderCard(order));
            ordersContainer.revalidate();
            ordersContainer.repaint();
        });

        chatBridge.sendFromSeller(seller.getName(),
            "✅ Order received! " + seller.getName() + " is preparing your food. Est. " +
            order.getEstimatedMinutes() + " minutes.");
    }

    private JPanel createOrderCard(Order order) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 210, 230), 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel orderId = new JLabel("🆔 " + order.getOrderId() + "  •  " + order.getFormattedTime());
        orderId.setFont(new Font("Segoe UI", Font.BOLD, 12));
        orderId.setForeground(new Color(100, 100, 130));

        JLabel customer = new JLabel("👤 " + order.getCustomerName() + "  📞 " + order.getPhone());
        customer.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel addr = new JLabel("📍 " + order.getAddress());
        addr.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addr.setForeground(Color.GRAY);

        StringBuilder itemsText = new StringBuilder("<html>");
        for (CartItem ci : order.getItems()) {
            itemsText.append("• ").append(ci.getSellerItem().item.getName())
                .append(" x").append(ci.getQuantity())
                .append(" = Rp ").append(String.format("%,d", ci.getTotal())).append("<br>");
        }
        itemsText.append("</html>");
        JLabel itemsLabel = new JLabel(itemsText.toString());
        itemsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel total = new JLabel("💰 Total: Rp " + String.format("%,d", order.getSubtotal()) +
            "   ⏱ Est: " + order.getEstimatedMinutes() + " min");
        total.setFont(new Font("Segoe UI", Font.BOLD, 13));
        total.setForeground(new Color(33, 150, 243));

        JLabel statusLbl = new JLabel(order.getStatus().displayName);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLbl.setForeground(order.getStatus().color);

        info.add(orderId); info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(customer); info.add(Box.createRigidArea(new Dimension(0, 2)));
        info.add(addr); info.add(Box.createRigidArea(new Dimension(0, 6)));
        info.add(itemsLabel); info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(total); info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(statusLbl);

        // Right: status buttons
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        String[][] btnDefs = {
            {"✅ Accept", "33,150,243"},
            {"🔄 On Process", "156,39,176"},
            {"🚚 Driver On Way", "0,188,212"},
            {"✔️ Complete", "76,175,80"},
            {"⏳ Busy", "255,87,34"},
            {"❌ Reject", "244,67,54"}
        };
        OrderStatus[] statuses = {
            OrderStatus.ACCEPTED, OrderStatus.ON_PROCESS,
            OrderStatus.DRIVER_ON_WAY, OrderStatus.COMPLETED,
            OrderStatus.BUSY, OrderStatus.REJECTED
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
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            btn.setMaximumSize(new Dimension(150, 28));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                order.updateStatus(target);
                statusLbl.setText(order.getStatus().displayName);
                statusLbl.setForeground(order.getStatus().color);
                chatBridge.sendOrderUpdate(order);
            });

            buttons.add(btn);
            buttons.add(Box.createRigidArea(new Dimension(0, 4)));
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
        dlg.setSize(500, 350);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(Color.WHITE);

        for (SpecialOffer offer : seller.getPromotions()) {
            JPanel card = new JPanel(new BorderLayout(12, 0));
            card.setBackground(new Color(255, 249, 235));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JLabel info = new JLabel(String.format(
                "<html><b>%s</b><br>%s<br>💰 Rp%,d → Rp%,d (Save %d%%)</html>",
                offer.getTitle(), offer.getDescription(),
                offer.getOriginalPrice(), offer.getOfferPrice(), offer.getDiscountPercent()));
            info.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JButton sendBtn = new JButton("📢 Send");
            sendBtn.setBackground(new Color(255, 152, 0));
            sendBtn.setForeground(Color.WHITE);
            sendBtn.setFocusPainted(false);
            sendBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            sendBtn.addActionListener(e -> {
                chatBridge.sendSpecialOffer(offer);
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Promotion sent to customer!");
            });

            card.add(info, BorderLayout.CENTER);
            card.add(sendBtn, BorderLayout.EAST);
            panel.add(card);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Custom promo
        JPanel customPane = new JPanel(new BorderLayout(8, 0));
        customPane.setBorder(BorderFactory.createTitledBorder("Custom Message"));
        JTextField customField = new JTextField();
        JButton customSend = new JButton("Send");
        customSend.addActionListener(e -> {
            String txt = customField.getText().trim();
            if (!txt.isEmpty()) {
                chatBridge.sendFromSeller(seller.getName(), "🔥 " + txt);
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
        // Handled via chatBridge updates sent from createOrderCard
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        // Only show TEXT messages (filter out recommendations/offers/system for now)
        if (message.type != MessageType.TEXT) return;

        SwingUtilities.invokeLater(() -> {
            addChatBubble(message);
            scrollChatToBottom();
        });
    }

    private void addChatBubble(ChatMessage msg) {
        boolean isBuyer = msg.senderType.equals("BUYER");
        JPanel row = new JPanel(new FlowLayout(isBuyer ? FlowLayout.LEFT : FlowLayout.RIGHT, 10, 4));
        row.setBackground(new Color(245, 245, 250));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 3));
        if (isBuyer) {
            bubble.setBackground(Color.WHITE);
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        } else {
            bubble.setBackground(new Color(230, 245, 230));
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 200, 100)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }

        if (isBuyer) {
            JLabel sender = new JLabel(msg.senderName);
            sender.setFont(new Font("Segoe UI", Font.BOLD, 10));
            sender.setForeground(new Color(100, 100, 150));
            bubble.add(sender, BorderLayout.NORTH);
        }

        JLabel text = new JLabel("<html><div style='max-width:200px'>" + msg.message + "</div></html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        text.setForeground(new Color(30, 30, 50));

        JLabel time = new JLabel(msg.getFormattedTime());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        time.setForeground(Color.GRAY);

        bubble.add(text, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        row.add(bubble);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubble.getPreferredSize().height + 10));

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
// BUYER WINDOW
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

    public BuyerChatWindow(ChatBridge chatBridge, MultiStoreSystem storeSystem) {
        this.chatBridge = chatBridge;
        this.storeSystem = storeSystem;
        this.cart = new ShoppingCart();
        chatBridge.addListener(this);

        setTitle("🍔 FoodChat — Multi-Seller Food Ordering");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(30, 30);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 252));

        // Build a top panel: header + seller status bar stacked
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buildHeader(), BorderLayout.NORTH);
        sellerStatusBar = buildSellerStatusBar();
        topPanel.add(sellerStatusBar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(750);
        split.setResizeWeight(0.68);
        split.setBorder(null);

        // Chat area
        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 252));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        split.setLeftComponent(chatScroll);

        // Cart area
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

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 22, 38));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("🍔");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel title = new JLabel("FoodChat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Multi-Seller Food Ordering");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(150, 150, 180));
        left.add(logo); left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        cartCountLbl = new JLabel("🛒 0 items");
        cartCountLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartCountLbl.setForeground(new Color(180, 180, 200));

        cartTotalLbl = new JLabel("Rp 0");
        cartTotalLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cartTotalLbl.setForeground(new Color(100, 220, 100));

        JButton sellerBtn = new JButton("🏪 Browse Sellers");
        sellerBtn.setBackground(new Color(33, 150, 243));
        sellerBtn.setForeground(Color.WHITE);
        sellerBtn.setFocusPainted(false);
        sellerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sellerBtn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        sellerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sellerBtn.addActionListener(e -> showSellerBrowser());

        right.add(cartCountLbl); right.add(cartTotalLbl); right.add(sellerBtn);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSellerStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        bar.setBackground(new Color(16, 16, 28));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel lbl = new JLabel("SELLERS:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(120, 120, 160));
        bar.add(lbl);

        for (Seller s : storeSystem.getSellers()) {
            Color pillBg = s.isBusy() ? new Color(80, 30, 30) : new Color(20, 60, 30);
            Color pillFg = s.isBusy() ? new Color(255, 120, 120) : new Color(100, 220, 120);
            String dot = s.isBusy() ? "🔴 " : "🟢 ";
            JButton pill = new JButton(dot + s.getCategory().emoji + " " + s.getName());
            pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
            pill.setBackground(pillBg);
            pill.setForeground(pillFg);
            pill.setFocusPainted(false);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(pillFg.darker(), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
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
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(120, 120, 160));
        sellerStatusBar.add(lbl);

        for (Seller s : storeSystem.getSellers()) {
            Color pillBg = s.isBusy() ? new Color(80, 30, 30) : new Color(20, 60, 30);
            Color pillFg = s.isBusy() ? new Color(255, 120, 120) : new Color(100, 220, 120);
            String dot = s.isBusy() ? "🔴 " : "🟢 ";
            JButton pill = new JButton(dot + s.getCategory().emoji + " " + s.getName());
            pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
            pill.setBackground(pillBg);
            pill.setForeground(pillFg);
            pill.setFocusPainted(false);
            pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(pillFg.darker(), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.setToolTipText(s.getName() + " — Est. " + s.getEstimatedWaitTime() + " min");
            pill.addActionListener(e -> openSellerWindow(s));
            sellerStatusBar.add(pill);
        }
        sellerStatusBar.revalidate();
        sellerStatusBar.repaint();
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(210, 210, 230)),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        bar.setBackground(Color.WHITE);

        // Quick filters
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickPanel.setOpaque(false);
        String[] quickBtns = {"🍛 Padang", "🍜 Korean", "🥗 Healthy", "🍚 Warteg", "🎁 Special Offers"};
        String[] quickQueries = {"padang", "korea", "healthy", "warteg", "special"};
        for (int i = 0; i < quickBtns.length; i++) {
            String q = quickQueries[i];
            JButton qb = new JButton(quickBtns[i]);
            qb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            qb.setBackground(new Color(240, 240, 252));
            qb.setForeground(new Color(60, 60, 100));
            qb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 225)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
            qb.setFocusPainted(false);
            qb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            qb.addActionListener(e -> {
                inputField.setText(q);
                sendMessage();
            });
            quickPanel.add(qb);
        }

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setToolTipText("Ask: 'nasi goreng under 20k', 'show korean food', 'fastest food'...");
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("📤 Send");
        sendBtn.setBackground(new Color(33, 150, 243));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
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
        wp.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        wp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emoji = new JLabel("🍔🍜🍛🥗");
        emoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        emoji.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel h = new JLabel("Welcome to FoodChat!");
        h.setFont(new Font("Segoe UI", Font.BOLD, 26));
        h.setForeground(new Color(30, 30, 50));
        h.setAlignmentX(Component.CENTER_ALIGNMENT);

        String tips = "<html><center><p style='font-size:13px;color:#555'>💬 Smart Chat — Try typing:</p>" +
            "<ul style='text-align:left'>" +
            "<li>\"<b>Hello</b>\" — Get started</li>" +
            "<li>\"<b>Help</b>\" — See all food categories</li>" +
            "<li>\"<b>Special offer</b>\" — Today's deals</li>" +
            "<li>\"<b>What's popular?</b>\" — Top-rated items</li>" +
            "<li>\"<b>Nasi goreng under 20k</b>\" — Search with budget</li>" +
            "<li>\"<b>Fastest food</b>\" — Quick delivery</li>" +
            "<li>\"<b>Show Korean food</b>\" — By category</li>" +
            "<li>\"<b>Are sellers open?</b>\" — Check status</li>" +
            "</ul></center></html>";
        JLabel tipsLabel = new JLabel(tips);
        tipsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tipsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        wp.add(emoji); wp.add(Box.createRigidArea(new Dimension(0, 12)));
        wp.add(h); wp.add(Box.createRigidArea(new Dimension(0, 16)));
        wp.add(tipsLabel);

        chatContainer.add(wp);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        chatBridge.sendFromBuyer(msg);
        inputField.setText("");
        processQuery(msg);
    }

    private void processQuery(String query) {
        String lower = query.toLowerCase();

        // ===== AUTO-RESPONSES FOR COMMON QUESTIONS =====
        
        // Greetings
        if (lower.matches("(hi|hello|hey|halo|hai|pagi|siang|malam|selamat.*)[!.?]*")) {
            Timer t = new Timer(600, e -> 
                chatBridge.sendFromSeller("FoodChat AI", 
                    "Hello! 👋 Welcome to FoodChat! What can I help you find today? Try: 'nasi goreng', 'korean food', 'special offer', or 'cheap food'"));
            t.setRepeats(false); t.start();
            return;
        }

        // Help / menu requests
        if (lower.contains("help") || lower.contains("bantuan") || lower.contains("apa aja") || 
            lower.contains("what can") || lower.contains("list menu") || lower.contains("show menu")) {
            Timer t = new Timer(600, e -> 
                chatBridge.sendFromSeller("FoodChat AI", 
                    "I can help you find:\n• 🍛 Padang food\n• 🍜 Korean dishes\n• 🍗 Fast food\n• 🥗 Healthy options\n• 🍚 Warteg/local food\n• 🍰 Desserts\n• 🥤 Drinks\n\nJust tell me what you're craving! Or ask for 'special offer' for deals!"));
            t.setRepeats(false); t.start();
            return;
        }

        // Thank you
        if (lower.matches(".*(thank|terima|makasih).*")) {
            Timer t = new Timer(500, e -> 
                chatBridge.sendFromSeller("FoodChat AI", "You're welcome! 😊 Anything else?"));
            t.setRepeats(false); t.start();
            return;
        }

        // Special offers / promos
        if (lower.contains("special") || lower.contains("offer") || lower.contains("promo")
            || lower.contains("diskon") || lower.contains("discount") || lower.contains("deal")) {
            Timer t = new Timer(800, e -> {
                chatBridge.sendFromSeller("FoodChat AI", "🎁 Here are today's special offers:");
                Timer t2 = new Timer(400, e2 -> {
                    List<SpecialOffer> offers = storeSystem.getAllOffers();
                    for (SpecialOffer offer : offers) chatBridge.sendSpecialOffer(offer);
                });
                t2.setRepeats(false); t2.start();
            });
            t.setRepeats(false); t.start();
            return;
        }

        // Seller status / availability check
        if (lower.contains("open") || lower.contains("available") || lower.contains("buka") || 
            lower.contains("tutup") || lower.contains("busy")) {
            Timer t = new Timer(600, e -> {
                StringBuilder sb = new StringBuilder("📊 Seller Status:\n");
                for (Seller s : storeSystem.getSellers()) {
                    String status = s.isBusy() ? "🔴 Busy (~" + s.getEstimatedWaitTime() + " min wait)" 
                                               : "🟢 Open (~" + s.getEstimatedWaitTime() + " min)";
                    sb.append("• ").append(s.getName()).append(": ").append(status).append("\n");
                }
                chatBridge.sendFromSeller("FoodChat AI", sb.toString());
            });
            t.setRepeats(false); t.start();
            return;
        }

        // Recommendation request (no specific food mentioned)
        if (lower.matches(".*(recommend|suggest|rekomendasi|saranin|what should|apa yang).*") 
            && !lower.contains("food") && query.length() < 50) {
            Timer t = new Timer(700, e -> {
                chatBridge.sendFromSeller("FoodChat AI", 
                    "🤔 What are you in the mood for?\n• Spicy (pedas)\n• Sweet (manis)\n• Healthy (sehat)\n• Fast/Quick (cepat)\n• Cheap (murah)\n\nOr tell me a category: Korean, Padang, Warteg, etc.");
            });
            t.setRepeats(false); t.start();
            return;
        }

        // Popular items shortcut
        if (lower.matches(".*(popular|favorit|favorite|best seller|terlaris).*")) {
            Timer t = new Timer(700, e -> {
                List<SellerItem> results = storeSystem.search("", null, true, null);
                chatBridge.sendRecommendations(results.stream().limit(5).collect(Collectors.toList()), 
                    "⭐ Top-rated items across all sellers:");
            });
            t.setRepeats(false); t.start();
            return;
        }

        // ===== SEARCH WITH FILTERS =====

        // Parse price constraint
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
        final Integer maxPrice = tempMaxPrice;  // Make it final for lambda usage

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
            // No results - give helpful suggestions
            Timer t = new Timer(700, e -> {
                String suggestion;
                if (maxPrice != null && maxPrice < 10000) {
                    suggestion = "Hmm, not much under Rp " + String.format("%,d", maxPrice) + 
                        ". Try 'cheap food' or increase your budget to 15k-20k!";
                } else {
                    suggestion = "I couldn't find that. Try:\n• Specific foods: 'nasi goreng', 'burger', 'salad'\n" +
                        "• Categories: 'korean', 'padang', 'healthy'\n• Taste: 'spicy', 'sweet', 'savory'\n" +
                        "• Or just ask: 'what's popular?'";
                }
                chatBridge.sendFromSeller("FoodChat AI", suggestion);
            });
            t.setRepeats(false); t.start();
        }
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            renderMessage(message);
            scrollToBottom();
            // Refresh seller status pills if busy status may have changed
            if (message.type == MessageType.SYSTEM || message.type == MessageType.ORDER_UPDATE) {
                refreshSellerStatusBar();
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
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void addTextBubble(ChatMessage msg) {
        boolean isBuyer = msg.senderType.equals("BUYER");
        JPanel row = new JPanel(new FlowLayout(isBuyer ? FlowLayout.RIGHT : FlowLayout.LEFT, 14, 6));
        row.setBackground(new Color(245, 245, 252));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 4));
        bubble.setBackground(isBuyer ? new Color(33, 150, 243) : Color.WHITE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isBuyer ? new Color(25, 130, 210) : new Color(215, 215, 230)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        if (!isBuyer) {
            JLabel sender = new JLabel(msg.senderName);
            sender.setFont(new Font("Segoe UI", Font.BOLD, 11));
            sender.setForeground(new Color(33, 150, 243));
            bubble.add(sender, BorderLayout.NORTH);
        }

        JLabel text = new JLabel("<html><div style='max-width:400px'>" + msg.message + "</div></html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(isBuyer ? Color.WHITE : new Color(30, 30, 50));

        JLabel time = new JLabel(msg.getFormattedTime());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        time.setForeground(isBuyer ? new Color(200, 230, 255) : Color.GRAY);

        bubble.add(text, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        row.add(bubble);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubble.getPreferredSize().height + 16));
        chatContainer.add(row);
    }

    private void addSystemNote(ChatMessage msg) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        row.setBackground(new Color(245, 245, 252));
        JLabel lbl = new JLabel("<html><i>" + msg.message + "</i></html>");
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lbl.setForeground(new Color(140, 140, 170));
        row.add(lbl);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatContainer.add(row);
    }

    private void addOrderUpdateCard(ChatMessage msg) {
        Order order = msg.order;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        row.setBackground(new Color(245, 245, 252));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(new Color(240, 248, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(order.getStatus().color, 2),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel icon = new JLabel(order.getStatus().displayName);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        icon.setForeground(order.getStatus().color);

        JLabel details = new JLabel(String.format("<html>Order <b>%s</b> from <b>%s</b>" +
            (order.getStatus() != OrderStatus.COMPLETED ? "<br>⏱ Est. %d minutes" : "<br>✔ Delivered!") +
            "</html>", order.getOrderId(), order.getSeller().getName(),
            order.getEstimatedMinutes()));
        details.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        card.add(icon, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        row.add(card);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        chatContainer.add(row);
    }

    private void addRecommendationCard(ChatMessage msg) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(245, 245, 252));
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("🏪 " + msg.message);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(40, 40, 70));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(header);
        wrapper.add(Box.createRigidArea(new Dimension(0, 8)));

        for (SellerItem si : msg.sellerItems) {
            wrapper.add(buildItemCard(si));
            wrapper.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height + 20));
        chatContainer.add(wrapper);
    }

    private JPanel buildItemCard(SellerItem si) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 215, 230)),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Category color stripe
        JPanel stripe = new JPanel();
        stripe.setBackground(si.seller.getCategory() == FoodCategory.PADANG ? new Color(255, 152, 0) :
            si.seller.getCategory() == FoodCategory.KOREAN ? new Color(233, 30, 99) :
            si.seller.getCategory() == FoodCategory.FASTFOOD ? new Color(244, 67, 54) :
            si.seller.getCategory() == FoodCategory.HEALTHY ? new Color(76, 175, 80) :
            si.seller.getCategory() == FoodCategory.WARTEG ? new Color(121, 85, 72) :
            si.seller.getCategory() == FoodCategory.DESSERT ? new Color(156, 39, 176) :
            new Color(33, 150, 243));
        stripe.setPreferredSize(new Dimension(4, 0));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel storeLbl = new JLabel(si.seller.getCategoryDisplay() + "  •  " + si.seller.getName());
        storeLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        storeLbl.setForeground(new Color(33, 150, 243));

        JLabel itemLbl = new JLabel(si.item.getName());
        itemLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        itemLbl.setForeground(new Color(22, 22, 38));

        JLabel meta = new JLabel(String.format("Rp %,d  •  ⭐ %.1f  •  ⏱ %d min  •  📏 %.1fkm",
            si.item.getPrice(), si.item.getRating(), si.item.getCookTimeMinutes() + 5,
            si.seller.getDistanceKm()));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        meta.setForeground(Color.GRAY);

        info.add(storeLbl); info.add(Box.createRigidArea(new Dimension(0, 3)));
        info.add(itemLbl); info.add(Box.createRigidArea(new Dimension(0, 3)));
        info.add(meta);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("+ Cart");
        addBtn.setBackground(new Color(33, 150, 243));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> {
            cart.addItem(si, 1);
            refreshCart();
        });

        JButton sellerBtn = new JButton("🏪 Seller");
        sellerBtn.setBackground(new Color(245, 245, 252));
        sellerBtn.setForeground(new Color(60, 60, 100));
        sellerBtn.setFocusPainted(false);
        sellerBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sellerBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 225)),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        sellerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sellerBtn.addActionListener(e -> openSellerWindow(si.seller));

        btnPanel.add(addBtn);
        btnPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        btnPanel.add(sellerBtn);

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
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(new Color(255, 249, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel title = new JLabel("🎁 " + offer.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(180, 100, 0));

        JLabel desc = new JLabel(offer.getDescription());
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel discount = new JLabel(String.format("🔥 Save %d%% — Rp %,d off!", offer.getDiscountPercent(), offer.getSavings()));
        discount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        discount.setForeground(new Color(220, 80, 30));

        JLabel pricing = new JLabel(String.format("<html><s>Rp %,d</s>  →  <b style='color:#2e7d32'>Rp %,d</b></html>",
            offer.getOriginalPrice(), offer.getOfferPrice()));
        pricing.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        info.add(title); info.add(Box.createRigidArea(new Dimension(0, 4)));
        info.add(desc); info.add(Box.createRigidArea(new Dimension(0, 8)));
        info.add(discount); info.add(Box.createRigidArea(new Dimension(0, 3)));
        info.add(pricing);

        JButton addBtn = new JButton("<html><center>🎁 Add<br>to Cart</center></html>");
        addBtn.setBackground(new Color(255, 152, 0));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> {
            for (SellerItem si : offer.getItems()) cart.addItem(si, 1);
            refreshCart();
            JOptionPane.showMessageDialog(this,
                "🎁 " + offer.getTitle() + " added!\nYou save Rp " + String.format("%,d", offer.getSavings()) + "!",
                "Deal Added!", JOptionPane.INFORMATION_MESSAGE);
        });

        card.add(info, BorderLayout.CENTER);
        card.add(addBtn, BorderLayout.EAST);
        wrapper.add(card);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 20));
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
        sw.setSize(670, 420);
        sw.setVisible(true);
    }

    private void showSellerBrowser() {
        JDialog dlg = new JDialog(this, "🏪 Browse Sellers", true);
        dlg.setSize(700, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JLabel hdr = new JLabel("  🏪 All Sellers");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 18));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(22, 22, 38));
        hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        dlg.add(hdr, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10));
        grid.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        grid.setBackground(new Color(245, 245, 252));

        for (Seller s : storeSystem.getSellers()) {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 215, 230)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setOpaque(false);

            JLabel cat = new JLabel(s.getCategory().emoji + " " + s.getCategory().displayName);
            cat.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            cat.setForeground(new Color(120, 120, 160));

            JLabel name = new JLabel(s.getName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 13));

            JLabel meta = new JLabel(String.format("⭐ %.1f  •  📏 %.1fkm  •  ⏱ ~%d min",
                s.getRating(), s.getDistanceKm(), s.getEstimatedWaitTime()));
            meta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            meta.setForeground(Color.GRAY);

            JLabel status = new JLabel(s.isBusy() ? "🔴 Busy" : "🟢 Open");
            status.setFont(new Font("Segoe UI", Font.BOLD, 11));
            status.setForeground(s.isBusy() ? new Color(220, 50, 50) : new Color(50, 180, 50));

            info.add(cat); info.add(name); info.add(meta); info.add(status);

            JButton open = new JButton(s.getWindow() != null && s.getWindow().isVisible() ? "🔍 Focus" : "▶ Open");
            open.setBackground(new Color(33, 150, 243));
            open.setForeground(Color.WHITE);
            open.setFocusPainted(false);
            open.setFont(new Font("Segoe UI", Font.BOLD, 11));
            open.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
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

        // Header
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setBackground(new Color(240, 240, 250));
        cartHeader.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel cartTitle = new JLabel("🛒 Your Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.setForeground(new Color(200, 80, 80));
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            if (!cart.isEmpty() && JOptionPane.showConfirmDialog(this,
                "Clear cart?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                cart.clear(); refreshCart();
            }
        });
        cartHeader.add(cartTitle, BorderLayout.WEST);
        cartHeader.add(clearBtn, BorderLayout.EAST);
        cartPanel.add(cartHeader, BorderLayout.NORTH);

        if (cart.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setBackground(Color.WHITE);
            JLabel el = new JLabel("<html><center>🛒<br><br>Cart is empty<br><small>Search for food to get started!</small></center></html>");
            el.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            el.setForeground(new Color(170, 170, 190));
            el.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(el, BorderLayout.CENTER);
            cartPanel.add(empty, BorderLayout.CENTER);
        } else {
            JPanel items = new JPanel();
            items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
            items.setBackground(Color.WHITE);
            items.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            for (CartItem ci : cart.getItems()) {
                items.add(buildCartRow(ci));
                items.add(Box.createRigidArea(new Dimension(0, 6)));
            }

            items.add(Box.createRigidArea(new Dimension(0, 8)));
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            items.add(sep);
            items.add(Box.createRigidArea(new Dimension(0, 8)));

            JPanel totRow = new JPanel(new BorderLayout());
            totRow.setOpaque(false);
            totRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel tl = new JLabel("TOTAL");
            tl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel ta = new JLabel("Rp " + String.format("%,d", cart.getTotal()));
            ta.setFont(new Font("Segoe UI", Font.BOLD, 14));
            ta.setForeground(new Color(33, 150, 243));
            totRow.add(tl, BorderLayout.WEST); totRow.add(ta, BorderLayout.EAST);
            items.add(totRow);

            JScrollPane sp = new JScrollPane(items);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(12);
            cartPanel.add(sp, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setBackground(Color.WHITE);
            footer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton checkout = new JButton("💳 Checkout");
            checkout.setBackground(new Color(76, 175, 80));
            checkout.setForeground(Color.WHITE);
            checkout.setFont(new Font("Segoe UI", Font.BOLD, 14));
            checkout.setBorder(BorderFactory.createEmptyBorder(13, 0, 13, 0));
            checkout.setFocusPainted(false);
            checkout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            checkout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            checkout.addActionListener(e -> showCheckout());

            footer.add(checkout, BorderLayout.CENTER);
            cartPanel.add(footer, BorderLayout.SOUTH);
        }

        cartPanel.revalidate();
        cartPanel.repaint();
    }

    private JPanel buildCartRow(CartItem ci) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(new Color(250, 250, 254));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 225, 240)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(ci.getSellerItem().item.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel store = new JLabel("from " + ci.getSellerItem().seller.getName());
        store.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        store.setForeground(Color.GRAY);

        JLabel price = new JLabel(String.format("Rp %,d × %d", ci.getSellerItem().item.getPrice(), ci.getQuantity()));
        price.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        price.setForeground(Color.GRAY);

        info.add(name); info.add(store); info.add(price);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        ctrl.setOpaque(false);

        JButton minus = new JButton("−");
        minus.setPreferredSize(new Dimension(26, 24));
        minus.setFocusPainted(false);
        minus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        minus.addActionListener(e -> {
            cart.updateQty(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId(), ci.getQuantity()-1);
            refreshCart();
        });

        JLabel qtyLbl = new JLabel(String.valueOf(ci.getQuantity()));
        qtyLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        qtyLbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        JButton plus = new JButton("+");
        plus.setPreferredSize(new Dimension(26, 24));
        plus.setFocusPainted(false);
        plus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        plus.addActionListener(e -> {
            cart.updateQty(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId(), ci.getQuantity()+1);
            refreshCart();
        });

        JButton del = new JButton("🗑");
        del.setPreferredSize(new Dimension(26, 24));
        del.setFocusPainted(false);
        del.setForeground(new Color(200, 80, 80));
        del.addActionListener(e -> {
            cart.removeItem(ci.getSellerItem().item.getId(), ci.getSellerItem().seller.getId());
            refreshCart();
        });

        ctrl.add(minus); ctrl.add(qtyLbl); ctrl.add(plus); ctrl.add(del);

        JLabel total = new JLabel("Rp " + String.format("%,d", ci.getTotal()));
        total.setFont(new Font("Segoe UI", Font.BOLD, 12));
        total.setForeground(new Color(33, 150, 243));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(total);
        right.add(Box.createRigidArea(new Dimension(0, 4)));
        right.add(ctrl);

        row.add(info, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void showCheckout() {
        Seller seller = cart.getPrimarySeller();
        if (seller == null) { JOptionPane.showMessageDialog(this, "Cart is empty!"); return; }

        JDialog dlg = new JDialog(this, "💳 Checkout", true);
        dlg.setSize(560, 550);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JLabel hdr = new JLabel("  💳 Checkout");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 18));
        hdr.setOpaque(true); hdr.setBackground(new Color(22, 22, 38)); hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        dlg.add(hdr, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(8, 4, 8, 4);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

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

        // Order summary
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel summary = new JPanel(new BorderLayout());
        summary.setBackground(new Color(240, 248, 255));
        summary.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 255)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel sumLbl = new JLabel(String.format(
            "<html><b>Order to: %s</b><br>%s<br>Est. delivery: ~%d min<br><b>Total: Rp %,d</b></html>",
            seller.getName(), seller.getCategoryDisplay(), seller.getEstimatedWaitTime(), cart.getTotal()));
        sumLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summary.add(sumLbl, BorderLayout.CENTER);
        form.add(summary, gbc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(Color.WHITE);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dlg.dispose());
        JButton place = new JButton("🚀 Place Order");
        place.setBackground(new Color(76, 175, 80));
        place.setForeground(Color.WHITE);
        place.setFocusPainted(false);
        place.setFont(new Font("Segoe UI", Font.BOLD, 13));
        place.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        place.addActionListener(e -> {
            String name = nameF.getText().trim();
            String phone = phoneF.getText().trim();
            String addr = addrA.getText().trim();
            if (name.isEmpty() || phone.isEmpty() || addr.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please fill all required fields (*)");
                return;
            }

            // Create order and send to seller
            Order order = new Order(name, phone, addr, notesA.getText().trim(),
                cart.getItems(), cart.getTotal(), seller);

            // Open seller window if not open
            openSellerWindow(seller);

            Timer t = new Timer(500, ev -> {
                if (seller.getWindow() != null) seller.getWindow().receiveOrder(order);
            });
            t.setRepeats(false); t.start();

            dlg.dispose();
            cart.clear();
            refreshCart();

            chatBridge.sendSystem("🚀 Order " + order.getOrderId() + " placed with " + seller.getName() +
                "! Est. " + order.getEstimatedMinutes() + " minutes.");
        });

        btnRow.add(cancel); btnRow.add(place);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = chatScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }
}
