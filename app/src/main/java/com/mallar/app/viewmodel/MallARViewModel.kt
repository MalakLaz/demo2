package com.mallar.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mallar.app.ml.EmbeddingModel
import com.mallar.app.ml.EmbeddingMatcher
import com.mallar.app.data.model.*
import com.mallar.app.data.repository.PlacesRepository
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
    val currentFloor: Int = 1,
    val hasArrived: Boolean = false,
    val allStores: List<Store> = emptyList()
)

class MallARViewModel(application: Application) : AndroidViewModel(application) {

    private val placesRepo = PlacesRepository(application)

    // ✅ الموديل
    private val model by lazy { EmbeddingModel(getApplication()) }

    // ✅ الماتشر
    private val matcher by lazy { EmbeddingMatcher(getApplication()) }

    private val _uiState = MutableStateFlow(MallARUiState())
    val uiState: StateFlow<MallARUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _uiState.value = _uiState.value.copy(
                    allStores = placesRepo.getAllAsStores(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

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

    fun selectStoreById(storeId: String) {
        val place = placesRepo.getPlaceById(storeId)
        place?.let {
            val steps = placesRepo.getNavigationSteps(it, _uiState.value.currentFloor)
            _uiState.value = _uiState.value.copy(
                selectedStore = it.toStore(),
                navigationSteps = steps,
                currentNavigationStep = 0,
                hasArrived = false
            )
        }
    }

    fun selectStore(store: Store) {
        val place = placesRepo.getPlaceById(store.id)
        if (place != null) {
            val steps = placesRepo.getNavigationSteps(place, _uiState.value.currentFloor)
            _uiState.value = _uiState.value.copy(
                selectedStore = store,
                navigationSteps = steps,
                currentNavigationStep = 0,
                hasArrived = false
            )
        }
    }

    // 🔥 AR Detection الحقيقي
    fun runARDetection(frame: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDetecting = true,
                detectionProgress = 0f,
                detectedLocation = null,
                detectionError = null
            )

            // progress
            for (i in 1..10) {
                delay(100)
                _uiState.value = _uiState.value.copy(detectionProgress = i / 10f)
            }

            try {
                // ✅ تشغيل الموديل
                val embedding = withContext(Dispatchers.Default) {
                    model.run(frame)
                }

                // ✅ مطابقة مع الداتابيز
                val matchName = matcher.findBestMatch(embedding)

                val detected = if (matchName != null) {
                    DetectedLocation(
                        id = matchName,
                        name = matchName,
                        floor = 1,
                        features = listOf("Detected by AI")
                    )
                } else {
                    DetectedLocation(
                        id = "unknown",
                        name = "Unknown مكان غير معروف",
                        floor = 1,
                        features = emptyList()
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isDetecting = false,
                    detectedLocation = detected
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDetecting = false,
                    detectionError = "Model error: ${e.message}"
                )
            }
        }
    }

    fun startARDetection() {
        runARDetection(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    fun nextNavigationStep() {
        val current = _uiState.value.currentNavigationStep
        val steps = _uiState.value.navigationSteps
        if (current < steps.size - 1) {
            val next = current + 1
            val isArrival = steps[next].direction == NavDirection.ARRIVAL
            _uiState.value = _uiState.value.copy(
                currentNavigationStep = next,
                hasArrived = isArrival
            )
        }
    }

    fun previousNavigationStep() {
        val current = _uiState.value.currentNavigationStep
        if (current > 0) {
            _uiState.value = _uiState.value.copy(
                currentNavigationStep = current - 1,
                hasArrived = false
            )
        }
    }

    fun resetNavigation() {
        _uiState.value = _uiState.value.copy(
            selectedStore = null,
            navigationSteps = emptyList(),
            currentNavigationStep = 0,
            hasArrived = false,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun clearDetection() {
        _uiState.value = _uiState.value.copy(
            detectedLocation = null,
            detectionError = null,
            isDetecting = false,
            detectionProgress = 0f
        )
    }
}