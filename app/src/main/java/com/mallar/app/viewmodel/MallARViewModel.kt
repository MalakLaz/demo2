package com.mallar.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mallar.app.data.model.*
import com.mallar.app.data.repository.GraphRepository
import com.mallar.app.data.repository.PlacesRepository
import com.mallar.app.ml.EmbeddingMatcher
import com.mallar.app.ml.EmbeddingModel
import com.mallar.app.navigation.MallGraph
import com.mallar.app.navigation.NavNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MallARUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedStore: Store? = null,
    val detectedLocation: DetectedLocation? = null,
    val detectionError: String? = null,
    val isDetecting: Boolean = false,
    val detectionProgress: Float = 0f,
    val detectionConfidence: Float = 0f,
    val currentNavigationStep: Int = 0,
    val navigationSteps: List<ARNavigationStep> = emptyList(),
    // ── A* path ───────────────────────────────────────────────────────────────
    val currentPath: List<NavNode> = emptyList(),          // full node list
    val pathDistanceMeters: Float = 0f,                    // estimated metres
    val currentFloor: Int = 1,
    val hasArrived: Boolean = false,
    val allStores: List<Store> = emptyList(),
    // ── current location (set after AR detection or manual selection) ─────────
    val currentLocationShopId: Int? = null,
    // ── sensor orientation ────────────────────────────────────────────────────
    val deviceAzimuth: Float = 0f
)

class MallARViewModel(application: Application) : AndroidViewModel(application) {

    private val placesRepo = PlacesRepository(application)
    private val graphRepo  = GraphRepository(application)

    private val model   by lazy { EmbeddingModel(getApplication()) }
    private val matcher by lazy { EmbeddingMatcher(getApplication()) }

    private val _uiState = MutableStateFlow(MallARUiState())
    val uiState: StateFlow<MallARUiState> = _uiState.asStateFlow()

    // Cached graph (loaded once on IO thread)
    private var graph: MallGraph? = null

    init { loadData() }

    // ── Init ──────────────────────────────────────────────────────────────────

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                graph = graphRepo.getGraph()
                _uiState.value = _uiState.value.copy(
                    allStores = placesRepo.getAllAsStores(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val results = placesRepo.searchPlaces(query)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(searchResults = results)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    // ── Store selection + A* ──────────────────────────────────────────────────

    fun selectStore(store: Store) {
        viewModelScope.launch(Dispatchers.IO) {
            val place = placesRepo.getPlaceById(store.id)

            // 1. Resolve Destination Graph Point ID
            val destShopId = graph?.shops
                ?.find { it.name.equals(store.name, ignoreCase = true) }
                ?.shopId

            val toPointId = if (destShopId != null) {
                graph?.pointIdForShop(destShopId)
            } else {
                graph?.findNearestNodeId(place?.x ?: 0f, place?.y ?: 0f)
            }

            // 2. Resolve Starting Graph Point ID
            val fromShopId = _uiState.value.currentLocationShopId
            val fromPointId = if (fromShopId != null) {
                graph?.pointIdForShop(fromShopId)
            } else {
                graph?.pointIdForShop(6) // fallback strictly to ZARA point
            }

            // 3. Compute A* path if we resolved both points 
            val path = if (fromPointId != null && toPointId != null) {
                graph?.findPath(fromPointId, toPointId)
            } else null

            val distMeters = if (path != null)
                (graph?.pathDistance(path) ?: 0f) * 0.05f  // rough px→m scale
            else 0f

            // Also generate turn-by-turn steps from the path
            val navSteps = if (path != null && path.size > 1)
                buildNavSteps(path, store.name)
            else
                place?.let { placesRepo.getNavigationSteps(it, _uiState.value.currentFloor) }
                    ?: emptyList()

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    selectedStore         = store,
                    currentPath           = path ?: emptyList(),
                    pathDistanceMeters    = distMeters,
                    navigationSteps       = navSteps,
                    currentNavigationStep = 0,
                    hasArrived            = false
                )
            }
        }
    }

    // ── Physical Sensor bindings ──────────────────────────────────────────────
    fun updateDeviceAzimuth(azimuth: Float) {
        _uiState.value = _uiState.value.copy(deviceAzimuth = azimuth)
    }

    fun onStepDetected() {
        val state = _uiState.value
        val steps = state.navigationSteps.toMutableList()
        val idx = state.currentNavigationStep

        if (idx < steps.size && !state.hasArrived) {
            val step = steps[idx]
            if (step.distance > 0) {
                // Deduct roughly 1 meter per step (standard pedestrian spacing)
                steps[idx] = step.copy(distance = step.distance - 1)
                _uiState.value = state.copy(navigationSteps = steps)
            } else if (step.direction != NavDirection.ARRIVAL) {
                nextNavigationStep()
            }
        }
    }

    fun selectStoreById(storeId: String) {
        val place = placesRepo.getPlaceById(storeId)
        place?.let { selectStore(it.toStore()) }
    }

    // ── Set current location manually (e.g. user picks "I am at Zara") ────────

    fun setCurrentLocation(shopId: Int) {
        _uiState.value = _uiState.value.copy(currentLocationShopId = shopId)
        // Re-run navigation if destination already selected
        _uiState.value.selectedStore?.let { selectStore(it) }
    }

    // ── AR Detection ──────────────────────────────────────────────────────────

    fun runARDetection(frame: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDetecting     = true,
                detectionProgress = 0f,
                detectedLocation  = null,
                detectionError    = null
            )

            for (i in 1..10) {
                delay(100)
                _uiState.value = _uiState.value.copy(detectionProgress = i / 10f)
            }

            try {
                val embedding  = withContext(Dispatchers.Default) { model.run(frame) }
                val matchName  = matcher.findBestMatch(embedding)

                // Try to map detected name → shopId in graph
                // Fallback to "ZARA" if no match is found, so the algorithm always has a valid starting point
                val finalMatchName = matchName ?: "ZARA"
                
                val detectedShopId = graph?.shops
                    ?.find { it.name.equals(finalMatchName, ignoreCase = true) }
                    ?.shopId ?: 34 // ZARA shopId is 34

                val detected = if (matchName != null) {
                    DetectedLocation(id = matchName, name = matchName,
                        floor = 1, features = listOf("Detected by AI"))
                } else {
                    DetectedLocation(id = "none", name = "ZARA (Demo Mock)",
                        floor = 1, features = listOf("Simulated location"))
                }

                _uiState.value = _uiState.value.copy(
                    isDetecting           = false,
                    detectedLocation      = detected,
                    currentLocationShopId = detectedShopId
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDetecting    = false,
                    detectionError = "Model error: ${e.message}"
                )
            }
        }
    }

    fun startARDetection() {
        runARDetection(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    fun confirmDetectedLocation() {
        val detectedShopId = _uiState.value.currentLocationShopId ?: return
        setCurrentLocation(detectedShopId)
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    fun nextNavigationStep() {
        val cur   = _uiState.value.currentNavigationStep
        val steps = _uiState.value.navigationSteps
        if (cur < steps.size - 1) {
            val next      = cur + 1
            val isArrival = steps[next].direction == NavDirection.ARRIVAL
            _uiState.value = _uiState.value.copy(
                currentNavigationStep = next,
                hasArrived            = isArrival
            )
        }
    }

    fun previousNavigationStep() {
        val cur = _uiState.value.currentNavigationStep
        if (cur > 0) {
            _uiState.value = _uiState.value.copy(
                currentNavigationStep = cur - 1,
                hasArrived            = false
            )
        }
    }

    fun resetNavigation() {
        _uiState.value = _uiState.value.copy(
            selectedStore         = null,
            currentPath           = emptyList(),
            pathDistanceMeters    = 0f,
            navigationSteps       = emptyList(),
            currentNavigationStep = 0,
            hasArrived            = false,
            searchQuery           = "",
            searchResults         = emptyList()
        )
    }

    fun clearDetection() {
        _uiState.value = _uiState.value.copy(
            detectedLocation  = null,
            detectionError    = null,
            isDetecting       = false,
            detectionProgress = 0f
        )
    }

    // ── Build turn-by-turn steps from A* node list ────────────────────────────

    private fun buildNavSteps(path: List<NavNode>, destName: String): List<ARNavigationStep> {
        if (path.size < 2) return listOf(
            ARNavigationStep("You are already at $destName", 0, NavDirection.ARRIVAL)
        )

        val steps = mutableListOf<ARNavigationStep>()

        // Group consecutive nodes into segments by direction
        var segStart = path[0]
        var segDist  = 0f

        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]
            segDist += hypot(curr.x - prev.x, curr.y - prev.y)

            val isLast = i == path.size - 1

            // Detect turn at this node (compare incoming vs outgoing angle)
            val turn = if (!isLast) {
                val next = path[i + 1]
                detectTurn(prev, curr, next)
            } else null

            if (turn != null || isLast) {
                val metres = (segDist * 0.05f).toInt().coerceAtLeast(1)
                val dir = when (turn) {
                    "LEFT"  -> NavDirection.LEFT
                    "RIGHT" -> NavDirection.RIGHT
                    else    -> NavDirection.STRAIGHT
                }
                val instruction = when {
                    isLast -> "You have arrived at $destName"
                    turn == "LEFT"  -> "Turn left"
                    turn == "RIGHT" -> "Turn right"
                    else            -> "Continue straight"
                }

                // Geometric azimuth angle for rendering the 3D rotating compass arrow
                val dy = curr.y - prev.y
                val dx = curr.x - prev.x
                var geometricBearing = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (geometricBearing < 0) geometricBearing += 360f
                while (geometricBearing >= 360f) geometricBearing -= 360f

                steps.add(ARNavigationStep(
                    instruction = instruction,
                    distance    = metres,
                    direction   = if (isLast) NavDirection.ARRIVAL else dir,
                    pathAngle   = geometricBearing
                ))
                segStart = curr
                segDist  = 0f
            }
        }

        return steps
    }

    private fun detectTurn(prev: NavNode, curr: NavNode, next: NavNode): String? {
        val inAngle  = Math.toDegrees(Math.atan2((curr.y - prev.y).toDouble(), (curr.x - prev.x).toDouble()))
        val outAngle = Math.toDegrees(Math.atan2((next.y - curr.y).toDouble(), (next.x - curr.x).toDouble()))
        var diff = outAngle - inAngle
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return when {
            diff > 30  -> "RIGHT"
            diff < -30 -> "LEFT"
            else       -> null
        }
    }

    private fun hypot(dx: Float, dy: Float) = kotlin.math.hypot(dx, dy)
}
