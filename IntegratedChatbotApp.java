import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.Timer;

public class IntegratedChatbotApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatBridge chatBridge = new ChatBridge();
            new SellerWindow(chatBridge);
            new BuyerChatWindow(chatBridge);
        });
    }
}

// =============================== 
// CHAT BRIDGE - Real-time messaging
// =============================== 

class ChatBridge {
    private List<ChatMessage> messageHistory;
    private List<ChatListener> listeners;
    private String currentBuyerName = "Customer";
    
    public ChatBridge() {
        messageHistory = new ArrayList<>();
        listeners = new ArrayList<>();
    }
    
    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }
    
    public void sendMessageFromBuyer(String message) {
        ChatMessage msg = new ChatMessage(currentBuyerName, "BUYER", message, MessageType.TEXT);
        messageHistory.add(msg);
        notifyListeners(msg);
    }
    
    public void sendMessageFromSeller(String message) {
        ChatMessage msg = new ChatMessage("Seller", "SELLER", message, MessageType.TEXT);
        messageHistory.add(msg);
        notifyListeners(msg);
    }
    
    public void sendStoreRecommendation(List<StoreItem> items, String message) {
        ChatMessage msg = new ChatMessage("Seller", "SELLER", message, MessageType.STORE_RECOMMENDATION);
        msg.setStoreItems(items);
        messageHistory.add(msg);
        notifyListeners(msg);
    }
    
    public void sendSpecialOffer(SpecialOffer offer) {
        ChatMessage msg = new ChatMessage("Seller", "SELLER", "🎁 Special Offer for you!", MessageType.SPECIAL_OFFER);
        msg.setSpecialOffer(offer);
        messageHistory.add(msg);
        notifyListeners(msg);
    }
    
    private void notifyListeners(ChatMessage message) {
        for (ChatListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }
    
    public void setBuyerName(String name) {
        this.currentBuyerName = name;
    }
    
    public List<ChatMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
}

interface ChatListener {
    void onMessageReceived(ChatMessage message);
}

enum MessageType {
    TEXT,
    STORE_RECOMMENDATION,
    SPECIAL_OFFER,
    SYSTEM
}

class ChatMessage {
    private String senderName;
    private String senderType; // "BUYER" or "SELLER"
    private String message;
    private MessageType type;
    private LocalDateTime timestamp;
    private List<StoreItem> storeItems;
    private SpecialOffer specialOffer;
    private boolean isRead;
    
    public ChatMessage(String senderName, String senderType, String message, MessageType type) {
        this.senderName = senderName;
        this.senderType = senderType;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }
    
    public String getSenderName() { return senderName; }
    public String getSenderType() { return senderType; }
    public String getMessage() { return message; }
    public MessageType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<StoreItem> getStoreItems() { return storeItems; }
    public SpecialOffer getSpecialOffer() { return specialOffer; }
    public boolean isRead() { return isRead; }
    
    public void setStoreItems(List<StoreItem> items) { this.storeItems = items; }
    public void setSpecialOffer(SpecialOffer offer) { this.specialOffer = offer; }
    public void setRead(boolean read) { this.isRead = read; }
    
    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
    }
}

// =============================== 
// DATA MODELS
// =============================== 

class Store {
    private String id;
    private String name;
    private String description;
    private double rating;
    private double distanceKm;
    
    public Store(String id, String name, String description, double rating, double distanceKm) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rating = rating;
        this.distanceKm = distanceKm;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
    public double getDistanceKm() { return distanceKm; }
}

class StoreItem {
    private Store store;
    private MenuItem menuItem;
    
    public StoreItem(Store store, MenuItem menuItem) {
        this.store = store;
        this.menuItem = menuItem;
    }
    
    public Store getStore() { return store; }
    public MenuItem getMenuItem() { return menuItem; }
}

class SpecialOffer {
    private String title;
    private String description;
    private List<StoreItem> items;
    private int discountPercent;
    private int originalPrice;
    private int offerPrice;
    
    public SpecialOffer(String title, String description, List<StoreItem> items, int discountPercent) {
        this.title = title;
        this.description = description;
        this.items = items;
        this.discountPercent = discountPercent;
        this.originalPrice = items.stream().mapToInt(i -> i.getMenuItem().getPrice()).sum();
        this.offerPrice = (int)(originalPrice * (1 - discountPercent / 100.0));
    }
    
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<StoreItem> getItems() { return items; }
    public int getDiscountPercent() { return discountPercent; }
    public int getOriginalPrice() { return originalPrice; }
    public int getOfferPrice() { return offerPrice; }
    public int getSavings() { return originalPrice - offerPrice; }
}

class MenuItem {
    private String id;
    private String name;
    private int price;
    private Set<String> tags;
    private String category;

    public MenuItem(String id, String name, int price, String category, String... tags) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.tags = new HashSet<>(Arrays.asList(tags));
    }

    public int getMatchScore(Set<String> queryTags) {
        int score = 0;
        for (String tag : queryTags) {
            if (this.tags.contains(tag)) {
                score += 10;
            }
        }
        return score;
    }

    public boolean hasAnyTag(Set<String> queryTags) {
        for (String tag : queryTags) {
            if (this.tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getCategory() { return category; }
    public Set<String> getTags() { return tags; }
}

class CartItem {
    private StoreItem storeItem;
    private int quantity;
    private int itemTotal;

    public CartItem(StoreItem item, int quantity) {
        this.storeItem = item;
        this.quantity = quantity;
        this.itemTotal = item.getMenuItem().getPrice() * quantity;
    }

    public StoreItem getStoreItem() { return storeItem; }
    public int getQuantity() { return quantity; }
    public int getItemTotal() { return itemTotal; }
    
    public void setQuantity(int qty) {
        this.quantity = qty;
        this.itemTotal = storeItem.getMenuItem().getPrice() * qty;
    }
}

class Order {
    private String orderId;
    private String customerName;
    private String phoneNumber;
    private String deliveryAddress;
    private String specialNotes;
    private List<CartItem> items;
    private int subtotal;
    private int total;
    private LocalDateTime orderTime;
    private OrderStatus status;
    
    public Order(String customerName, String phone, String address, String notes,
                 List<CartItem> items, int subtotal, int total) {
        this.orderId = generateOrderId();
        this.customerName = customerName;
        this.phoneNumber = phone;
        this.deliveryAddress = address;
        this.specialNotes = notes;
        this.items = new ArrayList<>(items);
        this.subtotal = subtotal;
        this.total = total;
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
    
    private String generateOrderId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "ORD-" + LocalDateTime.now().format(formatter);
    }
    
    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getSpecialNotes() { return specialNotes; }
    public List<CartItem> getItems() { return items; }
    public int getSubtotal() { return subtotal; }
    public int getTotal() { return total; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return orderTime.format(formatter);
    }
}

enum OrderStatus {
    PENDING("Pending", new Color(255, 200, 100)),
    CONFIRMED("Confirmed", new Color(100, 150, 250)),
    PREPARING("Preparing", new Color(150, 100, 250)),
    READY("Ready", new Color(100, 200, 100)),
    COMPLETED("Completed", new Color(100, 200, 100)),
    REJECTED("Rejected", new Color(200, 100, 100));
    
    private String displayName;
    private Color color;
    
    OrderStatus(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public Color getColor() { return color; }
}

// =============================== 
// SHOPPING CART SYSTEM
// =============================== 

class ShoppingCart {
    private List<CartItem> items;
    
    public ShoppingCart() {
        this.items = new ArrayList<>();
    }
    
    public void addItem(StoreItem item, int quantity) {
        for (CartItem cartItem : items) {
            if (cartItem.getStoreItem().getMenuItem().getId().equals(item.getMenuItem().getId()) &&
                cartItem.getStoreItem().getStore().getId().equals(item.getStore().getId())) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartItem(item, quantity));
    }
    
    public void removeItem(String itemId, String storeId) {
        items.removeIf(item -> 
            item.getStoreItem().getMenuItem().getId().equals(itemId) &&
            item.getStoreItem().getStore().getId().equals(storeId));
    }
    
    public void updateQuantity(String itemId, String storeId, int newQty) {
        if (newQty <= 0) {
            removeItem(itemId, storeId);
            return;
        }
        
        for (CartItem item : items) {
            if (item.getStoreItem().getMenuItem().getId().equals(itemId) &&
                item.getStoreItem().getStore().getId().equals(storeId)) {
                item.setQuantity(newQty);
                break;
            }
        }
    }
    
    public int getSubtotal() {
        return items.stream().mapToInt(CartItem::getItemTotal).sum();
    }
    
    public int getTotal() {
        return getSubtotal();
    }
    
    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
    
    public void clear() {
        items.clear();
    }
    
    public Order checkout(String customerName, String phone, String address, String notes) {
        return new Order(customerName, phone, address, notes, 
                        items, getSubtotal(), getTotal());
    }
}

// =============================== 
// MULTI-STORE SYSTEM
// =============================== 

class MultiStoreSystem {
    private List<Store> stores;
    private Map<String, List<MenuItem>> storeMenus;
    private Map<String, String> synonyms;

    public MultiStoreSystem() {
        stores = new ArrayList<>();
        storeMenus = new HashMap<>();
        synonyms = new HashMap<>();
        initializeStores();
        initializeMenus();
        initializeSynonyms();
    }

    private void initializeStores() {
        stores.add(new Store("STORE001", "Warung Es Teh Indonesia", 
            "Spesialis minuman segar tradisional", 4.7, 0.5));
        stores.add(new Store("STORE002", "Es Kepal Milo Corner", 
            "Minuman coklat legendaris ala Thailand", 4.8, 0.8));
        stores.add(new Store("STORE003", "Korean Street Food", 
            "Makanan Korea autentik & modern", 4.6, 1.2));
        stores.add(new Store("STORE004", "Warung Nasi Padang Sederhana", 
            "Masakan Padang dengan cita rasa rumahan", 4.5, 0.3));
        stores.add(new Store("STORE005", "Sweet Dessert House", 
            "Aneka dessert & kue manis", 4.9, 1.5));
        stores.add(new Store("STORE006", "Burger & Pasta Station", 
            "Western food dengan harga terjangkau", 4.4, 0.9));
    }

    private void initializeMenus() {
        // Warung Es Teh Indonesia
        List<MenuItem> warungEsTeh = new ArrayList<>();
        warungEsTeh.add(new MenuItem("S1I001", "Es Teh Manis", 5000, "drink", 
            "sweet", "ice", "cold", "tea", "indonesian", "refreshing"));
        warungEsTeh.add(new MenuItem("S1I002", "Es Teh Tawar", 3000, "drink", 
            "ice", "cold", "tea", "indonesian", "healthy"));
        warungEsTeh.add(new MenuItem("S1I003", "Es Jeruk", 8000, "drink", 
            "sweet", "sour", "ice", "cold", "fresh", "citrus", "indonesian"));
        warungEsTeh.add(new MenuItem("S1I004", "Es Kelapa Muda", 12000, "drink", 
            "sweet", "ice", "cold", "coconut", "fresh", "indonesian"));
        storeMenus.put("STORE001", warungEsTeh);
        
        // Es Kepal Milo Corner
        List<MenuItem> esKepalMilo = new ArrayList<>();
        esKepalMilo.add(new MenuItem("S2I001", "Es Kepal Milo Original", 15000, "drink", 
            "sweet", "ice", "cold", "chocolate", "milo", "creamy"));
        esKepalMilo.add(new MenuItem("S2I002", "Es Kepal Milo Oreo", 18000, "drink", 
            "sweet", "ice", "cold", "chocolate", "milo", "oreo", "creamy"));
        esKepalMilo.add(new MenuItem("S2I003", "Es Kepal Milo Matcha", 20000, "drink", 
            "sweet", "ice", "cold", "chocolate", "milo", "matcha", "creamy"));
        esKepalMilo.add(new MenuItem("S2I004", "Thai Tea", 12000, "drink", 
            "sweet", "ice", "cold", "tea", "milk", "creamy"));
        storeMenus.put("STORE002", esKepalMilo);
        
        // Korean Street Food
        List<MenuItem> koreanFood = new ArrayList<>();
        koreanFood.add(new MenuItem("S3I001", "Tteokbokki", 25000, "food", 
            "sweet", "spicy", "korean", "rice cake", "street food"));
        koreanFood.add(new MenuItem("S3I002", "Korean Fried Chicken", 30000, "food", 
            "sweet", "spicy", "korean", "chicken", "crispy", "fried"));
        koreanFood.add(new MenuItem("S3I003", "Kimchi Fried Rice", 22000, "food", 
            "spicy", "korean", "rice", "kimchi", "savory"));
        koreanFood.add(new MenuItem("S3I004", "Bibimbap", 28000, "food", 
            "savory", "korean", "rice", "vegetables", "egg", "healthy"));
        koreanFood.add(new MenuItem("S3I005", "Ramyeon", 20000, "food", 
            "spicy", "korean", "noodle", "soup", "hot"));
        storeMenus.put("STORE003", koreanFood);
        
        // Warung Nasi Padang
        List<MenuItem> nasiPadang = new ArrayList<>();
        nasiPadang.add(new MenuItem("S4I001", "Beef Rendang", 25000, "food", 
            "spicy", "savory", "meat", "beef", "indonesian", "coconut"));
        nasiPadang.add(new MenuItem("S4I002", "Ayam Pop", 20000, "food", 
            "savory", "chicken", "indonesian", "traditional"));
        nasiPadang.add(new MenuItem("S4I003", "Ikan Bakar", 22000, "food", 
            "spicy", "savory", "fish", "indonesian", "grilled"));
        nasiPadang.add(new MenuItem("S4I004", "Sayur Nangka", 8000, "food", 
            "savory", "vegetables", "indonesian", "healthy"));
        storeMenus.put("STORE004", nasiPadang);
        
        // Sweet Dessert House
        List<MenuItem> desserts = new ArrayList<>();
        desserts.add(new MenuItem("S5I001", "Chocolate Lava Cake", 35000, "dessert", 
            "sweet", "chocolate", "cake", "rich", "warm"));
        desserts.add(new MenuItem("S5I002", "Tiramisu", 30000, "dessert", 
            "sweet", "coffee", "cake", "italian", "creamy"));
        desserts.add(new MenuItem("S5I003", "Strawberry Cheesecake", 32000, "dessert", 
            "sweet", "fruit", "strawberry", "cake", "creamy", "cheese"));
        desserts.add(new MenuItem("S5I004", "Ice Cream Sundae", 25000, "dessert", 
            "sweet", "cold", "ice cream", "chocolate", "vanilla"));
        desserts.add(new MenuItem("S5I005", "Pannacotta", 28000, "dessert", 
            "sweet", "creamy", "italian", "vanilla"));
        storeMenus.put("STORE005", desserts);
        
        // Burger & Pasta Station
        List<MenuItem> western = new ArrayList<>();
        western.add(new MenuItem("S6I001", "Beef Burger", 28000, "food", 
            "savory", "beef", "burger", "western", "cheese"));
        western.add(new MenuItem("S6I002", "Chicken Burger", 25000, "food", 
            "savory", "chicken", "burger", "western", "cheese"));
        western.add(new MenuItem("S6I003", "Aglio Olio Pasta", 27000, "food", 
            "savory", "pasta", "italian", "garlic", "western"));
        western.add(new MenuItem("S6I004", "Carbonara Pasta", 30000, "food", 
            "savory", "pasta", "italian", "western", "creamy", "cheese"));
        western.add(new MenuItem("S6I005", "French Fries", 15000, "food", 
            "salty", "savory", "potato", "western", "crispy"));
        storeMenus.put("STORE006", western);
    }

    private void initializeSynonyms() {
        synonyms.put("manis", "sweet");
        synonyms.put("pedas", "spicy");
        synonyms.put("asin", "salty");
        synonyms.put("gurih", "savory");
        synonyms.put("asam", "sour");
        synonyms.put("es", "ice");
        synonyms.put("dingin", "cold");
        synonyms.put("panas", "hot");
        synonyms.put("hangat", "warm");
        synonyms.put("makanan", "food");
        synonyms.put("minuman", "drink");
        synonyms.put("dessert", "dessert");
        synonyms.put("pencuci mulut", "dessert");
        synonyms.put("nasi", "rice");
        synonyms.put("mie", "noodle");
        synonyms.put("ayam", "chicken");
        synonyms.put("sapi", "beef");
        synonyms.put("ikan", "fish");
        synonyms.put("sayur", "vegetables");
        synonyms.put("buah", "fruit");
        synonyms.put("korea", "korean");
        synonyms.put("indonesia", "indonesian");
        synonyms.put("jepang", "japanese");
        synonyms.put("italia", "italian");
        synonyms.put("barat", "western");
    }

    public List<StoreItem> searchItems(String query) {
        Set<String> queryTags = parseQuery(query.toLowerCase());
        List<StoreItem> results = new ArrayList<>();
        
        for (Store store : stores) {
            List<MenuItem> menu = storeMenus.get(store.getId());
            if (menu == null) continue;
            
            for (MenuItem item : menu) {
                if (!queryTags.isEmpty() && item.hasAnyTag(queryTags)) {
                    results.add(new StoreItem(store, item));
                }
            }
        }
        
        // Sort by match score
        results.sort((a, b) -> Integer.compare(
            b.getMenuItem().getMatchScore(queryTags),
            a.getMenuItem().getMatchScore(queryTags)
        ));
        
        return results.stream().limit(10).collect(Collectors.toList());
    }

    private Set<String> parseQuery(String query) {
        Set<String> tags = new HashSet<>();
        String[] words = query.split("[\\s,+&]+");
        for (String word : words) {
            word = word.trim().toLowerCase();
            if (synonyms.containsKey(word)) {
                tags.add(synonyms.get(word));
            } else {
                tags.add(word);
            }
        }
        return tags;
    }
    
    public List<SpecialOffer> getSpecialOffers() {
        List<SpecialOffer> offers = new ArrayList<>();
        
        // Sweet Package
        List<StoreItem> sweetPackage = new ArrayList<>();
        Store miloStore = getStoreById("STORE002");
        Store dessertStore = getStoreById("STORE005");
        MenuItem milo = getMenuItem("STORE002", "S2I001");
        MenuItem cake = getMenuItem("STORE005", "S5I001");
        if (milo != null && cake != null) {
            sweetPackage.add(new StoreItem(miloStore, milo));
            sweetPackage.add(new StoreItem(dessertStore, cake));
            offers.add(new SpecialOffer("Sweet Combo Package", 
                "Es Kepal Milo + Chocolate Lava Cake", sweetPackage, 20));
        }
        
        // Refreshing Package
        List<StoreItem> refreshPackage = new ArrayList<>();
        Store tehStore = getStoreById("STORE001");
        MenuItem esTeh = getMenuItem("STORE001", "S1I001");
        MenuItem esKelapa = getMenuItem("STORE001", "S1I004");
        if (esTeh != null && esKelapa != null) {
            refreshPackage.add(new StoreItem(tehStore, esTeh));
            refreshPackage.add(new StoreItem(tehStore, esKelapa));
            offers.add(new SpecialOffer("Refreshing Duo", 
                "Es Teh Manis + Es Kelapa Muda", refreshPackage, 15));
        }
        
        // Korean Feast
        List<StoreItem> koreanPackage = new ArrayList<>();
        Store koreanStore = getStoreById("STORE003");
        MenuItem chicken = getMenuItem("STORE003", "S3I002");
        MenuItem tteokbokki = getMenuItem("STORE003", "S3I001");
        if (chicken != null && tteokbokki != null) {
            koreanPackage.add(new StoreItem(koreanStore, chicken));
            koreanPackage.add(new StoreItem(koreanStore, tteokbokki));
            offers.add(new SpecialOffer("Korean Feast", 
                "Korean Fried Chicken + Tteokbokki", koreanPackage, 25));
        }
        
        return offers;
    }
    
    private Store getStoreById(String id) {
        return stores.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }
    
    private MenuItem getMenuItem(String storeId, String itemId) {
        List<MenuItem> menu = storeMenus.get(storeId);
        if (menu == null) return null;
        return menu.stream().filter(m -> m.getId().equals(itemId)).findFirst().orElse(null);
    }
    
    public List<Store> getAllStores() {
        return new ArrayList<>(stores);
    }
}

// =============================== 
// BUYER WINDOW
// =============================== 

class BuyerChatWindow extends JFrame implements ChatListener {
    private JPanel chatContainer;
    private JPanel cartPanel;
    private JTextField inputField;
    private JLabel cartCountLabel;
    private JLabel cartTotalLabel;
    private ShoppingCart shoppingCart;
    private ChatBridge chatBridge;
    private JScrollPane chatScroll;

    public BuyerChatWindow(ChatBridge chatBridge) {
        this.chatBridge = chatBridge;
        this.shoppingCart = new ShoppingCart();
        this.chatBridge.addListener(this);
        
        setTitle("Buyer - Food Ordering Chat");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(50, 50);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.7);
        
        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 250));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        splitPane.setLeftComponent(chatScroll);
        
        cartPanel = createCartPanel();
        splitPane.setRightComponent(cartPanel);
        
        add(splitPane, BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);

        addWelcomeMessage();
        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(60, 60, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("💬 Food Ordering Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);
        
        cartCountLabel = new JLabel("Cart: 0 items");
        cartCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cartCountLabel.setForeground(new Color(200, 200, 200));
        
        cartTotalLabel = new JLabel("Total: Rp 0");
        cartTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cartTotalLabel.setForeground(new Color(100, 250, 100));
        
        rightPanel.add(cartCountLabel);
        rightPanel.add(cartTotalLabel);
        
        header.add(title, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(220, 220, 220)));
        
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setBackground(new Color(245, 245, 250));
        cartHeader.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel cartTitle = new JLabel("🛒 Shopping Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.setForeground(new Color(200, 100, 100));
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> clearCart());
        
        cartHeader.add(cartTitle, BorderLayout.WEST);
        cartHeader.add(clearBtn, BorderLayout.EAST);
        
        panel.add(cartHeader, BorderLayout.NORTH);
        
        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        bar.setBackground(Color.WHITE);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("📤 Send");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setBackground(new Color(100, 150, 250));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 130, 230), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        bar.add(inputField, BorderLayout.CENTER);
        bar.add(sendBtn, BorderLayout.EAST);

        return bar;
    }

    private void addWelcomeMessage() {
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setBackground(new Color(245, 245, 250));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        JLabel welcome = new JLabel("<html><h2>Welcome to Food Ordering Chat!</h2></html>");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 24));
        
        JLabel instruction = new JLabel("<html><p>Chat with our seller to get food recommendations!</p>" +
            "<p>Try asking:</p>" +
            "<ul>" +
            "<li>\"I want something sweet\"</li>" +
            "<li>\"Do you have any Korean food?\"</li>" +
            "<li>\"What's your special offer?\"</li>" +
            "<li>\"I need a cold drink\"</li>" +
            "</ul></html>");
        instruction.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        welcomePanel.add(welcome);
        welcomePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        welcomePanel.add(instruction);
        
        chatContainer.add(welcomePanel);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            chatBridge.sendMessageFromBuyer(message);
            inputField.setText("");
        }
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            addChatMessage(message);
            scrollToBottom();
        });
    }

    private void addChatMessage(ChatMessage message) {
        if (message.getSenderType().equals("BUYER")) {
            addBuyerMessage(message);
        } else {
            addSellerMessage(message);
        }
    }

    private void addBuyerMessage(ChatMessage message) {
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        messagePanel.setBackground(new Color(245, 245, 250));
        
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(new Color(100, 150, 250));
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 130, 230), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel textLabel = new JLabel("<html>" + message.getMessage() + "</html>");
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textLabel.setForeground(Color.WHITE);
        
        JLabel timeLabel = new JLabel(message.getFormattedTime());
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(200, 220, 255));
        
        bubble.add(textLabel, BorderLayout.CENTER);
        bubble.add(timeLabel, BorderLayout.SOUTH);
        
        messagePanel.add(bubble);
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubble.getPreferredSize().height + 10));
        
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void addSellerMessage(ChatMessage message) {
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        messagePanel.setBackground(new Color(245, 245, 250));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        if (message.getType() == MessageType.TEXT) {
            JPanel bubble = new JPanel(new BorderLayout());
            bubble.setBackground(Color.WHITE);
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            
            JLabel textLabel = new JLabel("<html>" + message.getMessage() + "</html>");
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            JLabel timeLabel = new JLabel(message.getFormattedTime());
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            
            bubble.add(textLabel, BorderLayout.CENTER);
            bubble.add(timeLabel, BorderLayout.SOUTH);
            
            messagePanel.add(bubble);
        } else if (message.getType() == MessageType.STORE_RECOMMENDATION) {
            JPanel recPanel = createRecommendationPanel(message);
            chatContainer.add(recPanel);
            chatContainer.revalidate();
            chatContainer.repaint();
            return;
        } else if (message.getType() == MessageType.SPECIAL_OFFER) {
            JPanel offerPanel = createOfferPanel(message);
            chatContainer.add(offerPanel);
            chatContainer.revalidate();
            chatContainer.repaint();
            return;
        }
        
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 
            messagePanel.getPreferredSize().height + 10));
        
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private JPanel createRecommendationPanel(ChatMessage message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Header
        JLabel headerLabel = new JLabel("🏪 " + message.getMessage());
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(headerLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Items
        for (StoreItem storeItem : message.getStoreItems()) {
            panel.add(createStoreItemCard(storeItem));
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private JPanel createStoreItemCard(StoreItem storeItem) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // Store & Item Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel storeLabel = new JLabel("📍 " + storeItem.getStore().getName());
        storeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JLabel ratingLabel = new JLabel(String.format("⭐ %.1f  •  📏 %.1f km from President University", 
            storeItem.getStore().getRating(), storeItem.getStore().getDistanceKm()));
        ratingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ratingLabel.setForeground(Color.GRAY);
        
        JLabel itemLabel = new JLabel(storeItem.getMenuItem().getName());
        itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JLabel priceLabel = new JLabel("Rp " + String.format("%,d", storeItem.getMenuItem().getPrice()));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        priceLabel.setForeground(new Color(0, 150, 0));
        
        infoPanel.add(storeLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        infoPanel.add(ratingLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(itemLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        infoPanel.add(priceLabel);
        
        // Add button
        JButton addBtn = new JButton("+ Add to Cart");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(new Color(100, 200, 100));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        addBtn.addActionListener(e -> {
            shoppingCart.addItem(storeItem, 1);
            updateCartDisplay();
            JOptionPane.showMessageDialog(this,
                String.format("Added %s to cart!", storeItem.getMenuItem().getName()),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(addBtn, BorderLayout.EAST);
        
        return card;
    }

    private JPanel createOfferPanel(ChatMessage message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        SpecialOffer offer = message.getSpecialOffer();
        
        JPanel offerCard = new JPanel(new BorderLayout(15, 0));
        offerCard.setBackground(new Color(255, 250, 240));
        offerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(250, 200, 100), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Offer Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🎁 " + offer.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel descLabel = new JLabel(offer.getDescription());
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JLabel savingsLabel = new JLabel(String.format("Save %d%% - Rp %,d off!", 
            offer.getDiscountPercent(), offer.getSavings()));
        savingsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        savingsLabel.setForeground(new Color(200, 100, 50));
        
        JLabel priceLabel = new JLabel(String.format("<html><s>Rp %,d</s> → <b>Rp %,d</b></html>", 
            offer.getOriginalPrice(), offer.getOfferPrice()));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(descLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(savingsLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        infoPanel.add(priceLabel);
        
        // Add button
        JButton addBtn = new JButton("🎁 Add Offer to Cart");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBtn.setBackground(new Color(250, 150, 50));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        addBtn.addActionListener(e -> {
            for (StoreItem item : offer.getItems()) {
                shoppingCart.addItem(item, 1);
            }
            updateCartDisplay();
            JOptionPane.showMessageDialog(this,
                String.format("Added %s to cart!\nYou save Rp %,d!", 
                    offer.getTitle(), offer.getSavings()),
                "Special Offer Added",
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        offerCard.add(infoPanel, BorderLayout.CENTER);
        offerCard.add(addBtn, BorderLayout.EAST);
        
        panel.add(offerCard);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, offerCard.getPreferredSize().height + 20));
        
        return panel;
    }

    private void updateCartDisplay() {
        cartCountLabel.setText("Cart: " + shoppingCart.getItemCount() + " items");
        cartTotalLabel.setText("Total: Rp " + String.format("%,d", shoppingCart.getTotal()));
        
        cartPanel.removeAll();
        
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setBackground(new Color(245, 245, 250));
        cartHeader.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel cartTitle = new JLabel("🛒 Shopping Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.setForeground(new Color(200, 100, 100));
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> clearCart());
        
        cartHeader.add(cartTitle, BorderLayout.WEST);
        cartHeader.add(clearBtn, BorderLayout.EAST);
        
        cartPanel.add(cartHeader, BorderLayout.NORTH);
        
        if (shoppingCart.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(Color.WHITE);
            JLabel emptyLabel = new JLabel("<html><center>Cart is empty<br>Start chatting to order!</center></html>");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            emptyLabel.setForeground(Color.GRAY);
            emptyPanel.add(emptyLabel);
            cartPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            for (CartItem item : shoppingCart.getItems()) {
                contentPanel.add(createCartItemRow(item));
                contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
            
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(createSeparator());
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(createSummaryRow("TOTAL:", shoppingCart.getTotal()));
            
            JScrollPane scrollPane = new JScrollPane(contentPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            cartPanel.add(scrollPane, BorderLayout.CENTER);
            
            JButton checkoutBtn = new JButton("💳 Checkout");
            checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            checkoutBtn.setBackground(new Color(100, 200, 100));
            checkoutBtn.setForeground(Color.WHITE);
            checkoutBtn.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            checkoutBtn.setFocusPainted(false);
            checkoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            checkoutBtn.addActionListener(e -> showCheckoutDialog());
            
            JPanel checkoutPanel = new JPanel(new BorderLayout());
            checkoutPanel.setBackground(Color.WHITE);
            checkoutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            checkoutPanel.add(checkoutBtn, BorderLayout.CENTER);
            
            cartPanel.add(checkoutPanel, BorderLayout.SOUTH);
        }
        
        cartPanel.revalidate();
        cartPanel.repaint();
    }

    private JPanel createCartItemRow(CartItem item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(new Color(250, 250, 250));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(item.getStoreItem().getMenuItem().getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JLabel storeLabel = new JLabel("from " + item.getStoreItem().getStore().getName());
        storeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        storeLabel.setForeground(Color.GRAY);
        
        JLabel priceLabel = new JLabel(String.format("Rp %,d × %d", 
            item.getStoreItem().getMenuItem().getPrice(), item.getQuantity()));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        priceLabel.setForeground(Color.GRAY);
        
        infoPanel.add(nameLabel);
        infoPanel.add(storeLabel);
        infoPanel.add(priceLabel);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controlPanel.setOpaque(false);
        
        JButton minusBtn = new JButton("−");
        minusBtn.setPreferredSize(new Dimension(30, 25));
        minusBtn.setFocusPainted(false);
        minusBtn.addActionListener(e -> {
            shoppingCart.updateQuantity(item.getStoreItem().getMenuItem().getId(), 
                item.getStoreItem().getStore().getId(), item.getQuantity() - 1);
            updateCartDisplay();
        });
        
        JLabel qtyLabel = new JLabel(String.valueOf(item.getQuantity()));
        qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        qtyLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        
        JButton plusBtn = new JButton("+");
        plusBtn.setPreferredSize(new Dimension(30, 25));
        plusBtn.setFocusPainted(false);
        plusBtn.addActionListener(e -> {
            shoppingCart.updateQuantity(item.getStoreItem().getMenuItem().getId(),
                item.getStoreItem().getStore().getId(), item.getQuantity() + 1);
            updateCartDisplay();
        });
        
        JButton removeBtn = new JButton("🗑");
        removeBtn.setPreferredSize(new Dimension(30, 25));
        removeBtn.setForeground(new Color(200, 100, 100));
        removeBtn.setFocusPainted(false);
        removeBtn.addActionListener(e -> {
            shoppingCart.removeItem(item.getStoreItem().getMenuItem().getId(),
                item.getStoreItem().getStore().getId());
            updateCartDisplay();
        });
        
        controlPanel.add(minusBtn);
        controlPanel.add(qtyLabel);
        controlPanel.add(plusBtn);
        controlPanel.add(removeBtn);
        
        JLabel totalLabel = new JLabel(String.format("Rp %,d", item.getItemTotal()));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.add(totalLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(controlPanel);
        
        row.add(infoPanel, BorderLayout.CENTER);
        row.add(rightPanel, BorderLayout.EAST);
        
        return row;
    }

    private JPanel createSummaryRow(String label, int amount) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JLabel amountText = new JLabel(String.format("Rp %,d", amount));
        amountText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amountText.setForeground(new Color(50, 100, 200));
        
        row.add(labelText, BorderLayout.WEST);
        row.add(amountText, BorderLayout.EAST);
        
        return row;
    }

    private JPanel createSeparator() {
        JPanel separator = new JPanel();
        separator.setBackground(new Color(230, 230, 230));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separator;
    }

    private void clearCart() {
        if (shoppingCart.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear all items from cart?",
            "Clear Cart",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            shoppingCart.clear();
            updateCartDisplay();
        }
    }

    private void showCheckoutDialog() {
        JDialog dialog = new JDialog(this, "Checkout", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField nameField = new JTextField(30);
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField phoneField = new JTextField(30);
        formPanel.add(phoneField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextArea addressArea = new JTextArea(4, 30);
        addressArea.setLineWrap(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);
        formPanel.add(addressScroll, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextArea notesArea = new JTextArea(3, 30);
        notesArea.setLineWrap(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        formPanel.add(notesScroll, gbc);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        JButton orderBtn = new JButton("Place Order");
        orderBtn.setBackground(new Color(100, 200, 100));
        orderBtn.setForeground(Color.WHITE);
        orderBtn.setFocusPainted(false);
        orderBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressArea.getText().trim();
            String notes = notesArea.getText().trim();
            
            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields!");
                return;
            }
            
            Order order = shoppingCart.checkout(name, phone, address, notes);
            // Here you would send order to seller
            
            dialog.dispose();
            JOptionPane.showMessageDialog(this,
                String.format("Order placed successfully!\nOrder ID: %s\nTotal: Rp %,d",
                    order.getOrderId(), order.getTotal()),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
            shoppingCart.clear();
            updateCartDisplay();
        });
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(orderBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}

// =============================== 
// SELLER WINDOW
// =============================== 

class SellerWindow extends JFrame implements ChatListener {
    private JPanel chatContainer;
    private JTextField responseField;
    private ChatBridge chatBridge;
    private MultiStoreSystem storeSystem;
    private JScrollPane chatScroll;

    public SellerWindow(ChatBridge chatBridge) {
        this.chatBridge = chatBridge;
        this.storeSystem = new MultiStoreSystem();
        this.chatBridge.addListener(this);
        
        setTitle("Seller Dashboard - Chat Management");
        setSize(900, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(1000, 50);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);

        chatContainer = new JPanel();
        chatContainer.setLayout(new BoxLayout(chatContainer, BoxLayout.Y_AXIS));
        chatContainer.setBackground(new Color(245, 245, 250));
        chatScroll = new JScrollPane(chatContainer);
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(chatScroll, BorderLayout.CENTER);

        add(createBottomBar(), BorderLayout.SOUTH);

        addWelcomeMessage();
        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(60, 60, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("📊 Seller Chat Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        quickPanel.setOpaque(false);
        
        JButton offersBtn = new JButton("🎁 Send Special Offers");
        offersBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        offersBtn.setBackground(new Color(250, 150, 50));
        offersBtn.setForeground(Color.WHITE);
        offersBtn.setFocusPainted(false);
        offersBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        offersBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        offersBtn.addActionListener(e -> showOffersDialog());
        
        quickPanel.add(offersBtn);
        
        header.add(title, BorderLayout.WEST);
        header.add(quickPanel, BorderLayout.EAST);
        
        return header;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        bar.setBackground(Color.WHITE);

        responseField = new JTextField();
        responseField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        responseField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        responseField.addActionListener(e -> sendResponse());

        JButton sendBtn = new JButton("📤 Send");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setBackground(new Color(100, 200, 100));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 180, 80), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendResponse());

        bar.add(responseField, BorderLayout.CENTER);
        bar.add(sendBtn, BorderLayout.EAST);

        return bar;
    }

    private void addWelcomeMessage() {
        JPanel welcomePanel = new JPanel();
        welcomePanel.setBackground(new Color(245, 245, 250));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        JLabel welcomeLabel = new JLabel("<html><center>" +
            "<h2>Seller Dashboard Ready</h2>" +
            "<p>Waiting for customer messages...</p>" +
            "</center></html>");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeLabel.setForeground(Color.GRAY);
        
        welcomePanel.add(welcomeLabel);
        chatContainer.add(welcomePanel);
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            // Remove welcome if first message
            if (chatContainer.getComponentCount() > 0) {
                Component first = chatContainer.getComponent(0);
                if (first instanceof JPanel) {
                    JPanel panel = (JPanel) first;
                    if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JLabel) {
                        chatContainer.removeAll();
                    }
                }
            }
            
            addChatMessage(message);
            
            // Auto-respond to buyer queries
            if (message.getSenderType().equals("BUYER") && message.getType() == MessageType.TEXT) {
                autoRespond(message.getMessage());
            }
            
            scrollToBottom();
        });
    }

    private void autoRespond(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Check for special offer request
        if (lowerQuery.contains("special") || lowerQuery.contains("offer") || 
            lowerQuery.contains("promo") || lowerQuery.contains("diskon") ||
            lowerQuery.contains("discount")) {
            
            Timer timer = new Timer(1000, e -> {
                List<SpecialOffer> offers = storeSystem.getSpecialOffers();
                if (!offers.isEmpty()) {
                    chatBridge.sendSpecialOffer(offers.get(0));
                }
            });
            timer.setRepeats(false);
            timer.start();
            return;
        }
        
        // Search for items
        List<StoreItem> results = storeSystem.searchItems(query);
        
        if (!results.isEmpty()) {
            Timer timer = new Timer(1000, e -> {
                String responseMsg = "Here are some recommendations for you:";
                chatBridge.sendStoreRecommendation(results, responseMsg);
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            Timer timer = new Timer(800, e -> {
                chatBridge.sendMessageFromSeller(
                    "I couldn't find specific items for that. Try asking for 'sweet', 'spicy', 'cold drink', etc. Or ask about our special offers!");
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void addChatMessage(ChatMessage message) {
        if (message.getSenderType().equals("BUYER")) {
            addBuyerMessage(message);
        } else {
            addSellerMessage(message);
        }
    }

    private void addBuyerMessage(ChatMessage message) {
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        messagePanel.setBackground(new Color(245, 245, 250));
        
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(Color.WHITE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel textLabel = new JLabel("<html><b>Customer:</b> " + message.getMessage() + "</html>");
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JLabel timeLabel = new JLabel(message.getFormattedTime());
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);
        
        bubble.add(textLabel, BorderLayout.CENTER);
        bubble.add(timeLabel, BorderLayout.SOUTH);
        
        messagePanel.add(bubble);
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubble.getPreferredSize().height + 10));
        
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void addSellerMessage(ChatMessage message) {
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        messagePanel.setBackground(new Color(245, 245, 250));
        
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(new Color(100, 200, 100));
        bubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 180, 80), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel textLabel = new JLabel("<html>" + message.getMessage() + "</html>");
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textLabel.setForeground(Color.WHITE);
        
        JLabel timeLabel = new JLabel(message.getFormattedTime());
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(220, 255, 220));
        
        bubble.add(textLabel, BorderLayout.CENTER);
        bubble.add(timeLabel, BorderLayout.SOUTH);
        
        messagePanel.add(bubble);
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubble.getPreferredSize().height + 10));
        
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
    }

    private void sendResponse() {
        String message = responseField.getText().trim();
        if (!message.isEmpty()) {
            chatBridge.sendMessageFromSeller(message);
            responseField.setText("");
        }
    }

    private void showOffersDialog() {
        JDialog dialog = new JDialog(this, "Send Special Offer", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel offersPanel = new JPanel();
        offersPanel.setLayout(new BoxLayout(offersPanel, BoxLayout.Y_AXIS));
        offersPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        List<SpecialOffer> offers = storeSystem.getSpecialOffers();
        
        for (SpecialOffer offer : offers) {
            JPanel offerCard = new JPanel(new BorderLayout(10, 0));
            offerCard.setBackground(new Color(255, 250, 240));
            offerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(250, 200, 100), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            offerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            
            JLabel offerLabel = new JLabel(String.format("<html><b>%s</b><br>%s<br>Save Rp %,d!</html>",
                offer.getTitle(), offer.getDescription(), offer.getSavings()));
            offerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            JButton sendBtn = new JButton("Send");
            sendBtn.setBackground(new Color(100, 200, 100));
            sendBtn.setForeground(Color.WHITE);
            sendBtn.setFocusPainted(false);
            sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            sendBtn.addActionListener(e -> {
                chatBridge.sendSpecialOffer(offer);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Special offer sent to customer!");
            });
            
            offerCard.add(offerLabel, BorderLayout.CENTER);
            offerCard.add(sendBtn, BorderLayout.EAST);
            
            offersPanel.add(offerCard);
            offersPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        JScrollPane scroll = new JScrollPane(offersPanel);
        dialog.add(scroll, BorderLayout.CENTER);
        
        dialog.setVisible(true);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
}
