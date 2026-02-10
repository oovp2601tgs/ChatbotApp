# Food Ordering Chatbot System

## 🍽️ Overview
A Java-based dual-window food ordering system with AI-powered chatbot recommendations. The system connects buyers and sellers in real-time with intelligent food suggestions based on cuisine preferences, dietary restrictions, and location.

## ✨ Key Features

### 🤖 AI-Powered Recommendations
- Smart chatbot understands natural language queries
- Regional cuisine detection (Indonesian, Korean, Japanese, Chinese, Thai, Western)
- Dietary preference learning (vegetarian, halal, spicy levels)
- Personalized suggestions based on conversation history

### 🛒 Shopping Experience
- Multi-store shopping cart
- Real-time cart updates
- Quantity adjustments
- Special instructions per item
- Delivery fee calculation

### 💬 Real-Time Chat
- Dual-window interface (Buyer & Seller)
- Message history tracking
- Multiple message types:
  - Text messages
  - Store recommendations
  - Special offers
  - Order updates
  - Custom packages

### 🏪 Multi-Store System
- 9 different restaurants/cuisines
- Distance-based recommendations from President University
- Rating and popularity scores
- Delivery time estimates

## 🚀 Quick Start

### Prerequisites
- Java 8 or higher
- Swing/AWT libraries (included in Java SE)

### Running the Application
```bash
# Compile
javac IntegratedChatbotApp.java

# Run
java IntegratedChatbotApp
```

## 🎯 How It Works

1. **Buyer Window** - Customers chat with AI bot to find food
2. **AI Processing** - Chatbot analyzes requests and provides recommendations
3. **Order Building** - Add items to cart from multiple restaurants
4. **Checkout** - Fill delivery details and place order
5. **Seller Window** - View incoming orders and chat history

## 💡 Example Queries
- "I want spicy Korean food"
- "Show me vegetarian options"
- "What are today's special offers?"
- "I need something sweet"
- "Recommend Indonesian food"

## 📁 Project Structure
```
IntegratedChatbotApp.java
├── ChatBridge           # Message routing between windows
├── FoodChatbotAI        # Intelligent recommendation engine
├── MultiStoreSystem     # Restaurant and menu management
├── ShoppingCart         # Cart functionality
├── BuyerChatWindow      # Customer interface
├── SellerWindow         # Seller dashboard
└── Data Models          # Order, Store, MenuItem, etc.
```

## 🔧 Technical Details
- **Language**: Java
- **GUI**: Swing/AWT
- **Patterns**: Observer pattern for chat updates
- **AI Logic**: Tag-based matching with preference weights
- **Multi-threading**: Timer-based delayed responses

## 🎨 UI Features
- Modern chat bubbles with timestamps
- Responsive cart panel
- Visual food cards with store info
- Order status indicators
- Real-time updates

## 📋 Order Flow
1. Customer query → AI response
2. Add items → Shopping cart
3. Checkout → Order creation
4. Order confirmation → Seller notification
5. Status updates → Real-time tracking

## 🌟 Future Enhancements
- Payment gateway integration
- User authentication
- Order history
- Rating system
- Mobile app version
- Database persistence
