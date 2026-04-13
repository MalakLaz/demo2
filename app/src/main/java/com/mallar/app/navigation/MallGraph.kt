package com.mallar.app.navigation

import kotlin.math.hypot
import java.util.PriorityQueue

// ── Data Models ───────────────────────────────────────────────────────────────

data class NavNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val shopId: Int? = null
)

data class NavEdge(val from: Int, val to: Int)

data class ShopNode(
    val shopId: Int,
    val name: String,
    val pointId: Int
)

// ── MallGraph ─────────────────────────────────────────────────────────────────

class MallGraph(
    private val nodes: Map<Int, NavNode>,
    private val adjacency: Map<Int, List<Int>>,
    val shops: List<ShopNode>
) {

    // Find the graph point ID for a given shop ID
    fun pointIdForShop(shopId: Int): Int? =
        shops.find { it.shopId == shopId }?.pointId

    // Find shop by name (case-insensitive, partial match)
    fun findShopsByName(query: String): List<ShopNode> {
        val lower = query.lowercase().trim()
        return shops.filter { it.name.lowercase().contains(lower) }
    }

    // Find the nearest node geometrically (used when a store isn't explicitly mapped by name)
    fun findNearestNodeId(x: Float, y: Float): Int? {
        var minDst = Float.MAX_VALUE
        var nearest: Int? = null
        for (node in nodes.values) {
            val d = hypot(node.x - x, node.y - y)
            if (d < minDst) {
                minDst = d
                nearest = node.id
            }
        }
        return nearest
    }

    // ── A* Algorithm ──────────────────────────────────────────────────────────

    fun findPath(fromPointId: Int, toPointId: Int): List<NavNode>? {
        val start = nodes[fromPointId] ?: return null
        val end   = nodes[toPointId]   ?: return null

        data class Entry(val fScore: Float, val nodeId: Int) : Comparable<Entry> {
            override fun compareTo(other: Entry) = fScore.compareTo(other.fScore)
        }

        val openSet   = PriorityQueue<Entry>()
        val cameFrom  = mutableMapOf<Int, Int>()
        val gScore    = mutableMapOf<Int, Float>().withDefault { Float.MAX_VALUE }
        val fScore    = mutableMapOf<Int, Float>().withDefault { Float.MAX_VALUE }

        gScore[fromPointId] = 0f
        fScore[fromPointId] = heuristic(start, end)
        openSet.add(Entry(fScore[fromPointId]!!, fromPointId))

        val visited = mutableSetOf<Int>()

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!.nodeId

            if (current == toPointId) return reconstructPath(cameFrom, current)
            if (!visited.add(current)) continue

            for (neighborId in adjacency[current] ?: emptyList()) {
                if (neighborId in visited) continue
                val neighbor = nodes[neighborId] ?: continue
                val curNode  = nodes[current]    ?: continue

                val tentativeG = gScore.getValue(current) + dist(curNode, neighbor)

                if (tentativeG < gScore.getValue(neighborId)) {
                    cameFrom[neighborId] = current
                    gScore[neighborId]   = tentativeG
                    fScore[neighborId]   = tentativeG + heuristic(neighbor, end)
                    openSet.add(Entry(fScore[neighborId]!!, neighborId))
                }
            }
        }

        return null // No path found
    }

    // Convenience: find path between two shop IDs
    fun findPathBetweenShops(fromShopId: Int, toShopId: Int): List<NavNode>? {
        val fromPt = pointIdForShop(fromShopId) ?: return null
        val toPt   = pointIdForShop(toShopId)   ?: return null
        return findPath(fromPt, toPt)
    }

    // Total distance of a path in map units
    fun pathDistance(path: List<NavNode>): Float =
        (0 until path.size - 1).sumOf { i ->
            dist(path[i], path[i + 1]).toDouble()
        }.toFloat()

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun heuristic(a: NavNode, b: NavNode) = hypot(a.x - b.x, a.y - b.y)

    private fun dist(a: NavNode, b: NavNode) = hypot(a.x - b.x, a.y - b.y)

    private fun reconstructPath(cameFrom: Map<Int, Int>, end: Int): List<NavNode> {
        val path = mutableListOf<Int>()
        var cur  = end
        while (cameFrom.containsKey(cur)) {
            path.add(cur)
            cur = cameFrom[cur]!!
        }
        path.add(cur)
        path.reverse()
        return path.mapNotNull { nodes[it] }
    }
}
