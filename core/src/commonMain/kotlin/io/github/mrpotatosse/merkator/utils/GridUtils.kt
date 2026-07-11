package io.github.mrpotatosse.merkator.utils

import kotlin.math.abs
import kotlin.math.round

private const val EPS = 0.01f
fun worldToScreen(
    x: Float, y: Float, originX: Float, originY: Float,
    cellW: Float, cellH: Float
): Pair<Float, Float> {
    val offsetX = if (y.toInt() % 2 == 0) 0f else cellW / 2f
    val screenX = originX + x * cellW + offsetX
    val screenY = originY + y * (cellH / 2f)
    return screenX to screenY
}

fun screenToWorld(
    screenX: Float, screenY: Float, originX: Float, originY: Float,
    cellW: Float, cellH: Float
): Pair<Float, Float> {
    // approximate row, then test neighbors because diamonds overlap row bands
    val approxY = round((screenY - originY) / (cellH / 2f)).toInt()

    var best: Pair<Float, Float> = 0f to 0f
    var bestDist = Float.MAX_VALUE

    for (y in (approxY - 1)..(approxY + 1)) {
        val offsetX = if (y % 2 == 0) 0f else cellW / 2f
        val x = round((screenX - originX - offsetX) / cellW)

        // exact center of that candidate cell, via your own worldToScreen
        val (cx, cy) = worldToScreen(x, y.toFloat(), originX, originY, cellW, cellH)

        // diamond (Manhattan-in-iso-space) distance to the center
        val dist = abs(screenX - cx) / (cellW / 2f) + abs(screenY - cy) / (cellH / 2f)

        if (dist < bestDist) {
            bestDist = dist
            best = x to y.toFloat()
        }
    }
    return best
}

/**
 * Builds the isometric grid outline as line segments, skipping cells for which
 * [includeCell] returns false.
 *
 * Every kept cell contributes its four diamond edges to a set keyed on integer
 * lattice coordinates (half-cell units), so:
 *  - an edge shared by two kept cells is emitted exactly once,
 *  - an edge between a kept and a skipped cell is still emitted (holes stay outlined),
 *  - map borders need no special-casing anymore (the old l/t "fill" branches are gone).
 *
 * Screen positions still come from [worldToScreen]; the integer lattice is used
 * only as a float-safe dedup key.
 */
fun getIsoGridLines(
    width: Int,
    height: Int,
    cellWidth: Int,
    cellHeight: Int,
    rawOriginX: Float = 0f,
    rawOriginY: Float = 0f,
    includeCell: (cellId: Int) -> Boolean = { true },
): List<List<Pair<Float, Float>>> {
    val cellHalfWidth = cellWidth / 2.0f
    val cellHalfHeight = cellHeight / 2.0f
    val originX = rawOriginX + cellHalfWidth
    val originY = rawOriginY + cellHalfHeight

    // corner key on the half-unit lattice -> exact screen position
    val corners = HashMap<Long, Pair<Float, Float>>()
    // canonical edge = (cornerKeyA, cornerKeyB) with A < B
    val edges = LinkedHashSet<Pair<Long, Long>>()

    fun key(u: Int, v: Int): Long = (u.toLong() shl 32) or (v.toLong() and 0xFFFFFFFFL)

    fun addEdge(a: Long, b: Long) {
        edges += if (a < b) a to b else b to a
    }

    for (y in 0..<height * 2) {
        for (x in 0..<width) {
            val cellId = y * width + x
            if (!includeCell(cellId)) continue

            val (cx, cy) = worldToScreen(
                x.toFloat(),
                y.toFloat(),
                originX,
                originY,
                cellWidth.toFloat(),
                cellHeight.toFloat()
            )

            // corner lattice coords: center of cell (x, y) sits at u = 2x + y%2, v = y
            val u = 2 * x + (y % 2)
            val left = key(u - 1, y)
            val top = key(u, y - 1)
            val right = key(u + 1, y)
            val bottom = key(u, y + 1)

            corners[left] = Pair(cx - cellHalfWidth, cy)
            corners[top] = Pair(cx, cy - cellHalfHeight)
            corners[right] = Pair(cx + cellHalfWidth, cy)
            corners[bottom] = Pair(cx, cy + cellHalfHeight)

            addEdge(left, top)      // l
            addEdge(top, right)     // t
            addEdge(right, bottom)  // b (was implicit via neighbour's l / border fill)
            addEdge(bottom, left)   // r (was implicit via neighbour's t / border fill)
        }
    }

    val segments = edges.map { (a, b) -> corners.getValue(a) to corners.getValue(b) }
    return mergeCollinearSegments(segments)
}

/**
 * Merges segments that lie on the same infinite line and touch/overlap
 * end-to-end into single longer segments.
 */
private fun mergeCollinearSegments(
    segments: List<Pair<Pair<Float, Float>, Pair<Float, Float>>>
): List<List<Pair<Float, Float>>> {

    data class LineKey(val dirX: Int, val dirY: Int, val offset: Int)

    fun quantize(v: Float) = round(v / EPS)

    // group segments by the (canonicalized) line they lie on
    val groups = LinkedHashMap<LineKey, MutableList<Pair<Pair<Float, Float>, Pair<Float, Float>>>>()

    for (seg in segments) {
        val (p1, p2) = seg
        var dx = p2.first - p1.first
        var dy = p2.second - p1.second
        val len = kotlin.math.hypot(dx, dy)
        if (len < EPS) continue
        dx /= len
        dy /= len
        // canonicalize direction so (dx, dy) and (-dx, -dy) map to the same key
        if (dx < -EPS || (abs(dx) < EPS && dy < 0)) {
            dx = -dx; dy = -dy
        }
        // signed perpendicular distance from origin -> identifies the specific line
        val offset = p1.first * dy - p1.second * dx
        val key = LineKey(quantize(dx).toInt(), quantize(dy).toInt(), quantize(offset).toInt())
        groups.getOrPut(key) { mutableListOf() }.add(seg)
    }

    val result = mutableListOf<List<Pair<Float, Float>>>()

    for (segs in groups.values) {
        val (p1, p2) = segs[0]
        var dx = p2.first - p1.first
        var dy = p2.second - p1.second
        val len = kotlin.math.hypot(dx, dy)
        dx /= len
        dy /= len
        if (dx < -EPS || (abs(dx) < EPS && dy < 0)) {
            dx = -dx; dy = -dy
        }

        // project each segment's endpoints onto the shared direction
        data class Range(val tMin: Float, val tMax: Float, val pMin: Pair<Float, Float>, val pMax: Pair<Float, Float>)

        val ranges = segs.map { (a, b) ->
            val ta = a.first * dx + a.second * dy
            val tb = b.first * dx + b.second * dy
            if (ta <= tb) Range(ta, tb, a, b) else Range(tb, ta, b, a)
        }.sortedBy { it.tMin }

        var curMin = ranges[0].pMin
        var curMax = ranges[0].pMax
        var curMaxT = ranges[0].tMax

        for (i in 1 until ranges.size) {
            val r = ranges[i]
            if (r.tMin <= curMaxT + EPS) {
                // touches/overlaps current run -> extend it
                if (r.tMax > curMaxT) {
                    curMaxT = r.tMax
                    curMax = r.pMax
                }
            } else {
                // gap found -> close current run, start a new one
                result.add(listOf(curMin, curMax))
                curMin = r.pMin
                curMax = r.pMax
                curMaxT = r.tMax
            }
        }
        result.add(listOf(curMin, curMax))
    }

    return result
}