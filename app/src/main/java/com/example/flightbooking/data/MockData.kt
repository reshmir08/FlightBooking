package com.example.flightbooking.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.flightbooking.data.models.*
import java.time.LocalDateTime
import java.time.Duration

/**
 * Mock data for testing and development
 */
object MockData {

    // Airlines
    val airlines = listOf(
        Airline("AC", "Air Canada", null, 4.2f),
        Airline("UA", "United Airlines", null, 4.0f),
        Airline("AA", "American Airlines", null, 3.9f),
        Airline("DL", "Delta Air Lines", null, 4.1f),
        Airline("BA", "British Airways", null, 4.3f),
        Airline("LH", "Lufthansa", null, 4.4f)
    )

    // Airports
    val airports = listOf(
        Airport("YYZ", "Toronto Pearson International", "Toronto", "Canada", 43.6777, -79.6248),
        Airport("JFK", "John F. Kennedy International", "New York", "USA", 40.6413, -73.7781),
        Airport("LAX", "Los Angeles International", "Los Angeles", "USA", 33.9416, -118.4085),
        Airport("LHR", "London Heathrow", "London", "UK", 51.4700, -0.4543),
        Airport("CDG", "Charles de Gaulle", "Paris", "France", 49.0097, 2.5479),
        Airport("DXB", "Dubai International", "Dubai", "UAE", 25.2532, 55.3657),
        Airport("SIN", "Singapore Changi", "Singapore", "Singapore", 1.3644, 103.9915),
        Airport("HND", "Tokyo Haneda", "Tokyo", "Japan", 35.5494, 139.7798)
    )

    // Sample flights
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateSampleFlights(
        from: Airport = airports[0],
        to: Airport = airports[1],
        departureDate: LocalDateTime = LocalDateTime.now().plusDays(7)
    ): List<Flight> {
        val flights = mutableListOf<Flight>()

        // Direct flight
        flights.add(
            Flight(
                id = "FL001",
                flightNumber = "AC101",
                airline = airlines[0],
                segments = listOf(
                    FlightSegment(
                        flightNumber = "AC101",
                        airline = airlines[0],
                        origin = from,
                        destination = to,
                        departureTime = departureDate.withHour(8).withMinute(0),
                        arrivalTime = departureDate.withHour(10).withMinute(30),
                        duration = 150, // 2 hours 30 minutes in minutes
                        aircraft = "Boeing 787",
                        cabinClass = CabinClass.ECONOMY
                    )
                ),
                price = 299.99,
                availableSeats = 45,
                baggageInfo = BaggageInfo(carryOnAllowed = 1, checkedBagsIncluded = 1),
                amenities = listOf("WiFi", "In-flight Entertainment", "Meals"),
                isRefundable = false,
                carbonEmissions = 150.0,
                totalDuration = 150
            )
        )

        // One-stop flight
        flights.add(
            Flight(
                id = "FL002",
                flightNumber = "UA205",
                airline = airlines[1],
                segments = listOf(
                    FlightSegment(
                        flightNumber = "UA205",
                        airline = airlines[1],
                        origin = from,
                        destination = airports[2],
                        departureTime = departureDate.withHour(10).withMinute(15),
                        arrivalTime = departureDate.withHour(12).withMinute(0),
                        duration = 105, // 1 hour 45 minutes
                        aircraft = "Airbus A320"
                    ),
                    FlightSegment(
                        flightNumber = "UA306",
                        airline = airlines[1],
                        origin = airports[2],
                        destination = to,
                        departureTime = departureDate.withHour(14).withMinute(30),
                        arrivalTime = departureDate.withHour(17).withMinute(15),
                        duration = 165, // 2 hours 45 minutes
                        aircraft = "Boeing 737"
                    )
                ),
                price = 249.99,
                availableSeats = 23,
                baggageInfo = BaggageInfo(carryOnAllowed = 1, checkedBagsIncluded = 1),
                amenities = listOf("WiFi", "Snacks"),
                isRefundable = true,
                carbonEmissions = 180.0,
                totalDuration = 420 // 7 hours total including layover
            )
        )

        // Premium flight
        flights.add(
            Flight(
                id = "FL003",
                flightNumber = "BA401",
                airline = airlines[4],
                segments = listOf(
                    FlightSegment(
                        flightNumber = "BA401",
                        airline = airlines[4],
                        origin = from,
                        destination = to,
                        departureTime = departureDate.withHour(14).withMinute(0),
                        arrivalTime = departureDate.withHour(16).withMinute(45),
                        duration = 165, // 2 hours 45 minutes
                        aircraft = "Airbus A350",
                        cabinClass = CabinClass.BUSINESS
                    )
                ),
                price = 899.99,
                availableSeats = 12,
                baggageInfo = BaggageInfo(carryOnAllowed = 2, checkedBagsIncluded = 2, checkedBagWeight = "32 kg"),
                amenities = listOf("WiFi", "Premium Meals", "Lounge Access", "Lie-flat Seats"),
                isRefundable = true,
                carbonEmissions = 120.0,
                cabinClass = CabinClass.BUSINESS,
                totalDuration = 165
            )
        )

        // Budget flight
        flights.add(
            Flight(
                id = "FL004",
                flightNumber = "DL502",
                airline = airlines[3],
                segments = listOf(
                    FlightSegment(
                        flightNumber = "DL502",
                        airline = airlines[3],
                        origin = from,
                        destination = to,
                        departureTime = departureDate.withHour(6).withMinute(30),
                        arrivalTime = departureDate.withHour(9).withMinute(0),
                        duration = 150, // 2 hours 30 minutes
                        aircraft = "Boeing 737"
                    )
                ),
                price = 199.99,
                availableSeats = 67,
                baggageInfo = BaggageInfo(carryOnAllowed = 1, checkedBagsIncluded = 0),
                amenities = listOf("Basic Snacks"),
                isRefundable = false,
                carbonEmissions = 160.0,
                totalDuration = 150
            )
        )

        return flights
    }

    // Terminal and amenities data
    fun getSampleTerminal(): Terminal {
        val terminal = Terminal(
            id = "T1",
            name = "Terminal 1",
            airport = airports[0],
            gates = getSampleGates(),
            amenities = getSampleAmenities()
        )
        return terminal
    }

    fun getSampleGates(): List<Gate> {
        return listOf(
            // Terminal 1 - Gates A1-A10
            Gate("G1", "A1", "T1", Position(100f, 150f, 1), GateStatus.BOARDING, "AC101", "09:30"),
            Gate("G2", "A2", "T1", Position(150f, 150f, 1), GateStatus.AVAILABLE),
            Gate("G3", "A3", "T1", Position(200f, 150f, 1), GateStatus.DEPARTED, "UA205"),
            Gate("G4", "A4", "T1", Position(250f, 150f, 1), GateStatus.DELAYED, "BA401", "11:45"),
            Gate("G5", "A5", "T1", Position(300f, 150f, 1), GateStatus.AVAILABLE),
            Gate("G6", "A6", "T1", Position(350f, 150f, 1), GateStatus.BOARDING, "DL502", "14:20"),
            Gate("G7", "A7", "T1", Position(400f, 150f, 1), GateStatus.AVAILABLE),
            Gate("G8", "A8", "T1", Position(450f, 150f, 1), GateStatus.BOARDING, "AC456", "10:15"),
            Gate("G9", "A9", "T1", Position(500f, 150f, 1), GateStatus.MAINTENANCE),
            Gate("G10", "A10", "T1", Position(550f, 150f, 1), GateStatus.AVAILABLE),

            // Terminal 1 - Gates B1-B10
            Gate("G11", "B1", "T1", Position(100f, 350f, 1), GateStatus.BOARDING, "AC789", "15:30"),
            Gate("G12", "B2", "T1", Position(150f, 350f, 1), GateStatus.DELAYED, "UA123", "16:00"),
            Gate("G13", "B3", "T1", Position(200f, 350f, 1), GateStatus.AVAILABLE),
            Gate("G14", "B4", "T1", Position(250f, 350f, 1), GateStatus.BOARDING, "AC567", "12:45"),
            Gate("G15", "B5", "T1", Position(300f, 350f, 1), GateStatus.AVAILABLE),
            Gate("G16", "B6", "T1", Position(350f, 350f, 1), GateStatus.DEPARTED, "DL890"),
            Gate("G17", "B7", "T1", Position(400f, 350f, 1), GateStatus.AVAILABLE),
            Gate("G18", "B8", "T1", Position(450f, 350f, 1), GateStatus.BOARDING, "BA234", "13:20"),
            Gate("G19", "B9", "T1", Position(500f, 350f, 1), GateStatus.AVAILABLE),
            Gate("G20", "B10", "T1", Position(550f, 350f, 1), GateStatus.AVAILABLE),

            )
    }

    fun getSampleAmenities(): List<Amenity> {
        val amenities = mutableListOf<Amenity>()

        // Restrooms (multiple locations)
        amenities.addAll(listOf(
            Amenity("REST1", "Restroom - Gate A", AmenityType.RESTROOM, Position(80f, 180f, 1), "T1",
                "Family restroom with changing facilities", true, true, rating = 4.2f),
            Amenity("REST2", "Restroom - Gate B", AmenityType.RESTROOM, Position(280f, 380f, 1), "T1",
                "Standard restroom", true, true, rating = 4.0f),
            Amenity("REST3", "Restroom - Upper Level", AmenityType.RESTROOM, Position(350f, 230f, 2), "T1",
                "Accessible restroom", true, true, rating = 4.3f),
            Amenity("REST4", "Restroom - Central", AmenityType.RESTROOM, Position(300f, 250f, 1), "T1",
                "Family restroom", true, true, rating = 4.1f)
        ))

        // Coffee Shops
        amenities.addAll(listOf(
            Amenity("CAFE1", "Starbucks", AmenityType.CAFE, Position(120f, 250f, 1), "T1",
                "Coffee and pastries", true, true, "5:00 AM - 11:00 PM", 4.5f),
            Amenity("CAFE2", "Tim Hortons", AmenityType.CAFE, Position(420f, 180f, 1), "T1",
                "Canadian coffee chain", true, true, "24/7", 4.3f),
            Amenity("CAFE3", "Second Cup", AmenityType.CAFE, Position(250f, 280f, 2), "T1",
                "Premium coffee", true, true, "6:00 AM - 10:00 PM", 4.2f)
        ))

        // Restaurants & Food Courts
        amenities.addAll(listOf(
            Amenity("FOOD1", "Food Court", AmenityType.FOOD_COURT, Position(200f, 300f, 1), "T1",
                "Multiple dining options", true, true, "6:00 AM - 10:00 PM", 4.0f),
            Amenity("REST1", "Sushi Bar", AmenityType.RESTAURANT, Position(380f, 320f, 1), "T1",
                "Fresh sushi and Japanese cuisine", true, true, "11:00 AM - 9:00 PM", 4.4f),
            Amenity("REST2", "Burger Joint", AmenityType.RESTAURANT, Position(480f, 280f, 1), "T1",
                "Gourmet burgers", true, true, "10:00 AM - 10:00 PM", 3.9f),
            Amenity("REST3", "Pizza Place", AmenityType.RESTAURANT, Position(150f, 420f, 1), "T1",
                "Fresh pizza", true, true, "11:00 AM - 11:00 PM", 4.1f)
        ))

        // Lounges
        amenities.addAll(listOf(
            Amenity("LOUNGE1", "Maple Leaf Lounge", AmenityType.LOUNGE, Position(250f, 200f, 1), "T1",
                "Premium lounge with complimentary food and beverages", true, true, "5:30 AM - 10:00 PM", 4.7f),
            Amenity("LOUNGE2", "Air Canada Signature Suite", AmenityType.LOUNGE, Position(450f, 250f, 2), "T1",
                "Luxury lounge for premium passengers", true, true, "5:00 AM - 11:00 PM", 4.8f),
            Amenity("LOUNGE3", "Plaza Premium Lounge", AmenityType.LOUNGE, Position(350f, 380f, 1), "T1",
                "Pay-per-use lounge", true, true, "24/7", 4.5f)
        ))

        // Charging Stations
        amenities.addAll(listOf(
            Amenity("CHARGE1", "Charging Station A", AmenityType.CHARGING_STATION, Position(130f, 180f, 1), "T1",
                "Free USB and power outlets", true, true, rating = 4.0f),
            Amenity("CHARGE2", "Charging Station B", AmenityType.CHARGING_STATION, Position(330f, 180f, 1), "T1",
                "Free charging ports", true, true, rating = 4.1f),
            Amenity("CHARGE3", "Charging Station C", AmenityType.CHARGING_STATION, Position(230f, 380f, 1), "T1",
                "Multiple device charging", true, true, rating = 3.9f),
            Amenity("CHARGE4", "Charging Station D", AmenityType.CHARGING_STATION, Position(430f, 380f, 1), "T1",
                "Fast charging available", true, true, rating = 4.2f),
            Amenity("CHARGE5", "Charging Station E", AmenityType.CHARGING_STATION, Position(300f, 280f, 2), "T1",
                "Upper level charging", true, true, rating = 4.0f)
        ))

        // Other Services
        amenities.addAll(listOf(
            Amenity("ATM1", "ATM", AmenityType.ATM, Position(180f, 280f, 1), "T1",
                "24/7 banking services", true, true, rating = 4.0f),
            Amenity("CURR1", "Currency Exchange", AmenityType.CURRENCY_EXCHANGE, Position(160f, 200f, 1), "T1",
                "Exchange foreign currency", true, true, "6:00 AM - 10:00 PM", 3.8f),
            Amenity("DUTY1", "Duty Free Shop", AmenityType.DUTY_FREE, Position(320f, 320f, 1), "T1",
                "Tax-free shopping", true, true, "6:00 AM - 11:00 PM", 4.3f),
            Amenity("PHARM1", "Pharmacy", AmenityType.PHARMACY, Position(220f, 220f, 1), "T1",
                "Over-the-counter medications", true, true, "7:00 AM - 9:00 PM", 4.1f),
            Amenity("MED1", "Medical Center", AmenityType.MEDICAL_CENTER, Position(140f, 320f, 1), "T1",
                "First aid and medical assistance", true, true, "24/7", 4.5f),
            Amenity("INFO1", "Information Desk", AmenityType.INFORMATION_DESK, Position(150f, 150f, 1), "T1",
                "Airport information and assistance", true, true, "24/7", 4.4f),
            Amenity("PRAY1", "Prayer Room", AmenityType.PRAYER_ROOM, Position(520f, 320f, 1), "T1",
                "Multi-faith prayer room", true, true, "24/7", 4.6f),
            Amenity("NURS1", "Nursing Room", AmenityType.NURSING_ROOM, Position(180f, 350f, 1), "T1",
                "Private nursing and baby care", true, true, "24/7", 4.5f),
            Amenity("PLAY1", "Kids Play Area", AmenityType.PLAY_AREA, Position(480f, 350f, 1), "T1",
                "Supervised play area for children", false, true, "6:00 AM - 10:00 PM", 4.2f)
        ))

        return amenities
    }

    /**
     * Sample geofence zones for Terminal 1.
     *
     * Zones are centred on existing mock gate/amenity positions so voice
     * announcements fire naturally during demo mode walkthroughs.
     */
    fun getSampleGeofenceZones(): List<GeofenceZone> = listOf(

        // ── Security checkpoints ──────────────────────────────────────────────
        GeofenceZone(
            id = "GF_SEC_1",
            name = "Security Checkpoint A",
            center = Position(300f, 250f, 1),
            radiusMeters = 30f,
            announcementMessage = "Security checkpoint ahead. Please have your boarding pass and ID ready.",
            type = GeofenceZoneType.SECURITY
        ),

        // ── Gates ─────────────────────────────────────────────────────────────
        GeofenceZone(
            id = "GF_GATE_A1",
            name = "Gate A1",
            center = Position(100f, 150f, 1),
            radiusMeters = 25f,
            announcementMessage = "You have arrived at Gate A1.",
            type = GeofenceZoneType.GATE
        ),
        GeofenceZone(
            id = "GF_GATE_A4",
            name = "Gate A4",
            center = Position(250f, 150f, 1),
            radiusMeters = 25f,
            announcementMessage = "Gate A4 is nearby. This gate is currently delayed.",
            type = GeofenceZoneType.GATE
        ),
        GeofenceZone(
            id = "GF_GATE_A6",
            name = "Gate A6",
            center = Position(350f, 150f, 1),
            radiusMeters = 25f,
            announcementMessage = "You have arrived at Gate A6. Boarding is in progress.",
            type = GeofenceZoneType.BOARDING
        ),
        GeofenceZone(
            id = "GF_GATE_B1",
            name = "Gate B1",
            center = Position(100f, 350f, 1),
            radiusMeters = 25f,
            announcementMessage = "You have arrived at Gate B1. Boarding is in progress.",
            type = GeofenceZoneType.BOARDING
        ),
        GeofenceZone(
            id = "GF_GATE_C1",
            name = "Gate C1 — Upper Level",
            center = Position(200f, 200f, 2),
            radiusMeters = 25f,
            announcementMessage = "You have arrived at Gate C1 on the upper level.",
            type = GeofenceZoneType.GATE
        ),

        // ── Lounges ───────────────────────────────────────────────────────────
        GeofenceZone(
            id = "GF_LOUNGE_1",
            name = "Maple Leaf Lounge",
            center = Position(250f, 200f, 1),
            radiusMeters = 30f,
            announcementMessage = "Maple Leaf Lounge entrance is nearby on your left.",
            type = GeofenceZoneType.LOUNGE
        ),
        GeofenceZone(
            id = "GF_LOUNGE_2",
            name = "Air Canada Signature Suite",
            center = Position(450f, 250f, 2),
            radiusMeters = 30f,
            announcementMessage = "Air Canada Signature Suite is ahead. Premium lounge for business and first class passengers.",
            type = GeofenceZoneType.LOUNGE
        ),

        // ── Restrooms ─────────────────────────────────────────────────────────
        GeofenceZone(
            id = "GF_REST_A",
            name = "Restroom — Gate A Area",
            center = Position(80f, 180f, 1),
            radiusMeters = 20f,
            announcementMessage = "Restrooms are on your right.",
            type = GeofenceZoneType.RESTROOM
        ),
        GeofenceZone(
            id = "GF_REST_B",
            name = "Restroom — Gate B Area",
            center = Position(280f, 380f, 1),
            radiusMeters = 20f,
            announcementMessage = "Restrooms are nearby.",
            type = GeofenceZoneType.RESTROOM
        ),

        // ── Food & Dining ─────────────────────────────────────────────────────
        GeofenceZone(
            id = "GF_FOOD_COURT",
            name = "Food Court",
            center = Position(200f, 300f, 1),
            radiusMeters = 35f,
            announcementMessage = "Food court ahead. Multiple dining options available.",
            type = GeofenceZoneType.FOOD_COURT
        ),
        GeofenceZone(
            id = "GF_CAFE_STARBUCKS",
            name = "Starbucks",
            center = Position(120f, 250f, 1),
            radiusMeters = 20f,
            announcementMessage = "Starbucks coffee shop is on your right.",
            type = GeofenceZoneType.CUSTOM
        )
    )
}

// Made with Bob
