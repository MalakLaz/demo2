package com.mallar.app.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

class EmbeddingMatcher(context: Context) {

    private val embeddings: List<FloatArray>
    private val labels: List<String>

    private val THRESHOLD = 0.53f

    init {
        val json = context.assets.open("data.json")  // ← اسم الملف الجديد
            .bufferedReader().use { it.readText() }

        // format جديد: { "embeddings": [[...], ...], "labels": ["OXXO", ...] }
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val raw: Map<String, Any> = Gson().fromJson(json, type)

        @Suppress("UNCHECKED_CAST")
        val embRaw = raw["embeddings"] as List<List<Double>>
        @Suppress("UNCHECKED_CAST")
        labels = raw["labels"] as List<String>

        embeddings = embRaw.map { vec -> FloatArray(vec.size) { i -> vec[i].toFloat() } }
    }

    fun findBestMatch(input: FloatArray): String? {
        val normalizedInput = normalize(input)

        var bestScore = -1f
        var bestMatch: String? = null

        for (i in embeddings.indices) {
            val vector = embeddings[i]

            if (vector.size != normalizedInput.size) continue

            val score = cosineSimilarity(normalizedInput, vector)

            if (score > bestScore) {
                bestScore = score
                bestMatch = labels[i]
            }
        }

        return if (bestScore >= THRESHOLD) bestMatch else null
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return dot / (sqrt(normA) * sqrt(normB) + 1e-6f)
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum) + 1e-6f
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }
}