# Flight Booking App

A modern Android flight booking application built with Jetpack Compose, inspired by Air Canada's mobile app design.

## 📱 Features

### 🎫 Flight Booking
- **Trip Type Selection**: Choose between One-way, Round-trip, and Multi-city flights
- **Airport Selection**: Search and select departure and arrival airports from a comprehensive list
- **Date Picker**: Native Android DatePickerDialog for selecting travel dates
- **Passenger Selection**: Flexible passenger count selection (adults and children)
- **Cabin Class**: Choose from Economy, Premium Economy, Business, and First Class
- **Flight Search**: Search for available flights based on your criteria
- **Flight Results**: View detailed flight options with pricing and timing
- **Seat Selection**: Interactive seat map for choosing your preferred seat
- **Passenger Information**: Enter passenger details for booking
- **Payment Processing**: Secure payment interface for completing bookings
- **Booking Confirmation**: Detailed confirmation screen with booking reference

### 🗺️ Airport Navigation
- **Gate Finder**: Search for gates with flexible query support (e.g., "A1", "gate a1", "Gate A1")
- **Terminal Map**: Interactive airport terminal map with floor selection
- **Turn-by-Turn Navigation**: Visual route guidance with:
  - Curved path visualization using Bézier curves
  - Step-by-step directions
  - Distance and time estimates
  - Current location tracking
  - Compass rose showing N/E/S/W directions
  - Nearby landmarks and amenities along the route
  - Scrollable interface for easy viewing
- **Amenities List**: Browse airport facilities including:
  - Restaurants and cafes
  - Shops and duty-free
  - Lounges
  - Restrooms
  - ATMs and currency exchange
  - Information desks
- **Airport Hub**: Quick access to all airport services and information

### 👤 Profile & Settings
- **User Profile**: View and manage your profile information
  - Personal details
  - Aeroplan membership
  - Travel preferences
  - Saved payment methods
  - Booking history
- **Settings**: Customize app preferences
  - Notifications
  - Language selection
  - Theme preferences
  - Privacy settings
  - Help & support

### 🎨 UI/UX Features
- **Material Design 3**: Modern, clean interface following Material Design guidelines
- **Bottom Navigation**: Easy access to main sections (Book, Airport Nav, Profile, Settings)
- **Responsive Design**: Optimized for various screen sizes
- **Smooth Animations**: Polished transitions and interactions
- **Dark/Light Theme Support**: Comfortable viewing in any lighting condition

## 🏗️ Technical Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **State Management**: StateFlow and Compose State
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

### Key Components

#### ViewModels
- `BookingViewModel`: Manages flight booking state and logic
- `AirportNavigationViewModel`: Handles airport navigation, gate finding, and route calculation

#### Navigation
- Centralized navigation graph with shared ViewModels
- Deep linking support for direct screen access
- Back stack management

#### UI Components
- Reusable composables for consistent design
- Custom components for specialized features (date picker, passenger selector, etc.)
- Canvas-based drawing for maps and routes

### Project Structure
```
FlightBooking/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/flightbooking/
│   │   │   │   ├── data/
│   │   │   │   │   └── models/          # Data models
│   │   │   │   ├── navigation/          # Navigation setup
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/      # Reusable UI components
│   │   │   │   │   ├── screens/         # Screen composables
│   │   │   │   │   └── theme/           # Theme and styling
│   │   │   │   └── viewmodel/           # ViewModels
│   │   │   └── res/                     # Resources (drawables, strings, etc.)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34
- Gradle 8.2 or later

### Building the Project
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

```bash
./gradlew build
```

### Running the App
```bash
./gradlew installDebug
```

## 📋 Screens Overview

1. **Booking Screen**: Main flight search and booking interface
2. **Flight Results**: List of available flights
3. **Flight Details**: Detailed information about a selected flight
4. **Seat Selection**: Interactive seat map
5. **Passenger Form**: Enter passenger information
6. **Payment**: Secure payment processing
7. **Booking Confirmation**: Confirmation and booking details
8. **Airport Hub**: Central hub for airport services
9. **Gate Finder**: Search and locate gates
10. **Terminal Map**: Interactive airport map
11. **Navigation**: Turn-by-turn directions
12. **Amenities List**: Browse airport facilities
13. **Profile**: User profile and preferences
14. **Settings**: App settings and preferences

## 🎯 Key Features Highlights

### Smart Gate Search
The gate finder supports flexible search queries:
- Direct gate number: "A1"
- With prefix: "gate A1"
- Case-insensitive: "a1", "A1", "Gate a1"

### Advanced Navigation
- Automatic location initialization
- Real-time route calculation
- Visual path rendering with smooth curves
- Nearby amenity detection (within 100m of route)
- Compass orientation
- Step-by-step progress tracking

### User-Friendly Booking
- Native Android date picker
- Intuitive passenger selection
- Clear pricing display
- Streamlined checkout process

## 🔧 Configuration

### Customization
- Colors and themes: `ui/theme/Color.kt` and `ui/theme/Theme.kt`
- String resources: `res/values/strings.xml`
- Navigation routes: `navigation/NavGraph.kt`

## 📝 License

This project is created for educational and demonstration purposes.

## 👨‍💻 Development

Built with ❤️ using Jetpack Compose and modern Android development practices.

---

**Note**: This is a demonstration app and does not process real flight bookings or payments.