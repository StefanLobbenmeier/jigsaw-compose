@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package app.jigsaw

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import app.jigsaw.puzzle.*
import jigsaw.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.*

private val Ink = Color(0xFF173C35)
private val Paper = Color(0xFFF7F5EF)
private data class Photo(val title: String, val file: String, val credit: String)
private val photos = listOf(Photo("Alpine escape", "alpine.jpg", "Unsplash · photo-1501785888041-af3ef285b470"), Photo("Still waters", "lake.jpg", "Unsplash · photo-1470770841072-f978cf4d019e"))

@Composable fun App() {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var title by remember { mutableStateOf("Alpine escape") }
    var credit by remember { mutableStateOf(photos[0].credit) }
    var columns by remember { mutableIntStateOf(4) }
    var seed by remember { mutableIntStateOf(42) }
    var revision by remember { mutableIntStateOf(0) }
    var reference by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("Tap a stash piece to put it on the table. Drag matching pieces close to join.") }
    var sort by remember { mutableStateOf(StashSort.MANUAL) }
    val scope = rememberCoroutineScope()
    val bitmap = image
    val geometry = remember(bitmap, columns, seed) { PuzzleGeometry(columns, maxOf(2, (columns * (bitmap?.height ?: 800).toFloat() / (bitmap?.width ?: 1200)).roundToInt()), 800f, 800f * (bitmap?.height ?: 800) / (bitmap?.width ?: 1200), seed) }
    val session = remember(geometry) { PuzzleSession(geometry).apply { reset(seed) } }
    val zOrder = remember(session) { mutableStateListOf<Int>() }
    val colorKeys = remember(bitmap, geometry) { bitmap?.let { colorKeys(it, geometry) } ?: emptyMap() }
    fun changed() { revision++ }
    fun load(bytes: ByteArray, name: String, attribution: String) {
        try {
            val decoded = bytes.decodeToImageBitmap()
            require(decoded.width.toLong() * decoded.height <= 36_000_000) { "Choose an image under 36 megapixels." }
            require(decoded.width.toFloat() / decoded.height in 0.4f..2.5f) { "Choose an image with an aspect ratio between 2:5 and 5:2. Crop very tall or wide images first." }
            image = decoded; title = name; credit = attribution; selected = null; sort = StashSort.MANUAL
            message = "Your table is ready. Tap a piece below to begin."
        } catch (e: Exception) { message = e.message ?: "This image format could not be opened. Try JPEG or PNG." }
    }
    LaunchedEffect(Unit) {
        appReady()
        try { load(Res.readBytes("files/alpine.jpg"), photos[0].title, photos[0].credit) }
        catch (e: Exception) { message = "Gallery could not load. Use your own image to start." }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Paper, surface = Color.White, secondaryContainer = Color(0xFFE0EADD))) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Paper)) {
            val compact = maxWidth < 760.dp
            Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("jigsaw", fontSize = 30.sp, color = Ink)
                    Spacer(Modifier.width(14.dp))
                    Text("a little space to piece things together", color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${session.joinedCount} / ${geometry.pieces.size - 1} joins", style = MaterialTheme.typography.labelLarge)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    photos.forEach { photo -> OutlinedButton(onClick = { scope.launch { runCatching { Res.readBytes("files/${photo.file}") }.onSuccess { load(it, photo.title, photo.credit) }.onFailure { message = "Unable to open gallery photo." } } }) { Text(photo.title) } }
                    Button(onClick = { pickImage { bytes, name -> scope.launch { if (bytes != null) load(bytes, name ?: "Your image", "Your image · stays on this device") else if (name != null) message = name } } }) { Text("＋ Your image") }
                    listOf(3,4,6,8).forEach { n -> FilterChip(selected = columns == n, onClick = { columns = n; selected = null }, label = { Text("${n} × ${maxOf(2, (n * geometry.height / geometry.width).roundToInt())}") }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(if (session.complete) "Beautifully done. Every piece belongs." else message, fontSize = 12.sp, color = Ink) }
                    TextButton(onClick = { reference = !reference }) { Text(if (reference) "Hide guide" else "Show guide") }
                }
                if (bitmap != null) {
                    PuzzleTable(bitmap, session, revision, zOrder, selected, reference, Modifier.fillMaxWidth().weight(1f), onSelect = { selected = it }, onChange = { changed() }, onJoin = { count -> if (count > 0) message = "Nice fit! Joined pieces now move together." })
                } else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Opening your puzzle table…") }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("STASH · ${session.stashIds.size}", style = MaterialTheme.typography.labelMedium)
                    StashSort.entries.forEach { mode -> FilterChip(sort == mode, onClick = { sort = mode; session.sortStash(mode, colorKeys); changed() }, label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                    TextButton(onClick = { selected?.takeIf { !session.isInStash(it) }?.let { session.returnToStash(it); changed(); selected = null } }, enabled = selected != null && !session.isInStash(selected!!)) { Text("Return group") }
                    TextButton(onClick = { session.scatter(1100f, maxOf(800f, geometry.height + 200)); zOrder.clear(); zOrder.addAll(session.boardIds); changed() }) { Text("Scatter") }
                    TextButton(onClick = { seed++; selected = null; message = "Fresh start. All pieces are back in the stash." }) { Text("Restart") }
                }
                if (bitmap != null) {
                    LazyVerticalGrid(GridCells.Adaptive(88.dp), Modifier.fillMaxWidth().height(if (compact) 160.dp else 190.dp).background(Color(0xFFE8E9DF), RoundedCornerShape(16.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(session.stashIds, key = { it }) { id ->
                            Column(Modifier.background(Color.White, RoundedCornerShape(10.dp)).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Canvas(Modifier.fillMaxWidth().height(65.dp).clickable {
                                    session.place(id, Offset(100f + (id % 5) * 24f, 80f + (id % 3) * 24f)); zOrder.remove(id); zOrder.add(id); selected = id; changed()
                                }) {
                                    val s = minOf(size.width / (geometry.cellWidth * 1.6f), size.height / (geometry.cellHeight * 1.6f))
                                    withTransform({ translate((size.width - geometry.cellWidth * s)/2, (size.height - geometry.cellHeight * s)/2); scale(s,s,Offset.Zero) }) { drawPiece(bitmap, geometry, id, false) }
                                }
                                Row(horizontalArrangement = Arrangement.Center) {
                                    Text("${id+1}", fontSize = 10.sp, color = Ink, modifier = Modifier.padding(4.dp))
                                    Text("←", fontSize = 14.sp, modifier = Modifier.clickable { session.reorderStash(id, session.stashIds.indexOf(id)-1); sort = StashSort.MANUAL; changed() }.padding(horizontal = 6.dp))
                                    Text("→", fontSize = 14.sp, modifier = Modifier.clickable { session.reorderStash(id, session.stashIds.indexOf(id)+1); sort = StashSort.MANUAL; changed() }.padding(horizontal = 6.dp))
                                }
                            }
                        }
                    }
                }
                Text("$credit  •  ${if (session.complete) "Puzzle complete!" else "No rotation · drag to join · select a group to return it"}", fontSize = 10.sp, color = Ink, maxLines = 1)
            }
        }
    }
}

private fun DrawScope.drawPiece(image: ImageBitmap, geometry: PuzzleGeometry, id: Int, selected: Boolean) {
    val path = geometry.path(id)
    val solved = geometry.solvedPosition(id)
    clipPath(path) {
        drawImage(image, dstOffset = IntOffset(-solved.x.roundToInt(), -solved.y.roundToInt()), dstSize = IntSize(geometry.width.roundToInt(), geometry.height.roundToInt()), filterQuality = FilterQuality.Medium)
    }
    drawPath(path, if (selected) Color(0xFFE6AC48) else Color(0xFF254139).copy(alpha=.65f), style = Stroke(if (selected) 3f else 1.2f))
}

@Composable private fun PuzzleTable(image: ImageBitmap, session: PuzzleSession, revision: Int, order: MutableList<Int>, selected: Int?, reference: Boolean, modifier: Modifier, onSelect: (Int?) -> Unit, onChange: () -> Unit, onJoin: (Int) -> Unit) {
    val geometry = session.geometry
    val tableHeight = maxOf(800f, geometry.height + 220f)
    val hitTesters = remember(geometry) { geometry.pieces.associate { it.id to PathHitTester(geometry.path(it.id)) } }
    var active by remember(session) { mutableStateOf<Int?>(null) }
    Box(modifier.background(Color(0xFFDCE5DB), RoundedCornerShape(18.dp))) {
        Canvas(Modifier.fillMaxSize().pointerInput(session) {
            detectTapGestures { point ->
                val s=minOf(size.width/1200f,size.height/tableHeight)
                val local=(point-Offset((size.width-1200f*s)/2,(size.height-tableHeight*s)/2))/s
                val ids=(order.filter { it in session.boardIds } + session.boardIds.filter { it !in order }).asReversed()
                onSelect(ids.firstOrNull { id -> val p=local-session.position(id); p in hitTesters.getValue(id) })
            }
        }.pointerInput(session) {
            fun scale() = minOf(size.width/1200f, size.height/tableHeight)
            fun origin(): Offset { val s=scale(); return Offset((size.width-1200f*s)/2, (size.height-tableHeight*s)/2) }
            detectDragGestures(onDragStart = { point ->
                val local = (point-origin())/scale()
                val ids = (order.filter { it in session.boardIds } + session.boardIds.filter { it !in order }).asReversed()
                active = ids.firstOrNull { id -> val p = local-session.position(id); p in hitTesters.getValue(id) }
                onSelect(active)
                active?.let { id -> val members=session.groupIds(id); order.removeAll(members); order.addAll(members); onChange() }
            }, onDrag = { change, amount -> change.consume(); active?.let { session.moveGroup(it, amount/scale()); onChange() } }, onDragEnd = {
                active?.let { id ->
                    // Keep the entire group within reach, without changing its alignment.
                    val members=session.groupIds(id); val minX=members.minOf { session.position(it).x }; val minY=members.minOf { session.position(it).y }
                    val maxX=members.maxOf { session.position(it).x + geometry.cellWidth }; val maxY=members.maxOf { session.position(it).y + geometry.cellHeight }
                    val dx=if(minX<20) 20-minX else if(maxX>1180) 1180-maxX else 0f
                    val dy=if(minY<20) 20-minY else if(maxY>tableHeight-20) tableHeight-20-maxY else 0f
                    session.moveGroup(id, Offset(dx,dy)); onJoin(session.snap(id).joinedPieces); onChange()
                }; active=null
            }, onDragCancel = { active=null })
        }) {
            val observed = revision
            val s=minOf(size.width/1200f,size.height/tableHeight)
            withTransform({ translate((size.width-1200f*s)/2,(size.height-tableHeight*s)/2); scale(s,s,Offset.Zero) }) {
                drawRoundRect(Color.White.copy(alpha=.27f), Offset(180f,110f), androidx.compose.ui.geometry.Size(geometry.width, geometry.height), androidx.compose.ui.geometry.CornerRadius(8f))
                if (reference) drawImage(image, dstOffset=IntOffset(180,110), dstSize=IntSize(geometry.width.roundToInt(),geometry.height.roundToInt()), alpha=.32f)
                val ids=order.filter { it in session.boardIds } + session.boardIds.filter { it !in order }
                val highlighted=selected?.let { session.groupIds(it) } ?: emptyList()
                ids.forEach { id -> val p=session.position(id); withTransform({translate(p.x,p.y)}) { drawPiece(image,geometry,id,id in highlighted) } }
            }
        }
        if (session.boardIds.isEmpty()) Text("Your quiet corner starts with one piece.\nTap any piece in the stash below.", Modifier.align(Alignment.Center).padding(20.dp), color=Ink, style=MaterialTheme.typography.bodyLarge)
        if (session.complete) Surface(Modifier.align(Alignment.TopCenter).padding(12.dp), color=Ink, shape=RoundedCornerShape(20.dp)) { Text("✓ Puzzle complete — beautifully done", Modifier.padding(horizontal=18.dp,vertical=10.dp), color=Color.White) }
    }
}

private fun colorKeys(image: ImageBitmap, geometry: PuzzleGeometry): Map<Int, Float> {
    val pixels=image.toPixelMap()
    return geometry.pieces.associate { piece ->
        var r=0f; var g=0f; var b=0f
        for (y in 1..5) for(x in 1..5) {
            val px=((piece.column+(x/6f))*image.width/geometry.columns).toInt().coerceIn(0,image.width-1)
            val py=((piece.row+(y/6f))*image.height/geometry.rows).toInt().coerceIn(0,image.height-1)
            val c=pixels[px,py]; r+=c.red; g+=c.green; b+=c.blue
        }
        val hi=maxOf(r,g,b); val lo=minOf(r,g,b); val d=hi-lo
        var hue=if(d<.001f) 0f else when(hi) { r -> ((g-b)/d)%6; g -> (b-r)/d+2; else -> (r-g)/d+4 }
        if(hue<0) hue+=6
        piece.id to (hue*100f+hi/25f)
    }
}
