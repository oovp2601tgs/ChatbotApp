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
            SellerWindow sellerWindow = new SellerWindow(chatBridge);
            BuyerChatWindow buyerWindow = new BuyerChatWindow(chatBridge);
            
            sellerWindow.setVisible(true);
            buyerWindow.setVisible(true);
        });
    }
}

// =============================== 
// ENHANCED CHAT BRIDGE - Real-time messaging with AI responses
// =============================== 

class ChatBridge {
    private final List<ChatMessage> messageHistory;
    private final List<ChatListener> listeners;
    private String currentBuyerName = "Customer";
    private final FoodChatbotAI chatbotAI;
    private final MultiStoreSystem storeSystem;
    
    public ChatBridge() {
        messageHistory = new ArrayList<>();
        listeners = new ArrayList<>();
        storeSystem = new MultiStoreSystem();
        chatbotAI = new FoodChatbotAI(storeSystem);
    }
    
    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }
    
    public void sendMessageFromBuyer(String message) {
        ChatMessage msg = new ChatMessage(currentBuyerName, "BUYER", message, MessageType.TEXT);
        messageHistory.add(msg);
        notifyListeners(msg);
        
        // Generate AI response
        ChatResponse aiResponse = chatbotAI.generateResponse(message);
        
        Timer timer = new Timer(1500, e -> {
            switch (aiResponse.getRecommendationType()) {
                case STORE_RECOMMENDATION:
                    sendStoreRecommendation(aiResponse.getStoreItems(), aiResponse.getMessage());
                    break;
                case SPECIAL_OFFER:
                    sendSpecialOffer(aiResponse.getSpecialOffer());
                    break;
                case CUSTOM_PACKAGE:
                    sendCustomPackage(aiResponse.getStoreItems(), aiResponse.getMessage());
                    break;
                default:
                    sendMessageFromSeller(aiResponse.getMessage());
                    break;
            }
        });
        timer.setRepeats(false);
        timer.start();
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
    
    public void sendCustomPackage(List<StoreItem> items, String message) {
        ChatMessage msg = new ChatMessage("Seller", "SELLER", message, MessageType.CUSTOM_PACKAGE);
        msg.setStoreItems(items);
        messageHistory.add(msg);
        notifyListeners(msg);
    }
    
    public void sendOrderUpdate(Order order, String message) {
        ChatMessage msg = new ChatMessage("Seller", "SELLER", message, MessageType.ORDER_UPDATE);
        msg.setOrder(order);
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
        this.chatbotAI.setCustomerName(name);
    }
    
    public List<ChatMessage> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    public void placeOrder(Order order) {
        // Simulate order processing
        Timer timer = new Timer(2000, e -> {
            sendOrderUpdate(order, "✅ Your order has been received! Order ID: " + order.getOrderId());
        });
        timer.setRepeats(false);
        timer.start();
    }
}

interface ChatListener {
    void onMessageReceived(ChatMessage message);
}

enum MessageType {
    TEXT,
    STORE_RECOMMENDATION,
    SPECIAL_OFFER,
    ORDER_UPDATE,
    CUSTOM_PACKAGE,
    SYSTEM
}

enum RecommendationType {
    STORE_RECOMMENDATION,
    SPECIAL_OFFER,
    CUSTOM_PACKAGE,
    TEXT_RESPONSE
}

class ChatResponse {
    private final String message;
    private final RecommendationType type;
    private List<StoreItem> storeItems;
    private SpecialOffer specialOffer;
    
    public ChatResponse(String message, RecommendationType type) {
        this.message = message;
        this.type = type;
    }
    
    public String getMessage() { return message; }
    public RecommendationType getRecommendationType() { return type; }
    public List<StoreItem> getStoreItems() { return storeItems; }
    public SpecialOffer getSpecialOffer() { return specialOffer; }
    
    public void setStoreItems(List<StoreItem> items) { this.storeItems = items; }
    public void setSpecialOffer(SpecialOffer offer) { this.specialOffer = offer; }
}

class ChatMessage {
    private final String senderName;
    private final String senderType;
    private final String message;
    private final MessageType type;
    private final LocalDateTime timestamp;
    private List<StoreItem> storeItems;
    private SpecialOffer specialOffer;
    private Order order;
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
    public Order getOrder() { return order; }
    public boolean isRead() { return isRead; }
    
    public void setStoreItems(List<StoreItem> items) { this.storeItems = items; }
    public void setSpecialOffer(SpecialOffer offer) { this.specialOffer = offer; }
    public void setOrder(Order order) { this.order = order; }
    public void setRead(boolean read) { this.isRead = read; }
    
    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
    }
}

// =============================== 
// ENHANCED DATA MODELS
// =============================== 

class CuisineRegion {
    private final String id;
    private final String name;
    private final String description;
    private final List<String> tags;
    
    public CuisineRegion(String id, String name, String description, String... tags) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tags = new ArrayList<>(Arrays.asList(tags));
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    
    public boolean matchesQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return name.toLowerCase().contains(lowerQuery) ||
               description.toLowerCase().contains(lowerQuery) ||
               tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery));
    }
}

class Store {
    private final String id;
    private final String name;
    private final String description;
    private final double rating;
    private final double distanceKm;
    private final CuisineRegion cuisineRegion;
    private final List<String> specialtyTags;
    private final int deliveryTime;
    private final double deliveryFee;
    private boolean isOpen;
    
    public Store(String id, String name, String description, double rating, double distanceKm, 
                 CuisineRegion cuisineRegion, int deliveryTime, double deliveryFee, String... specialtyTags) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rating = rating;
        this.distanceKm = distanceKm;
        this.cuisineRegion = cuisineRegion;
        this.deliveryTime = deliveryTime;
        this.deliveryFee = deliveryFee;
        this.specialtyTags = new ArrayList<>(Arrays.asList(specialtyTags));
        this.isOpen = true;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
    public double getDistanceKm() { return distanceKm; }
    public CuisineRegion getCuisineRegion() { return cuisineRegion; }
    public List<String> getSpecialtyTags() { return specialtyTags; }
    public int getDeliveryTime() { return deliveryTime; }
    public double getDeliveryFee() { return deliveryFee; }
    public boolean isOpen() { return isOpen; }
    
    public boolean hasSpecialty(String tag) {
        return specialtyTags.stream().anyMatch(t -> t.equalsIgnoreCase(tag));
    }
    
    public double getRelevanceScore(String query, Set<String> queryTags) {
        double score = 0;
        
        // Distance score (closer is better)
        score += (5.0 - Math.min(distanceKm, 5.0)) * 10;
        
        // Rating score
        score += rating * 8;
        
        // Tag matching score
        for (String tag : queryTags) {
            if (specialtyTags.contains(tag)) {
                score += 20;
            }
        }
        
        // Delivery time score (faster is better)
        score += (60.0 - deliveryTime) / 2;
        
        return score;
    }
}

class StoreItem {
    private final Store store;
    private final MenuItem menuItem;
    private final boolean isBestSeller;
    private final boolean isRecommended;
    private final int popularityScore;
    
    public StoreItem(Store store, MenuItem menuItem, boolean isBestSeller, boolean isRecommended, int popularityScore) {
        this.store = store;
        this.menuItem = menuItem;
        this.isBestSeller = isBestSeller;
        this.isRecommended = isRecommended;
        this.popularityScore = popularityScore;
    }
    
    public Store getStore() { return store; }
    public MenuItem getMenuItem() { return menuItem; }
    public boolean isBestSeller() { return isBestSeller; }
    public boolean isRecommended() { return isRecommended; }
    public int getPopularityScore() { return popularityScore; }
    
    public int getTotalScore(Set<String> queryTags) {
        int score = menuItem.getMatchScore(queryTags);
        if (isBestSeller) score += 15;
        if (isRecommended) score += 10;
        score += popularityScore / 10;
        score += (int)(store.getRating() * 5);
        return score;
    }
}

class SpecialOffer {
    private final String id;
    private final String title;
    private final String description;
    private final List<StoreItem> items;
    private final int discountPercent;
    private final int originalPrice;
    private final int offerPrice;
    private final String offerType;
    private final LocalDateTime validUntil;
    private boolean isActive;
    
    public SpecialOffer(String id, String title, String description, List<StoreItem> items, 
                       int discountPercent, String offerType, int hoursValid) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.items = items;
        this.discountPercent = discountPercent;
        this.offerType = offerType;
        this.originalPrice = items.stream().mapToInt(i -> i.getMenuItem().getPrice()).sum();
        this.offerPrice = (int)(originalPrice * (1 - discountPercent / 100.0));
        this.validUntil = LocalDateTime.now().plusHours(hoursValid);
        this.isActive = true;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<StoreItem> getItems() { return items; }
    public int getDiscountPercent() { return discountPercent; }
    public int getOriginalPrice() { return originalPrice; }
    public int getOfferPrice() { return offerPrice; }
    public int getSavings() { return originalPrice - offerPrice; }
    public String getOfferType() { return offerType; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public boolean isActive() { return isActive; }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validUntil);
    }
    
    public String getFormattedValidUntil() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "Valid until " + validUntil.format(formatter);
    }
}

class CustomPackage {
    private final String id;
    private final String name;
    private final String description;
    private final List<StoreItem> items;
    private final int totalPrice;
    private final int packageDiscount;
    private final Set<String> tags;
    private final String theme;
    
    public CustomPackage(String id, String name, String description, List<StoreItem> items, 
                        int packageDiscount, String theme, String... tags) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.items = items;
        this.theme = theme;
        this.tags = new HashSet<>(Arrays.asList(tags));
        this.totalPrice = items.stream().mapToInt(i -> i.getMenuItem().getPrice()).sum();
        this.packageDiscount = packageDiscount;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<StoreItem> getItems() { return items; }
    public int getTotalPrice() { return totalPrice; }
    public int getPackageDiscount() { return packageDiscount; }
    public int getDiscountedPrice() { return totalPrice - (totalPrice * packageDiscount / 100); }
    public Set<String> getTags() { return tags; }
    public String getTheme() { return theme; }
    
    public boolean matchesTheme(String themeQuery) {
        return theme.toLowerCase().contains(themeQuery.toLowerCase()) ||
               tags.stream().anyMatch(tag -> tag.toLowerCase().contains(themeQuery.toLowerCase()));
    }
}

class MenuItem {
    private final String id;
    private final String name;
    private final int price;
    private final Set<String> tags;
    private final String category;
    private final String description;
    private final int spiceLevel;
    private final boolean isVegetarian;
    private final boolean isHalal;
    private final int calories;
    private final List<String> ingredients;

    public MenuItem(String id, String name, int price, String category, String description,
                    int spiceLevel, boolean isVegetarian, boolean isHalal, int calories, 
                    String... tags) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.spiceLevel = spiceLevel;
        this.isVegetarian = isVegetarian;
        this.isHalal = isHalal;
        this.calories = calories;
        this.tags = new HashSet<>(Arrays.asList(tags));
        this.ingredients = new ArrayList<>();
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
    
    public boolean matchesAllTags(Set<String> queryTags) {
        return queryTags.stream().allMatch(tag -> this.tags.contains(tag));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public int getSpiceLevel() { return spiceLevel; }
    public boolean isVegetarian() { return isVegetarian; }
    public boolean isHalal() { return isHalal; }
    public int getCalories() { return calories; }
    public Set<String> getTags() { return tags; }
    public List<String> getIngredients() { return ingredients; }
    
    public void addIngredient(String ingredient) {
        ingredients.add(ingredient);
    }
}

class CartItem {
    private final StoreItem storeItem;
    private int quantity;
    private int itemTotal;
    private String specialInstructions;

    public CartItem(StoreItem item, int quantity, String specialInstructions) {
        this.storeItem = item;
        this.quantity = quantity;
        this.specialInstructions = specialInstructions;
        this.itemTotal = item.getMenuItem().getPrice() * quantity;
    }

    public StoreItem getStoreItem() { return storeItem; }
    public int getQuantity() { return quantity; }
    public int getItemTotal() { return itemTotal; }
    public String getSpecialInstructions() { return specialInstructions; }
    
    public void setQuantity(int qty) {
        this.quantity = qty;
        this.itemTotal = storeItem.getMenuItem().getPrice() * qty;
    }
    
    public void setSpecialInstructions(String instructions) {
        this.specialInstructions = instructions;
    }
}

class Order {
    private final String orderId;
    private final String customerName;
    private final String phoneNumber;
    private final String deliveryAddress;
    private final String specialNotes;
    private final List<CartItem> items;
    private final int subtotal;
    private final int deliveryFee;
    private final int total;
    private final LocalDateTime orderTime;
    private LocalDateTime estimatedDelivery;
    private OrderStatus status;
    private final String assignedStore;
    private String deliveryPerson;
    private final List<OrderUpdate> updates;
    
    public Order(String customerName, String phone, String address, String notes,
                 List<CartItem> items, int subtotal, int deliveryFee, String assignedStore) {
        this.orderId = generateOrderId();
        this.customerName = customerName;
        this.phoneNumber = phone;
        this.deliveryAddress = address;
        this.specialNotes = notes;
        this.items = new ArrayList<>(items);
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = subtotal + deliveryFee;
        this.orderTime = LocalDateTime.now();
        this.estimatedDelivery = orderTime.plusMinutes(45);
        this.status = OrderStatus.PENDING;
        this.assignedStore = assignedStore;
        this.updates = new ArrayList<>();
        this.updates.add(new OrderUpdate("Order created", OrderStatus.PENDING, LocalDateTime.now()));
    }
    
    private String generateOrderId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "ORD-" + LocalDateTime.now().format(formatter);
    }
    
    public void addUpdate(String description, OrderStatus newStatus) {
        this.status = newStatus;
        this.updates.add(new OrderUpdate(description, newStatus, LocalDateTime.now()));
    }
    
    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getSpecialNotes() { return specialNotes; }
    public List<CartItem> getItems() { return items; }
    public int getSubtotal() { return subtotal; }
    public int getDeliveryFee() { return deliveryFee; }
    public int getTotal() { return total; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public OrderStatus getStatus() { return status; }
    public String getAssignedStore() { return assignedStore; }
    public String getDeliveryPerson() { return deliveryPerson; }
    public List<OrderUpdate> getUpdates() { return updates; }
    
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setDeliveryPerson(String deliveryPerson) { this.deliveryPerson = deliveryPerson; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    
    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return orderTime.format(formatter);
    }
    
    public String getFormattedEstimatedDelivery() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return estimatedDelivery.format(formatter);
    }
}

class OrderUpdate {
    private final String description;
    private final OrderStatus status;
    private final LocalDateTime timestamp;
    
    public OrderUpdate(String description, OrderStatus status, LocalDateTime timestamp) {
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }
    
    public String getDescription() { return description; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return timestamp.format(formatter);
    }
}

enum OrderStatus {
    PENDING("Pending", new Color(255, 200, 100), "⏳"),
    CONFIRMED("Confirmed", new Color(100, 150, 250), "✅"),
    PREPARING("Preparing", new Color(150, 100, 250), "👨‍🍳"),
    READY("Ready for Pickup", new Color(100, 200, 100), "📦"),
    ON_THE_WAY("On the Way", new Color(100, 180, 250), "🚚"),
    DELIVERED("Delivered", new Color(100, 200, 100), "🎉"),
    CANCELLED("Cancelled", new Color(200, 100, 100), "❌"),
    REJECTED("Rejected", new Color(200, 100, 100), "⛔");
    
    private final String displayName;
    private final Color color;
    private final String emoji;
    
    OrderStatus(String displayName, Color color, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.emoji = emoji;
    }
    
    public String getDisplayName() { return displayName; }
    public Color getColor() { return color; }
    public String getEmoji() { return emoji; }
}

// =============================== 
// ENHANCED SHOPPING CART SYSTEM
// =============================== 

class ShoppingCart {
    private final List<CartItem> items;
    private final Map<String, Integer> storeItemCount;
    private String selectedStore;
    
    public ShoppingCart() {
        this.items = new ArrayList<>();
        this.storeItemCount = new HashMap<>();
    }
    
    public void addItem(StoreItem item, int quantity, String specialInstructions) {
        for (CartItem cartItem : items) {
            if (cartItem.getStoreItem().getMenuItem().getId().equals(item.getMenuItem().getId()) &&
                cartItem.getStoreItem().getStore().getId().equals(item.getStore().getId())) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                if (specialInstructions != null && !specialInstructions.isEmpty()) {
                    cartItem.setSpecialInstructions(specialInstructions);
                }
                updateStoreCount();
                return;
            }
        }
        items.add(new CartItem(item, quantity, specialInstructions));
        updateStoreCount();
    }
    
    public void removeItem(String itemId, String storeId) {
        items.removeIf(item -> 
            item.getStoreItem().getMenuItem().getId().equals(itemId) &&
            item.getStoreItem().getStore().getId().equals(storeId));
        updateStoreCount();
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
        updateStoreCount();
    }
    
    public void updateSpecialInstructions(String itemId, String storeId, String instructions) {
        for (CartItem item : items) {
            if (item.getStoreItem().getMenuItem().getId().equals(itemId) &&
                item.getStoreItem().getStore().getId().equals(storeId)) {
                item.setSpecialInstructions(instructions);
                break;
            }
        }
    }
    
    private void updateStoreCount() {
        storeItemCount.clear();
        for (CartItem item : items) {
            String storeId = item.getStoreItem().getStore().getId();
            storeItemCount.put(storeId, storeItemCount.getOrDefault(storeId, 0) + item.getQuantity());
        }
        
        // Auto-select store with most items
        if (!storeItemCount.isEmpty()) {
            selectedStore = Collections.max(storeItemCount.entrySet(), Map.Entry.comparingByValue()).getKey();
        }
    }
    
    public int getSubtotal() {
        return items.stream().mapToInt(CartItem::getItemTotal).sum();
    }
    
    public int getDeliveryFee() {
        if (selectedStore == null || items.isEmpty()) return 0;
        
        // Find store delivery fee
        for (CartItem item : items) {
            if (item.getStoreItem().getStore().getId().equals(selectedStore)) {
                return (int) item.getStoreItem().getStore().getDeliveryFee();
            }
        }
        return 5000; // Default delivery fee
    }
    
    public int getTotal() {
        return getSubtotal() + getDeliveryFee();
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
    
    public int getUniqueItemCount() {
        return items.size();
    }
    
    public String getSelectedStore() {
        return selectedStore;
    }
    
    public String getSelectedStoreName() {
        if (selectedStore == null) return "Multiple Stores";
        for (CartItem item : items) {
            if (item.getStoreItem().getStore().getId().equals(selectedStore)) {
                return item.getStoreItem().getStore().getName();
            }
        }
        return "Multiple Stores";
    }
    
    public void clear() {
        items.clear();
        storeItemCount.clear();
        selectedStore = null;
    }
    
    public Order checkout(String customerName, String phone, String address, String notes) {
        return new Order(customerName, phone, address, notes, 
                        items, getSubtotal(), getDeliveryFee(), selectedStore);
    }
    
    public boolean hasItemsFromMultipleStores() {
        return storeItemCount.size() > 1;
    }
}

// =============================== 
// AI CHATBOT FOR FOOD RECOMMENDATIONS
// =============================== 

class FoodChatbotAI {
    private final MultiStoreSystem storeSystem;
    private String customerName;
    private final Map<String, Integer> preferenceWeights;
    private final List<String> conversationHistory;
    
    public FoodChatbotAI(MultiStoreSystem storeSystem) {
        this.storeSystem = storeSystem;
        this.customerName = "Customer";
        this.preferenceWeights = new HashMap<>();
        this.conversationHistory = new ArrayList<>();
    }
    
    public void setCustomerName(String name) {
        this.customerName = name;
    }
    
    public ChatResponse generateResponse(String userMessage) {
        conversationHistory.add("User: " + userMessage);
        
        String lowerMessage = userMessage.toLowerCase();
        ChatResponse response;
        
        // Update preference weights based on message
        updatePreferences(userMessage);
        
        // Check for regional cuisine requests
        if (containsRegionalRequest(lowerMessage)) {
            response = handleRegionalRequest(lowerMessage);
        }
        // Check for specific food type requests
        else if (containsFoodTypeRequest(lowerMessage)) {
            response = handleFoodTypeRequest(lowerMessage);
        }
        // Check for special offers request
        else if (containsOfferRequest(lowerMessage)) {
            response = handleOfferRequest();
        }
        // Check for recommendation based on preferences
        else if (containsRecommendationRequest(lowerMessage)) {
            response = handlePersonalizedRecommendation();
        }
        // Check for dietary restrictions
        else if (containsDietaryRequest(lowerMessage)) {
            response = handleDietaryRequest(lowerMessage);
        }
        // Check for price range
        else if (containsPriceRequest(lowerMessage)) {
            response = handlePriceRequest(lowerMessage);
        }
        // Check for greeting
        else if (containsGreeting(lowerMessage)) {
            response = new ChatResponse(generateGreeting(), RecommendationType.TEXT_RESPONSE);
        }
        // Default: search for items
        else {
            response = handleSearchRequest(userMessage);
        }
        
        conversationHistory.add("Bot: " + response.getMessage());
        return response;
    }
    
    private void updatePreferences(String message) {
        String[] words = message.toLowerCase().split("\\s+");
        Map<String, Integer> tagIncrement = new HashMap<>();
        
        for (String word : words) {
            String tag = storeSystem.getTagFromSynonym(word);
            if (tag != null) {
                tagIncrement.put(tag, tagIncrement.getOrDefault(tag, 0) + 1);
            }
        }
        
        for (Map.Entry<String, Integer> entry : tagIncrement.entrySet()) {
            preferenceWeights.put(entry.getKey(), 
                preferenceWeights.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }
    
    private boolean containsRegionalRequest(String message) {
        String[] regionalKeywords = {"padang", "nasi padang", "sumatra", "west sumatra",
                                    "korean", "korea", "kimchi", "k-pop",
                                    "japanese", "japan", "sushi", "ramen", "bento",
                                    "chinese", "china", "dimsum", "noodle",
                                    "western", "burger", "pasta", "steak", "fries",
                                    "indonesian", "indo", "nasi", "gado-gado",
                                    "thai", "thailand", "tom yum", "pad thai",
                                    "indian", "india", "curry", "naan", "biryani",
                                    "italian", "italy", "pizza", "pasta", "risotto",
                                    "middle eastern", "arabic", "shawarma", "kebab",
                                    "mediterranean", "greek", "salad", "olive"};
        
        for (String keyword : regionalKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsFoodTypeRequest(String message) {
        String[] foodTypeKeywords = {"spicy", "sweet", "sour", "salty", "savory",
                                    "fried", "grilled", "steamed", "baked",
                                    "vegetarian", "vegan", "halal", "healthy",
                                    "appetizer", "main course", "dessert", "drink",
                                    "snack", "breakfast", "lunch", "dinner",
                                    "hot", "cold", "fresh", "creamy", "crispy"};
        
        for (String keyword : foodTypeKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsOfferRequest(String message) {
        String[] offerKeywords = {"offer", "promo", "discount", "deal", "special",
                                 "sale", "bundle", "package", "combo", "cheap"};
        for (String keyword : offerKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsRecommendationRequest(String message) {
        String[] recKeywords = {"recommend", "suggest", "what should", "what to eat",
                               "advice", "idea", "option", "choice", "best"};
        for (String keyword : recKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsDietaryRequest(String message) {
        String[] dietaryKeywords = {"vegetarian", "vegan", "halal", "kosher",
                                   "gluten-free", "dairy-free", "nut-free",
                                   "healthy", "low calorie", "low carb", "keto"};
        for (String keyword : dietaryKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsPriceRequest(String message) {
        String[] priceKeywords = {"cheap", "expensive", "affordable", "budget",
                                 "price", "cost", "rp", "rupiah", "$"};
        for (String keyword : priceKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsGreeting(String message) {
        String[] greetings = {"hello", "hi", "hey", "good morning", "good afternoon",
                             "good evening", "how are you", "what's up"};
        for (String greeting : greetings) {
            if (message.contains(greeting)) {
                return true;
            }
        }
        return false;
    }
    
    private ChatResponse handleRegionalRequest(String message) {
        CuisineRegion region = storeSystem.findCuisineRegion(message);
        if (region != null) {
            List<StoreItem> regionalItems = storeSystem.getRegionalRecommendations(region.getId(), 6);
            if (!regionalItems.isEmpty()) {
                String responseMsg = String.format("🍽️ Here are the best %s cuisine recommendations for you! " +
                    "These are authentic dishes from %s restaurants near President University.",
                    region.getName(), region.getName());
                
                ChatResponse response = new ChatResponse(responseMsg, RecommendationType.STORE_RECOMMENDATION);
                response.setStoreItems(regionalItems);
                return response;
            }
        }
        
        // Fallback to search
        return handleSearchRequest(message);
    }
    
    private ChatResponse handleFoodTypeRequest(String message) {
        List<StoreItem> items = storeSystem.searchItems(message);
        if (!items.isEmpty()) {
            // Create custom package based on food type
            String foodType = extractFoodType(message);
            List<StoreItem> bestItems = items.stream()
                .limit(4)
                .collect(Collectors.toList());
            
            if (bestItems.size() >= 2) {
                String packageName = foodType.substring(0, 1).toUpperCase() + foodType.substring(1) + " Lovers Package";
                String responseMsg = String.format("🌶️ I've created a special '%s' package for you! " +
                    "Enjoy these selected dishes that match your taste preference.",
                    packageName);
                
                ChatResponse response = new ChatResponse(responseMsg, RecommendationType.CUSTOM_PACKAGE);
                response.setStoreItems(bestItems);
                return response;
            }
        }
        
        return handleSearchRequest(message);
    }
    
    private ChatResponse handleOfferRequest() {
        List<SpecialOffer> offers = storeSystem.getSpecialOffers();
        if (!offers.isEmpty()) {
            // Get the most relevant offer based on preferences
            SpecialOffer bestOffer = offers.get(0);
            for (SpecialOffer offer : offers) {
                if (offer.getOfferType().equals("FLASH_SALE") || offer.getDiscountPercent() > bestOffer.getDiscountPercent()) {
                    bestOffer = offer;
                }
            }
            
            ChatResponse response = new ChatResponse("", RecommendationType.SPECIAL_OFFER);
            response.setSpecialOffer(bestOffer);
            return response;
        }
        
        return new ChatResponse("We currently don't have any special offers, but check back soon!", 
                               RecommendationType.TEXT_RESPONSE);
    }
    
    private ChatResponse handlePersonalizedRecommendation() {
        // Get top 3 preferences
        List<String> topPreferences = preferenceWeights.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (topPreferences.isEmpty()) {
            topPreferences = Arrays.asList("popular", "best seller", "recommended");
        }
        
        String query = String.join(" ", topPreferences);
        List<StoreItem> items = storeSystem.searchItems(query);
        
        if (!items.isEmpty()) {
            String responseMsg = String.format("🎯 Based on your preferences, I recommend these dishes for you, %s!", 
                customerName);
            
            ChatResponse response = new ChatResponse(responseMsg, RecommendationType.STORE_RECOMMENDATION);
            response.setStoreItems(items.stream().limit(5).collect(Collectors.toList()));
            return response;
        }
        
        return new ChatResponse("I need to learn more about your preferences. Could you tell me what kind of food you like?", 
                               RecommendationType.TEXT_RESPONSE);
    }
    
    private ChatResponse handleDietaryRequest(String message) {
        String responseMsg = "I'll find options that match your dietary requirements! ";
        List<StoreItem> items = storeSystem.searchItems(message);
        
        if (!items.isEmpty()) {
            responseMsg += "Here are some suitable options:";
            ChatResponse response = new ChatResponse(responseMsg, RecommendationType.STORE_RECOMMENDATION);
            response.setStoreItems(items.stream().limit(4).collect(Collectors.toList()));
            return response;
        }
        
        return new ChatResponse("I couldn't find specific items matching your dietary requirements. " +
                               "Try being more specific or ask for our vegetarian/healthy options.", 
                               RecommendationType.TEXT_RESPONSE);
    }
    
    private ChatResponse handlePriceRequest(String message) {
        String priceRange = "affordable";
        if (message.contains("expensive") || message.contains("premium") || message.contains("luxury")) {
            priceRange = "premium";
        } else if (message.contains("cheap") || message.contains("low cost") || message.contains("budget")) {
            priceRange = "budget";
        }
        
        List<StoreItem> items = storeSystem.getItemsByPriceRange(priceRange);
        
        if (!items.isEmpty()) {
            String responseMsg = String.format("💰 Here are some %s options for you:", priceRange);
            ChatResponse response = new ChatResponse(responseMsg, RecommendationType.STORE_RECOMMENDATION);
            response.setStoreItems(items.stream().limit(5).collect(Collectors.toList()));
            return response;
        }
        
        return handleSearchRequest(message);
    }
    
    private ChatResponse handleSearchRequest(String query) {
        List<StoreItem> items = storeSystem.searchItems(query);
        
        if (!items.isEmpty()) {
            // Check if we can create a themed package
            Set<String> tags = storeSystem.parseQuery(query.toLowerCase());
            if (tags.size() >= 2) {
                List<StoreItem> packageItems = items.stream()
                    .limit(3)
                    .collect(Collectors.toList());
                
                if (packageItems.size() >= 2) {
                    String theme = createThemeFromTags(tags);
                    String responseMsg = String.format("🎪 I've created a '%s' themed package for you based on your search!", theme);
                    
                    ChatResponse response = new ChatResponse(responseMsg, RecommendationType.CUSTOM_PACKAGE);
                    response.setStoreItems(packageItems);
                    return response;
                }
            }
            
            String responseMsg = "Here are some recommendations based on your search:";
            ChatResponse response = new ChatResponse(responseMsg, RecommendationType.STORE_RECOMMENDATION);
            response.setStoreItems(items.stream().limit(6).collect(Collectors.toList()));
            return response;
        } else {
            return new ChatResponse("I couldn't find specific items for that. Try asking for: 'spicy food', 'sweet desserts', " +
                                   "'Korean cuisine', 'vegetarian options', or ask about our special offers!", 
                                   RecommendationType.TEXT_RESPONSE);
        }
    }
    
    private String generateGreeting() {
        String[] greetings = {
            String.format("Hello %s! 👋 I'm your food assistant. How can I help you today?", customerName),
            String.format("Hi %s! 🍽️ Ready to explore delicious food options?", customerName),
            String.format("Welcome %s! 😊 What type of cuisine are you craving today?", customerName),
            String.format("Hey there %s! 🌮 Looking for something tasty to eat?", customerName)
        };
        return greetings[new Random().nextInt(greetings.length)];
    }
    
    private String extractFoodType(String message) {
        String[] types = {"spicy", "sweet", "sour", "salty", "savory", "fried", "grilled", "steamed"};
        for (String type : types) {
            if (message.contains(type)) {
                return type;
            }
        }
        return "delicious";
    }
    
    private String createThemeFromTags(Set<String> tags) {
        if (tags.contains("spicy")) return "Spicy Adventure";
        if (tags.contains("sweet")) return "Sweet Treats";
        if (tags.contains("healthy")) return "Healthy Choice";
        if (tags.contains("fried")) return "Crispy Delights";
        return "Special Selection";
    }
}

// =============================== 
// SIMPLIFIED MULTI-STORE SYSTEM
// =============================== 

class MultiStoreSystem {
    private final List<Store> stores;
    private final Map<String, List<MenuItem>> storeMenus;
    private final Map<String, String> synonyms;
    private final List<CuisineRegion> cuisineRegions;
    private final List<SpecialOffer> activeOffers;
    private final List<CustomPackage> customPackages;

    public MultiStoreSystem() {
        stores = new ArrayList<>();
        storeMenus = new HashMap<>();
        synonyms = new HashMap<>();
        cuisineRegions = new ArrayList<>();
        activeOffers = new ArrayList<>();
        customPackages = new ArrayList<>();
        
        initializeCuisineRegions();
        initializeSynonyms();
        initializeStores();
        initializeMenus();
        initializeSpecialOffers();
        initializeCustomPackages();
    }

    private void initializeCuisineRegions() {
        cuisineRegions.add(new CuisineRegion("INDONESIAN", "Indonesian", 
            "Authentic Indonesian cuisine with rich flavors and spices",
            "indonesian", "spicy", "rice", "noodle", "traditional", "savory"));
        
        cuisineRegions.add(new CuisineRegion("WESTERN", "Western", 
            "Western-style food including burgers, pasta, and steaks",
            "western", "burger", "pasta", "steak", "cheese", "fries"));
        
        cuisineRegions.add(new CuisineRegion("KOREAN", "Korean", 
            "Korean street food and traditional dishes",
            "korean", "kimchi", "spicy", "rice cake", "fried chicken", "bbq"));
        
        cuisineRegions.add(new CuisineRegion("JAPANESE", "Japanese", 
            "Japanese cuisine including sushi, ramen, and bento",
            "japanese", "sushi", "ramen", "bento", "tempura", "miso"));
        
        cuisineRegions.add(new CuisineRegion("CHINESE", "Chinese", 
            "Chinese dishes from various regions",
            "chinese", "dimsum", "noodle", "dumpling", "stir fry", "wok"));
        
        cuisineRegions.add(new CuisineRegion("THAI", "Thai", 
            "Thai cuisine with balanced sweet, sour, and spicy flavors",
            "thai", "spicy", "sweet", "sour", "coconut", "lemongrass"));
        
        cuisineRegions.add(new CuisineRegion("MIDDLE_EASTERN", "Middle Eastern", 
            "Middle Eastern and Arabic cuisine",
            "middle eastern", "arabic", "shawarma", "kebab", "hummus", "falafel"));
        
        cuisineRegions.add(new CuisineRegion("DESSERTS", "Desserts", 
            "Sweet treats and desserts from around the world",
            "dessert", "sweet", "cake", "ice cream", "chocolate", "pastry"));
    }

    private void initializeStores() {
        // Indonesian Cuisine
        stores.add(new Store("STORE001", "Warung Nasi Padang Sederhana", 
            "Authentic Padang cuisine from West Sumatra", 4.7, 0.3,
            getCuisineRegion("INDONESIAN"), 25, 5000,
            "spicy", "coconut", "beef", "traditional", "rice"));
        
        stores.add(new Store("STORE002", "Warung Es Teh Indonesia", 
            "Specialist in traditional Indonesian drinks", 4.6, 0.5,
            getCuisineRegion("INDONESIAN"), 20, 4000,
            "drink", "sweet", "refreshing", "traditional", "cold"));
        
        // Western Cuisine
        stores.add(new Store("STORE003", "Burger & Pasta Station", 
            "Western food at affordable prices", 4.4, 0.9,
            getCuisineRegion("WESTERN"), 35, 8000,
            "burger", "pasta", "cheese", "western", "fries"));
        
        // Korean Cuisine
        stores.add(new Store("STORE004", "Korean Street Food", 
            "Authentic Korean street food", 4.6, 1.2,
            getCuisineRegion("KOREAN"), 30, 7000,
            "korean", "spicy", "street food", "rice cake", "kimchi"));
        
        // Japanese Cuisine
        stores.add(new Store("STORE005", "Sushi Master", 
            "Fresh sushi and Japanese dishes", 4.7, 1.0,
            getCuisineRegion("JAPANESE"), 30, 8000,
            "sushi", "japanese", "fresh", "seafood", "rice"));
        
        // Chinese Cuisine
        stores.add(new Store("STORE006", "Dimsum Palace", 
            "Traditional Chinese dimsum and dishes", 4.6, 0.8,
            getCuisineRegion("CHINESE"), 30, 6000,
            "dimsum", "steamed", "chinese", "dumpling", "bite-sized"));
        
        // Thai Cuisine
        stores.add(new Store("STORE007", "Thai Street Kitchen", 
            "Authentic Thai street food", 4.7, 1.3,
            getCuisineRegion("THAI"), 30, 8000,
            "thai", "spicy", "sweet", "sour", "coconut"));
        
        // Desserts
        stores.add(new Store("STORE008", "Sweet Dessert House", 
            "Various desserts and sweet treats", 4.9, 1.5,
            getCuisineRegion("DESSERTS"), 20, 5000,
            "dessert", "sweet", "cake", "ice cream", "chocolate"));
        
        stores.add(new Store("STORE009", "Es Kepal Milo Corner", 
            "Signature Milo and Thai tea drinks", 4.8, 0.8,
            getCuisineRegion("DESSERTS"), 15, 4000,
            "drink", "sweet", "chocolate", "cold", "creamy"));
    }

    private void initializeMenus() {
        // STORE001 - Warung Nasi Padang Sederhana
        List<MenuItem> padangMenu = new ArrayList<>();
        padangMenu.add(new MenuItem("S1I001", "Beef Rendang", 25000, "main", 
            "Slow-cooked beef in coconut milk and spices", 4, false, true, 450,
            "spicy", "beef", "coconut", "indonesian", "traditional", "rice"));
        padangMenu.add(new MenuItem("S1I002", "Ayam Pop", 20000, "main", 
            "Boiled chicken with Padang spices", 2, false, true, 350,
            "chicken", "indonesian", "traditional", "savory", "rice"));
        storeMenus.put("STORE001", padangMenu);
        
        // STORE002 - Warung Es Teh Indonesia
        List<MenuItem> drinkMenu = new ArrayList<>();
        drinkMenu.add(new MenuItem("S2I001", "Es Teh Manis", 5000, "drink", 
            "Sweet iced tea", 0, true, true, 100,
            "drink", "sweet", "ice", "cold", "refreshing"));
        drinkMenu.add(new MenuItem("S2I002", "Es Jeruk", 8000, "drink", 
            "Fresh orange juice", 0, true, true, 120,
            "drink", "sweet", "sour", "cold", "fresh", "fruit"));
        storeMenus.put("STORE002", drinkMenu);
        
        // STORE003 - Burger & Pasta Station
        List<MenuItem> westernMenu = new ArrayList<>();
        westernMenu.add(new MenuItem("S3I001", "Beef Burger", 28000, "main", 
            "Juicy beef burger with cheese", 1, false, true, 550,
            "burger", "beef", "cheese", "western", "savory"));
        westernMenu.add(new MenuItem("S3I002", "Carbonara Pasta", 30000, "main", 
            "Creamy pasta with bacon", 1, false, true, 480,
            "pasta", "creamy", "cheese", "western", "savory"));
        storeMenus.put("STORE003", westernMenu);
        
        // STORE004 - Korean Street Food
        List<MenuItem> koreanMenu = new ArrayList<>();
        koreanMenu.add(new MenuItem("S4I001", "Tteokbokki", 25000, "main", 
            "Spicy rice cakes", 4, true, true, 320,
            "korean", "spicy", "rice cake", "street food", "savory"));
        koreanMenu.add(new MenuItem("S4I002", "Korean Fried Chicken", 30000, "main", 
            "Crispy fried chicken with sweet spicy sauce", 3, false, true, 600,
            "korean", "chicken", "fried", "spicy", "sweet", "crispy"));
        storeMenus.put("STORE004", koreanMenu);
        
        // Add more menus for other stores...
        // For simplicity, we'll add minimal menus for now
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
        synonyms.put("china", "chinese");
        synonyms.put("thai", "thai");
        synonyms.put("arab", "arabic");
        synonyms.put("timur tengah", "middle eastern");
        synonyms.put("italia", "italian");
        synonyms.put("barat", "western");
        synonyms.put("murah", "cheap");
        synonyms.put("mahal", "expensive");
        synonyms.put("sehat", "healthy");
        synonyms.put("vegetarian", "vegetarian");
        synonyms.put("halal", "halal");
    }

    private void initializeSpecialOffers() {
        // For now, create empty offers - can be populated later
    }

    private void initializeCustomPackages() {
        // For now, create empty packages - can be populated later
    }

    public List<StoreItem> searchItems(String query) {
        Set<String> queryTags = parseQuery(query.toLowerCase());
        List<StoreItem> results = new ArrayList<>();
        
        for (Store store : stores) {
            if (!store.isOpen()) continue;
            
            List<MenuItem> menu = storeMenus.get(store.getId());
            if (menu == null) continue;
            
            for (MenuItem item : menu) {
                if (queryTags.isEmpty() || item.hasAnyTag(queryTags)) {
                    // Simple logic for best seller and recommended
                    boolean isBestSeller = item.getName().toLowerCase().contains("rendang") || 
                                          item.getName().toLowerCase().contains("burger");
                    boolean isRecommended = store.getRating() >= 4.5;
                    int popularityScore = new Random().nextInt(100);
                    
                    results.add(new StoreItem(store, item, isBestSeller, isRecommended, popularityScore));
                }
            }
        }
        
        // Simple sort by rating and distance
        results.sort((a, b) -> {
            double scoreA = a.getStore().getRating() * 10 - a.getStore().getDistanceKm();
            double scoreB = b.getStore().getRating() * 10 - b.getStore().getDistanceKm();
            return Double.compare(scoreB, scoreA);
        });
        
        return results.stream().limit(8).collect(Collectors.toList());
    }

    public Set<String> parseQuery(String query) {
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
    
    public String getTagFromSynonym(String word) {
        return synonyms.get(word.toLowerCase());
    }
    
    public CuisineRegion findCuisineRegion(String query) {
        for (CuisineRegion region : cuisineRegions) {
            if (region.matchesQuery(query)) {
                return region;
            }
        }
        return null;
    }
    
    public List<StoreItem> getRegionalRecommendations(String regionId, int limit) {
        List<StoreItem> results = new ArrayList<>();
        
        for (Store store : stores) {
            if (store.getCuisineRegion().getId().equals(regionId) && store.isOpen()) {
                List<MenuItem> menu = storeMenus.get(store.getId());
                if (menu != null && !menu.isEmpty()) {
                    // Take first item from menu as representative
                    results.add(new StoreItem(store, menu.get(0), true, true, 80));
                }
            }
        }
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }
    
    public List<StoreItem> getItemsByPriceRange(String priceRange) {
        List<StoreItem> results = new ArrayList<>();
        int maxPrice;
        
        switch (priceRange.toLowerCase()) {
            case "budget":
                maxPrice = 20000;
                break;
            case "affordable":
                maxPrice = 40000;
                break;
            case "premium":
                maxPrice = 100000;
                break;
            default:
                maxPrice = 50000;
        }
        
        for (Store store : stores) {
            if (!store.isOpen()) continue;
            
            List<MenuItem> menu = storeMenus.get(store.getId());
            if (menu == null) continue;
            
            for (MenuItem item : menu) {
                if (item.getPrice() <= maxPrice) {
                    results.add(new StoreItem(store, item, false, true, 50));
                }
            }
        }
        
        results.sort((a, b) -> Integer.compare(a.getMenuItem().getPrice(), b.getMenuItem().getPrice()));
        return results.stream().limit(10).collect(Collectors.toList());
    }
    
    public List<SpecialOffer> getSpecialOffers() {
        return new ArrayList<>(activeOffers);
    }
    
    public List<CustomPackage> getCustomPackages() {
        return new ArrayList<>(customPackages);
    }
    
    public List<Store> getNearbyStores(int limit) {
        return stores.stream()
            .filter(Store::isOpen)
            .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private CuisineRegion getCuisineRegion(String id) {
        return cuisineRegions.stream()
            .filter(region -> region.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    public List<Store> getAllStores() {
        return new ArrayList<>(stores);
    }
    
    public Store getStoreById(String id) {
        return stores.stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}

// =============================== 
// SIMPLIFIED BUYER WINDOW
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
        } else if (message.getType() == MessageType.CUSTOM_PACKAGE) {
            JPanel packagePanel = createPackagePanel(message);
            chatContainer.add(packagePanel);
            chatContainer.revalidate();
            chatContainer.repaint();
            return;
        } else if (message.getType() == MessageType.ORDER_UPDATE) {
            JPanel orderPanel = createOrderUpdatePanel(message);
            chatContainer.add(orderPanel);
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
        if (message.getStoreItems() != null) {
            for (StoreItem storeItem : message.getStoreItems()) {
                panel.add(createStoreItemCard(storeItem));
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
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
            shoppingCart.addItem(storeItem, 1, "");
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
        if (offer == null) return panel;
        
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
            if (offer.getItems() != null) {
                for (StoreItem item : offer.getItems()) {
                    shoppingCart.addItem(item, 1, "");
                }
                updateCartDisplay();
                JOptionPane.showMessageDialog(this,
                    String.format("Added %s to cart!\nYou save Rp %,d!", 
                        offer.getTitle(), offer.getSavings()),
                    "Special Offer Added",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        offerCard.add(infoPanel, BorderLayout.CENTER);
        offerCard.add(addBtn, BorderLayout.EAST);
        
        panel.add(offerCard);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, offerCard.getPreferredSize().height + 20));
        
        return panel;
    }
    
    private JPanel createPackagePanel(ChatMessage message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel headerLabel = new JLabel("🎪 " + message.getMessage());
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(headerLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        if (message.getStoreItems() != null) {
            for (StoreItem storeItem : message.getStoreItems()) {
                panel.add(createStoreItemCard(storeItem));
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }
    
    private JPanel createOrderUpdatePanel(ChatMessage message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel orderCard = new JPanel(new BorderLayout(15, 0));
        orderCard.setBackground(new Color(240, 250, 255));
        orderCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 180, 250), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel orderLabel = new JLabel("📦 " + message.getMessage());
        orderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        orderCard.add(orderLabel, BorderLayout.CENTER);
        
        panel.add(orderCard);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, orderCard.getPreferredSize().height + 20));
        
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
            contentPanel.add(createSummaryRow("Subtotal:", shoppingCart.getSubtotal()));
            contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            contentPanel.add(createSummaryRow("Delivery:", shoppingCart.getDeliveryFee()));
            contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
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
            chatBridge.placeOrder(order);
            
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
// SIMPLIFIED SELLER WINDOW
// =============================== 

class SellerWindow extends JFrame implements ChatListener {
    private JPanel chatContainer;
    private JTextField responseField;
    private ChatBridge chatBridge;
    private JScrollPane chatScroll;

    public SellerWindow(ChatBridge chatBridge) {
        this.chatBridge = chatBridge;
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
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel infoLabel = new JLabel("<html><center>Special offers will be generated<br>automatically by the AI chatbot.</center></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messagePanel.add(infoLabel, BorderLayout.CENTER);
        
        dialog.add(messagePanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okBtn);
        
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