package com.example.flightbooking.data

import com.example.flightbooking.data.models.*

/**
 * MockFloorPlanData
 *
 * Generates a procedural [FloorPlan] for each floor of Terminal 1.
 * All coordinates match the logical positions used in [MockData.getSampleGates]
 * and [MockData.getSampleAmenities], so the rendered floor plan aligns exactly
 * with the gate/POI markers overlaid on top.
 *
 * Coordinate system reminder:
 *   x: 60 – 620  (west → east)
 *   y: 120 – 480  (north → south)
 *   unit ≈ 1 metre
 */
object MockFloorPlanData {

    private val cache = mutableMapOf<Int, FloorPlan>()

    fun getFloorPlan(floor: Int): FloorPlan =
        cache.getOrPut(floor) {
            when (floor) {
                1    -> buildFloor1()
                2    -> buildFloor2()
                else -> FloorPlan(floor, emptyList(), emptyList())
            }
        }

    // ── Floor 1 ───────────────────────────────────────────────────────────────
    // Layout:
    //   Main spine corridor: y=250, x=80–560 (east-west)
    //   Gate-A wing:         y=150, x=80–560 (gates every 50 units, y=150)
    //   Gate-B wing:         y=350, x=80–560 (gates every 50 units, y=350)
    //   North connector:     x=300, y=150–250 (spine ↔ gate-A)
    //   South connector:     x=300, y=250–350 (spine ↔ gate-B)
    //   Entrance hall:       y=420–460, x=200–400

    private fun buildFloor1(): FloorPlan {
        val corridors = listOf(
            // Main east-west spine
            Corridor(80f, 250f, 560f, 250f, floor = 1, widthUnits = 22f),
            // Gate-A concourse (top row)
            Corridor(80f, 150f, 560f, 150f, floor = 1, widthUnits = 18f),
            // Gate-B concourse (bottom row)
            Corridor(80f, 350f, 560f, 350f, floor = 1, widthUnits = 18f),
            // North vertical connector spine ↔ Gate-A
            Corridor(300f, 150f, 300f, 250f, floor = 1, widthUnits = 18f),
            // South vertical connector spine ↔ Gate-B
            Corridor(300f, 250f, 300f, 350f, floor = 1, widthUnits = 18f),
            // West end vertical (ties gate-A and gate-B to spine at x=90)
            Corridor(90f, 150f, 90f, 350f, floor = 1, widthUnits = 14f),
            // East end vertical (ties gate-A and gate-B to spine at x=550)
            Corridor(550f, 150f, 550f, 350f, floor = 1, widthUnits = 14f),
            // Entrance hall approach
            Corridor(200f, 350f, 400f, 450f, floor = 1, widthUnits = 30f)
        )

        val rooms = mutableListOf<RoomOutline>()

        // ── Gate rooms (A1-A10 at y=150, B1-B10 at y=350) ────────────────────
        val gateAPositions = listOf(100f, 150f, 200f, 250f, 300f, 350f, 400f, 450f, 500f, 550f)
        val gateBPositions = listOf(100f, 150f, 200f, 250f, 300f, 350f, 400f, 450f, 500f, 550f)
        val gateANames   = listOf("A1","A2","A3","A4","A5","A6","A7","A8","A9","A10")
        val gateBNames   = listOf("B1","B2","B3","B4","B5","B6","B7","B8","B9","B10")

        gateAPositions.forEachIndexed { i, x ->
            rooms += RoomOutline(
                id = "room_gate_A${i+1}", label = gateANames[i],
                bounds = MapRect(x - 16f, 120f, x + 16f, 148f),
                floor = 1, type = RoomType.GATE
            )
        }
        gateBPositions.forEachIndexed { i, x ->
            rooms += RoomOutline(
                id = "room_gate_B${i+1}", label = gateBNames[i],
                bounds = MapRect(x - 16f, 352f, x + 16f, 385f),
                floor = 1, type = RoomType.GATE
            )
        }

        // ── Amenity zones ─────────────────────────────────────────────────────
        rooms += listOf(
            // Security checkpoint — centred at (300, 250) with generous zone
            RoomOutline(
                id = "room_security", label = "Security",
                bounds = MapRect(255f, 225f, 345f, 275f),
                floor = 1, type = RoomType.SECURITY
            ),
            // Food court — centred at (200, 300)
            RoomOutline(
                id = "room_food_court", label = "Food Court",
                bounds = MapRect(160f, 275f, 240f, 325f),
                floor = 1, type = RoomType.FOOD_COURT
            ),
            // Maple Leaf Lounge — (250, 200)
            RoomOutline(
                id = "room_lounge_1", label = "Maple Leaf Lounge",
                bounds = MapRect(215f, 180f, 285f, 220f),
                floor = 1, type = RoomType.LOUNGE
            ),
            // Restroom Gate A — (80, 180)
            RoomOutline(
                id = "room_rest_a", label = "WC",
                bounds = MapRect(68f, 168f, 92f, 192f),
                floor = 1, type = RoomType.RESTROOM
            ),
            // Restroom Gate B — (280, 380)
            RoomOutline(
                id = "room_rest_b", label = "WC",
                bounds = MapRect(268f, 368f, 292f, 392f),
                floor = 1, type = RoomType.RESTROOM
            ),
            // Restroom Central — (300, 250) tucked beside security
            RoomOutline(
                id = "room_rest_c", label = "WC",
                bounds = MapRect(348f, 238f, 368f, 262f),
                floor = 1, type = RoomType.RESTROOM
            ),
            // Starbucks — (120, 250)
            RoomOutline(
                id = "room_starbucks", label = "Starbucks",
                bounds = MapRect(105f, 238f, 135f, 262f),
                floor = 1, type = RoomType.CAFE
            ),
            // Tim Hortons — (420, 180)
            RoomOutline(
                id = "room_timhortons", label = "Tim Hortons",
                bounds = MapRect(405f, 168f, 435f, 192f),
                floor = 1, type = RoomType.CAFE
            ),
            // Stair/Elevator connection to Floor 2 — near (300, 200)
            RoomOutline(
                id = "room_stairs_f1", label = "To Floor 2",
                bounds = MapRect(285f, 188f, 315f, 212f),
                floor = 1, type = RoomType.STAIRS_ELEVATOR
            ),
            // Entrance hall
            RoomOutline(
                id = "room_entrance", label = "Entrance",
                bounds = MapRect(200f, 420f, 400f, 465f),
                floor = 1, type = RoomType.GENERAL
            )
        )

        return FloorPlan(floor = 1, corridors = corridors, rooms = rooms)
    }

    // ── Floor 2 ───────────────────────────────────────────────────────────────
    // Layout:
    //   Lounge concourse: y=200, x=200–620 (east-west)
    //   Gate-C row:       y=200, x=200–620 (same concourse as lounges)
    //   Central spine:    x=300, y=200–300

    private fun buildFloor2(): FloorPlan {
        val corridors = listOf(
            // Main lounge / gate-C concourse
            Corridor(180f, 200f, 620f, 200f, floor = 2, widthUnits = 22f),
            // Short spur to lounge suite
            Corridor(440f, 200f, 440f, 270f, floor = 2, widthUnits = 16f),
            // Stairwell connection back to floor 1
            Corridor(300f, 200f, 300f, 260f, floor = 2, widthUnits = 18f)
        )

        val rooms = mutableListOf<RoomOutline>()

        // Gate-C rooms (C1-C5 at y=200, floor=2)
        val gateCXs    = listOf(200f, 300f, 400f, 500f, 600f)
        val gateCNames = listOf("C1","C2","C3","C4","C5")
        gateCXs.forEachIndexed { i, x ->
            rooms += RoomOutline(
                id = "room_gate_C${i+1}", label = gateCNames[i],
                bounds = MapRect(x - 16f, 164f, x + 16f, 182f),
                floor = 2, type = RoomType.GATE
            )
        }

        rooms += listOf(
            // Air Canada Signature Suite lounge — (450, 250)
            RoomOutline(
                id = "room_lounge_2", label = "AC Signature Suite",
                bounds = MapRect(415f, 230f, 490f, 280f),
                floor = 2, type = RoomType.LOUNGE
            ),
            // Restroom upper level — (350, 230)
            RoomOutline(
                id = "room_rest_upper", label = "WC",
                bounds = MapRect(338f, 218f, 362f, 242f),
                floor = 2, type = RoomType.RESTROOM
            ),
            // Second Cup café — (250, 280)
            RoomOutline(
                id = "room_second_cup", label = "Second Cup",
                bounds = MapRect(235f, 265f, 265f, 295f),
                floor = 2, type = RoomType.CAFE
            ),
            // Stair/elevator hub
            RoomOutline(
                id = "room_stairs_f2", label = "To Floor 1",
                bounds = MapRect(285f, 230f, 315f, 270f),
                floor = 2, type = RoomType.STAIRS_ELEVATOR
            )
        )

        return FloorPlan(floor = 2, corridors = corridors, rooms = rooms)
    }
}

// Made with Bob
