package com.mallar.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mallar.app.data.model.*
import kotlin.math.sqrt

class PlacesRepository(private val context: Context) {

    private var cachedPlaces: List<Place>? = null

    private fun loadPlaces(): List<Place> {
        if (cachedPlaces != null) return cachedPlaces!!

        return try {
            val json = context.assets.open("places.json")
                .bufferedReader().use { it.readText() }

            val type = object : TypeToken<List<Place>>() {}.type
            val places: List<Place> = Gson().fromJson(json, type)

            cachedPlaces = places.filter { it.name.isNotBlank() }
            cachedPlaces!!

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getAllPlaces(): List<Place> = loadPlaces()

    // ✅ FIXED: return Store list (ViewModel expects this)
    fun getAllAsStores(): List<Store> =
        loadPlaces().map { it.toStore() }

    fun getPlaceById(id: String): Place? =
        loadPlaces().find { it.stringId == id }

    // ✅ FIXED: return SearchResult (ViewModel expects this)
    fun searchPlaces(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val lower = query.lowercase().trim()

        return loadPlaces()
            .filter { it.name.lowercase().contains(lower) }
            .map {
                val dist = estimateDistance(it)
                SearchResult(
                    store = it.toStore(),
                    distanceMeters = dist,
                    estimatedMinutes = (dist / 50).coerceAtLeast(1)
                )
            }
            .sortedBy { it.distanceMeters }
    }

    // ✅ FIXED: return ARNavigationStep + accept floor
    fun getNavigationSteps(place: Place, currentFloor: Int): List<ARNavigationStep> {
        val steps = mutableListOf<ARNavigationStep>()

        val zone = when {
            place.x > 850 -> "RIGHT"
            place.x < 300 -> "LEFT"
            else -> "CENTER"
        }

        when (zone) {
            "RIGHT" -> {
                steps.add(ARNavigationStep("Go straight", 30, NavDirection.STRAIGHT))
                steps.add(ARNavigationStep("Turn right", 20, NavDirection.RIGHT))
            }
            "LEFT" -> {
                steps.add(ARNavigationStep("Go straight", 30, NavDirection.STRAIGHT))
                steps.add(ARNavigationStep("Turn left", 20, NavDirection.LEFT))
            }
            else -> {
                steps.add(ARNavigationStep("Go straight", 20, NavDirection.STRAIGHT))
            }
        }

        // floor change (optional)
        if (place.floor != currentFloor) {
            steps.add(
                ARNavigationStep(
                    "Go to floor ${place.floor}",
                    0,
                    NavDirection.ELEVATOR,
                    true
                )
            )
        }

        steps.add(
            ARNavigationStep(
                "You have arrived at ${place.name}",
                0,
                NavDirection.ARRIVAL
            )
        )

        return steps
    }

    private fun estimateDistance(place: Place): Int {
        val dx = place.x - 580f
        val dy = place.y - 390f
        return (sqrt(dx * dx + dy * dy) * 0.15f).toInt().coerceIn(10, 500)
    }
}