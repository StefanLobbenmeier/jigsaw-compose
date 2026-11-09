package app.jigsaw.puzzle

import androidx.compose.ui.geometry.Offset
import kotlin.test.*

class PuzzleTest {
    private fun geometry() = PuzzleGeometry(3, 2, 300f, 200f, 42)
    @Test fun sharedEdgesAndSeed() {
        val g = geometry()
        for (p in g.pieces) {
            if (p.column < 2) assertSame(p.right, g.pieces[p.id+1].left)
            if (p.row < 1) assertSame(p.bottom, g.pieces[p.id+3].top)
        }
        assertEquals(g.pieces, geometry().pieces)
        assertTrue(g.pieces[0].top.flat && g.pieces[0].left.flat)
        assertTrue(g.pieces.map { it.right }.distinct().size > 2)
    }
    @Test fun correctNeighborsSnapAndMoveAsAGroup() {
        val s = PuzzleSession(geometry())
        s.place(0, Offset(20f, 30f)); s.place(1, Offset(130f, 36f))
        assertEquals(1, s.snap(0, 15f).joinedPieces)
        assertEquals(Offset(30f,36f),s.position(0))
        s.moveGroup(0,Offset(5f,9f))
        assertEquals(Offset(135f,45f),s.position(1))
        assertEquals(setOf(0,1),s.groupIds(1).toSet())
    }
    @Test fun wrongNeighborsAndDistantPiecesNeverSnap() {
        val s = PuzzleSession(geometry())
        s.place(0,Offset.Zero); s.place(2,Offset(200f,0f))
        assertEquals(0,s.snap(0).joinedPieces)
        s.place(1,Offset(160f,0f))
        assertEquals(0,s.snap(0,20f).joinedPieces)
    }
    @Test fun chainMergeCompletesAndResetRestoresStash() {
        val g = geometry(); val s = PuzzleSession(g)
        g.pieces.forEach { s.place(it.id,g.solvedPosition(it.id)) }
        assertFalse(s.complete)
        assertEquals(5,s.snap(0).joinedPieces)
        assertTrue(s.complete)
        s.reset(); assertFalse(s.complete); assertEquals(6,s.stashIds.size)
    }
    @Test fun mergedGroupsJoinWithoutChangingInternalOffsets() {
        val g = geometry(); val s = PuzzleSession(g)
        listOf(0,1,3,4).forEach { s.place(it,g.solvedPosition(it)+if(it<3) Offset(10f,10f) else Offset(20f,20f)) }
        s.snap(0,0f); s.snap(3,0f)
        assertEquals(2,s.snap(0,15f).joinedPieces)
        assertEquals(Offset(100f,100f),s.position(4)-s.position(0))
        s.returnToStash(0); assertEquals(6,s.stashIds.size)
    }
    @Test fun stashOrdering() {
        val s = PuzzleSession(geometry()); s.reorderStash(0,5)
        assertEquals(0,s.stashIds.last())
        s.sortStash(StashSort.COLOR,mapOf(0 to -1f))
        assertEquals(0,s.stashIds.first())
        s.sortStash(StashSort.SHAPE)
        assertEquals(s.stashIds.map { s.geometry.pieces[it].shapeKey }.sorted(),s.stashIds.map { s.geometry.pieces[it].shapeKey })
    }
}
