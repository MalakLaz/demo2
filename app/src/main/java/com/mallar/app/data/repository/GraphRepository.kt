package com.mallar.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mallar.app.navigation.MallGraph
import com.mallar.app.navigation.NavEdge
import com.mallar.app.navigation.NavNode
import com.mallar.app.navigation.ShopNode

// ── Raw JSON models ───────────────────────────────────────────────────────────

private data class RawNode(
    @SerializedName("id")     val id: Int,
    @SerializedName("x")      val x: Float,
    @SerializedName("y")      val y: Float,
    @SerializedName("shopId") val shopId: Int?
)

private data class RawEdge(
    @SerializedName("from") val from: Int,
    @SerializedName("to")   val to: Int
)

private data class RawShop(
    @SerializedName("shopId")  val shopId: Int,
    @SerializedName("name")    val name: String,
    @SerializedName("pointId") val pointId: Int
)

private data class RawGraphData(
    @SerializedName("nodes") val nodes: List<RawNode>,
    @SerializedName("edges") val edges: List<RawEdge>,
    @SerializedName("shops") val shops: List<RawShop>
)

// ── GraphRepository ───────────────────────────────────────────────────────────

class GraphRepository(private val context: Context) {

    private var cachedGraph: MallGraph? = null

    fun getGraph(): MallGraph {
        cachedGraph?.let { return it }

        val json = context.assets
            .open("mall_graph.json")
            .bufferedReader()
            .use { it.readText() }

        val raw = Gson().fromJson(json, RawGraphData::class.java)

        // Build nodes map
        val nodes: Map<Int, NavNode> = raw.nodes.associate { r ->
            r.id to NavNode(id = r.id, x = r.x, y = r.y, shopId = r.shopId)
        }

        // Build bidirectional adjacency list
        val adjacency = mutableMapOf<Int, MutableList<Int>>()
        for (node in raw.nodes) adjacency[node.id] = mutableListOf()
        for (edge in raw.edges) {
            adjacency[edge.from]?.add(edge.to)
            adjacency[edge.to]?.add(edge.from)   // bidirectional
        }

        // Build shops list
        val shops: List<ShopNode> = raw.shops.map { r ->
            ShopNode(shopId = r.shopId, name = r.name, pointId = r.pointId)
        }

        return MallGraph(nodes, adjacency, shops).also { cachedGraph = it }
    }
}
