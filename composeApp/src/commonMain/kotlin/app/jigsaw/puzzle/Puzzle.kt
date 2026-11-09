package app.jigsaw.puzzle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.random.Random

/** Shared once by its two neighboring pieces. Positive depth points down/right. */
data class Edge(val center: Float, val width: Float, val depth: Float) {
    val flat get() = depth == 0f
}

data class Piece(val id: Int, val row: Int, val column: Int, val top: Edge, val right: Edge, val bottom: Edge, val left: Edge) {
    val isBorder get() = listOf(top, right, bottom, left).any { it.flat }
    // Inward/outward signatures are relative to each piece, independent of size.
    val shapeKey get() = listOf(-top.depth, right.depth, bottom.depth, -left.depth)
        .joinToString("") { if (it == 0f) "0" else if (it > 0f) "2" else "1" }
}

/** Geometry uses image coordinates. Render each path after translating by position - solvedPosition. */
class PuzzleGeometry(val columns: Int, val rows: Int, val width: Float, val height: Float, seed: Int = 1) {
    init { require(columns > 0 && rows > 0 && width > 0 && height > 0) }
    val cellWidth = width / columns
    val cellHeight = height / rows
    private val random = Random(seed)
    private fun edge() = Edge(.44f + random.nextFloat() * .12f, .25f + random.nextFloat() * .07f,
        (.17f + random.nextFloat() * .055f) * if (random.nextBoolean()) 1 else -1)
    private val flat = Edge(.5f, 0f, 0f)
    private val horizontal = List(rows + 1) { r -> List(columns) { if (r == 0 || r == rows) flat else edge() } }
    private val vertical = List(rows) { List(columns + 1) { c -> if (c == 0 || c == columns) flat else edge() } }
    val pieces = List(rows * columns) { id ->
        val r = id / columns; val c = id % columns
        Piece(id, r, c, horizontal[r][c], vertical[r][c + 1], horizontal[r + 1][c], vertical[r][c])
    }
    fun solvedPosition(id: Int) = Offset(pieces[id].column * cellWidth, pieces[id].row * cellHeight)
    fun neighbors(id: Int): List<Int> = pieces[id].let { p -> buildList {
        if (p.row > 0) add(id - columns)
        if (p.column < columns - 1) add(id + 1)
        if (p.row < rows - 1) add(id + columns)
        if (p.column > 0) add(id - 1)
    } }

    /** Local piece path; image must be drawn at -solvedPosition(id) inside this clip. */
    fun path(id: Int): Path {
        val p = pieces[id]
        val path = Path().apply { moveTo(0f, 0f) }
        fun side(edge: Edge, origin: Offset, horizontal: Boolean, reverse: Boolean) {
            fun point(t: Float, n: Float): Offset = origin + if (horizontal) Offset(t * cellWidth, n * cellHeight) else Offset(n * cellWidth, t * cellHeight)
            fun line(t: Float, n: Float) { val q = point(t, n); path.lineTo(q.x, q.y) }
            fun curve(a: Offset, b: Offset, c: Offset) { path.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y) }
            if (edge.flat) { line(if (reverse) 0f else 1f, 0f); return }
            val c = edge.center; val w = edge.width; val d = edge.depth
            // Four cubic curves make a neck and rounded head. Reversal uses identical controls.
            val segments = listOf(
                listOf(point(c-w*.5f,0f), point(c-w*.3f,0f), point(c-w*.30f,d*.15f), point(c-w*.45f,d*.48f)),
                listOf(point(c-w*.45f,d*.48f), point(c-w*.78f,d*1.15f), point(c-w*.12f,d*1.2f), point(c,d)),
                listOf(point(c,d), point(c+w*.12f,d*1.2f), point(c+w*.78f,d*1.15f), point(c+w*.45f,d*.48f)),
                listOf(point(c+w*.45f,d*.48f), point(c+w*.30f,d*.15f), point(c+w*.3f,0f), point(c+w*.5f,0f))
            )
            if (reverse) {
                line(c+w*.5f,0f)
                segments.asReversed().forEach { curve(it[2],it[1],it[0]) }
                line(0f,0f)
            } else {
                line(c-w*.5f,0f)
                segments.forEach { curve(it[1],it[2],it[3]) }
                line(1f,0f)
            }
        }
        side(p.top, Offset.Zero, true, false)
        side(p.right, Offset(cellWidth,0f), false, false)
        side(p.bottom, Offset(0f,cellHeight), true, true)
        side(p.left, Offset.Zero, false, true)
        path.close()
        return path
    }
}

enum class StashSort { MANUAL, COLOR, SHAPE }
data class SnapResult(val joinedPieces: Int, val complete: Boolean)

/** Mutations are explicit; increment a Compose state revision after a gesture or use snapshot(). */
class PuzzleSession(val geometry: PuzzleGeometry) {
    private val positions = geometry.pieces.associate { it.id to geometry.solvedPosition(it.id) }.toMutableMap()
    private val groups = geometry.pieces.associate { it.id to it.id }.toMutableMap()
    private val stash = geometry.pieces.map { it.id }.toMutableList()
    val stashIds: List<Int> get() = stash.toList()
    val boardIds: List<Int> get() = geometry.pieces.map { it.id }.filter { it !in stash }
    val complete get() = stash.isEmpty() && groups.values.distinct().size == 1
    val joinedCount get() = geometry.pieces.size - groups.values.distinct().size
    fun position(id: Int): Offset = positions.getValue(id)
    fun groupIds(id: Int) = geometry.pieces.map { it.id }.filter { groups[it] == groups[id] }
    fun isInStash(id: Int) = id in stash
    fun place(id: Int, position: Offset) { stash.remove(id); positions[id] = position }
    fun moveGroup(id: Int, delta: Offset) { groupIds(id).forEach { positions[it] = position(it) + delta } }
    fun returnToStash(id: Int) {
        val members = groupIds(id)
        members.forEach { groups[it] = it; if (it !in stash) stash.add(it) }
    }
    fun reset(seed: Int = 1) {
        geometry.pieces.forEach { positions[it.id] = geometry.solvedPosition(it.id); groups[it.id] = it.id }
        stash.clear(); stash.addAll(geometry.pieces.map { it.id }.shuffled(Random(seed)))
    }
    fun scatter(areaWidth: Float, areaHeight: Float, seed: Int = 1) {
        reset(seed); val random = Random(seed)
        stash.toList().forEach { place(it, Offset(random.nextFloat() * (areaWidth - geometry.cellWidth).coerceAtLeast(0f), random.nextFloat() * (areaHeight - geometry.cellHeight).coerceAtLeast(0f))) }
    }
    fun reorderStash(id: Int, targetIndex: Int) { if (stash.remove(id)) stash.add(targetIndex.coerceIn(0, stash.size),id) }
    /** colorKeys can be hue buckets or any numeric key derived from source-image pixels. */
    fun sortStash(sort: StashSort, colorKeys: Map<Int, Float> = emptyMap()) {
        when (sort) {
            StashSort.MANUAL -> Unit
            StashSort.COLOR -> stash.sortWith(compareBy<Int> { colorKeys[it] ?: 0f }.thenBy { it })
            StashSort.SHAPE -> stash.sortWith(compareBy<Int> { geometry.pieces[it].shapeKey }.thenBy { it })
        }
    }
    /** Only real adjacent pieces can join; alignment is measured as translation from solved positions. */
    fun snap(id: Int, tolerance: Float = minOf(geometry.cellWidth, geometry.cellHeight) * .22f): SnapResult {
        require(tolerance >= 0f)
        if (id in stash) return SnapResult(0, complete)
        var joined = 0
        while (true) {
            val moving = groupIds(id)
            val candidates = moving.flatMap { a -> geometry.neighbors(a).filter { it !in stash && it !in moving }.map { a to it } }
            val match = candidates.map { (a,b) ->
                val adjustment = (position(b) - geometry.solvedPosition(b)) - (position(a) - geometry.solvedPosition(a))
                Triple(a,b,adjustment)
            }.filter { (_,_,d) -> d.getDistance() <= tolerance }.minByOrNull { it.third.getDistance() } ?: break
            moveGroup(id, match.third)
            val merged = groupIds(match.second)
            val mainGroup = groups.getValue(id)
            merged.forEach { groups[it] = mainGroup }
            joined += merged.size
        }
        return SnapResult(joined, complete)
    }
}
