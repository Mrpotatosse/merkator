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

fun getIsoGridLines(
    width: Int,
    height: Int,
    cellWidth: Int,
    cellHeight: Int,
    rawOriginX: Float = 0f,
    rawOriginY: Float = 0f
): List<List<Pair<Float, Float>>> {
    val cellHalfWidth = cellWidth / 2.0f
    val cellHalfHeight = cellHeight / 2.0f
    val originX = rawOriginX + cellHalfWidth
    val originY = rawOriginY + cellHalfHeight

    // raw (unmerged) segments, each as a pair of endpoints
    val segments = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()

    for (y in 0..<height * 2) {
        for (x in 0..<width) {
            val (startX, startY) = worldToScreen(
                x.toFloat(),
                y.toFloat(),
                originX,
                originY,
                cellWidth.toFloat(),
                cellHeight.toFloat()
            )

            // l
            segments += Pair(startX - cellHalfWidth, startY) to Pair(startX, startY - cellHalfHeight)
            // t
            segments += Pair(startX, startY - cellHalfHeight) to Pair(startX + cellHalfWidth, startY)

            if (((y % 2 == 1) and (x == width - 1)) or (y == (height * 2) - 1)) {
                // l fill
                segments += Pair(startX, startY + cellHalfHeight) to Pair(startX + cellHalfWidth, startY)
            }
            if (((y % 2 == 0) and (x == 0)) or (y == (height * 2) - 1)) {
                // t fill
                segments += Pair(startX - cellHalfWidth, startY) to Pair(startX, startY + cellHalfHeight)
            }
        }
    }

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