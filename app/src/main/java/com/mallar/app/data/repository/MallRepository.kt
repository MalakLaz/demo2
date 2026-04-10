package com.mallar.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.mallar.app.R
import com.mallar.app.data.model.*

class MallRepository(private val context: Context) {

    private var mallData: MallData? = null

    fun loadMallData(): MallData {
        if (mallData != null) return mallData!!
        val inputStream = context.resources.openRawResource(R.raw.mall_data)
        val json = inputStream.bufferedReader().use { it.readText() }
        mallData = Gson().fromJson(json, MallData::class.java)
        return mallData!!
    }

    fun searchStores(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val data = loadMallData()
        val lowerQuery = query.lowercase()
        return data.stores
            .filter { store ->
                store.name.lowercase().contains(lowerQuery) ||
                store.category.lowercase().contains(lowerQuery) ||
                store.tags.any { it.lowercase().contains(lowerQuery) }
            }
            .map { store ->
                SearchResult(
                    store = store,
                    distanceMeters = calculateDistance(store),
                    estimatedMinutes = calculateTime(store)
                )
            }
            .sortedBy { it.distanceMeters }
    }

    fun getAllStores(): List<Store> = loadMallData().stores

    fun getStoreById(id: String): Store? =
        loadMallData().stores.find { it.id == id }

    fun getNavigationSteps(store: Store, currentFloor: Int = 1): List<ARNavigationStep> {
        val steps = mutableListOf<ARNavigationStep>()
        val targetFloor = store.floor

        // Add floor navigation if needed
        if (currentFloor < targetFloor) {
            for (floor in currentFloor until targetFloor) {
                steps.add(
                    ARNavigationStep(
                        instruction = "Head to escalator",
                        distance = 30,
                        direction = NavDirection.STRAIGHT,
                        isFloorChange = false
                    )
                )
                steps.add(
                    ARNavigationStep(
                        instruction = "Take escalator to Floor ${floor + 1}",
                        distance = 10,
                        direction = NavDirection.ESCALATOR_UP,
                        isFloorChange = true
                    )
                )
            }
        }

        // Wing-specific navigation
        when (store.wing) {
            "A" -> {
                steps.add(ARNavigationStep("Go straight", 20, NavDirection.STRAIGHT))
                steps.add(ARNavigationStep("Turn left toward Wing A", 15, NavDirection.LEFT))
            }
            "B" -> {
                steps.add(ARNavigationStep("Go straight", 20, NavDirection.STRAIGHT))
                steps.add(ARNavigationStep("Turn right toward Wing B", 15, NavDirection.RIGHT))
            }
            "C" -> {
                steps.add(ARNavigationStep("Continue straight", 25, NavDirection.STRAIGHT))
            }
        }

        steps.add(
            ARNavigationStep(
                instruction = "${store.name} is on your left",
                distance = 10,
                direction = NavDirection.LEFT
            )
        )
        steps.add(
            ARNavigationStep(
                instruction = "You have arrived at ${store.name}!",
                distance = 0,
                direction = NavDirection.ARRIVAL
            )
        )
        return steps
    }

    fun detectLocationFromFeatures(features: List<String>): DetectedLocation? {
        val data = loadMallData()
        return data.detectedLocations.maxByOrNull { location ->
            location.features.count { f -> features.any { it.lowercase().contains(f.lowercase()) } }
        }
    }

    private fun calculateDistance(store: Store): Int {
        val baseDistance = when (store.wing) {
            "A" -> 50
            "B" -> 80
            "C" -> 120
            else -> 100
        }
        val floorBonus = (store.floor - 1) * 40
        return baseDistance + floorBonus
    }

    private fun calculateTime(store: Store): Int {
        return (calculateDistance(store) / 50.0).toInt().coerceAtLeast(1)
    }
}
