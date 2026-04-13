package com.mallar.app.data.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
//  Original MallData models (kept for backward compatibility with mall_data.json)
// ─────────────────────────────────────────────────────────────────────────────
data class MallData(
    @SerializedName("mall") val mall: MallInfo,
    @SerializedName("stores") val stores: List<Store>,
    @SerializedName("navigationPaths") val navigationPaths: Map<String, NavigationPath>,
    @SerializedName("detectedLocations") val detectedLocations: List<DetectedLocation>
)

data class MallInfo(
    @SerializedName("name") val name: String,
    @SerializedName("floors") val floors: Int,
    @SerializedName("totalStores") val totalStores: Int
)

data class Store(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("floor") val floor: Int,
    @SerializedName("wing") val wing: String,
    @SerializedName("storeNumber") val storeNumber: String,
    @SerializedName("description") val description: String,
    @SerializedName("openTime") val openTime: String,
    @SerializedName("closeTime") val closeTime: String,
    @SerializedName("logoColor") val logoColor: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("navigationNodes") val navigationNodes: List<String>
)

data class NavigationPath(
    @SerializedName("steps") val steps: List<NavigationStep>
)

data class NavigationStep(
    @SerializedName("instruction") val instruction: String,
    @SerializedName("distance") val distance: Int,
    @SerializedName("direction") val direction: String
)

data class DetectedLocation(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("floor") val floor: Int,
    @SerializedName("features") val features: List<String>
)

// ─────────────────────────────────────────────────────────────────────────────
//  YOUR places.json models — matched to actual JSON keys:
//  { "node_id": "N1", "x": 1038, "y": 217, "Brand": "Carina", "ID": 1, "Logo": "logos/carina.jpg" }
// ─────────────────────────────────────────────────────────────────────────────
data class Place(
    @SerializedName("node_id")  val nodeId: String      = "",
    @SerializedName("ID")       val id: Int             = 0,
    @SerializedName("Brand")    val name: String        = "",
    @SerializedName("Logo")     val logo: String        = "",
    @SerializedName("x")        val x: Float            = 0f,
    @SerializedName("y")        val y: Float            = 0f,
    // These don't exist in the JSON — safe defaults
    val category: String    = "Store",
    val floor: Int          = 1,
    val wing: String        = "A",
    val description: String = "",
    val openTime: String    = "10:00",
    val closeTime: String   = "22:00",
    val tags: List<String>  = emptyList(),
    val steps: List<PlaceNavStep> = emptyList()
) {
    // Unique string ID for navigation args
    val stringId: String get() = nodeId.ifBlank { "place_$id" }
}

data class PlaceNavStep(
    @SerializedName("instruction") val instruction: String,
    @SerializedName("distance")    val distance: Int,
    @SerializedName("direction")   val direction: String   // "straight","left","right","escalator_up"
)

// ─────────────────────────────────────────────────────────────────────────────
//  UI / shared models
// ─────────────────────────────────────────────────────────────────────────────
data class SearchResult(
    val store: Store,
    val distanceMeters: Int,
    val estimatedMinutes: Int
)

enum class NavDirection {
    STRAIGHT, LEFT, RIGHT, ESCALATOR_UP, ESCALATOR_DOWN, ELEVATOR, ARRIVAL
}

data class ARNavigationStep(
    val instruction: String,
    val distance: Int,
    val direction: NavDirection,
    val isFloorChange: Boolean = false,
    val pathAngle: Float = 0f
)

// ─── Helper: convert a Place → Store (so existing UI works unchanged) ────────
fun Place.toStore(): Store = Store(
    id           = stringId,
    name         = name,
    category     = category,
    floor        = floor,
    wing         = wing,
    storeNumber  = nodeId,
    description  = description,
    openTime     = openTime,
    closeTime    = closeTime,

    // 🔥 هنا الحل
    logoColor    = logo,

    tags         = tags + listOf(name),
    navigationNodes = steps.map { it.direction }
)