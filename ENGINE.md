# Puzzle engine integration

`PuzzleGeometry(columns, rows, width, height, seed)` is immutable. Width/height are the desired solved image dimensions in logical board units. Every internal boundary is one `Edge` instance used by both neighboring pieces; seeded control-point variations make the silhouette reproducible. Border edges are straight. A piece's local origin is its solved cell's top-left (tabs may extend outside that cell).

For rendering a board piece:

1. Translate the canvas to `session.position(id)`.
2. Clip with `geometry.path(id)`.
3. Draw the entire source image, scaled to `geometry.width × geometry.height`, at `-geometry.solvedPosition(id)`.
4. Draw a subtle outline with the same path. Do not clip the piece's canvas/container to its rectangular cell: tabs extend about 27% of a cell beyond the origin/size.

For stash thumbnails, use the same path/image offsets at a smaller canvas scale. Hue/color sorting can sample pixels within each piece's rectangular source cell (ignore tabs) and supply a numeric key to `sortStash(COLOR, keys)`. `shapeKey` encodes flat/inward/outward edges, clockwise from the top; `isBorder` enables a border filter.

`PuzzleSession` is mutable and deliberately independent of UI state. Hold it with `remember`, then increment a Compose revision state after each mutation so canvas and lists redraw. Read the revision inside the drawing composable. Use `stashIds`, `boardIds`, `groupIds`, `position`, `joinedCount`, and `complete` for UI.

- New session starts with all pieces in the stash; `reset(seed)` shuffles them.
- `place(id, boardPosition)` takes a single piece out of the stash. Use only for a stash piece; board gestures use `moveGroup(id, delta)`.
- On drag release, `snap(id, tolerance)` aligns the moved group to its nearest qualifying real neighbor, merges the groups, and repeats for additional close neighbors. Default tolerance is 22% of the smaller cell dimension. It compares translations from solved positions, so a group can form anywhere on the table without first anchoring to the board origin. It never joins wrong neighbors merely because their visual shapes resemble each other.
- `snap` returns newly joined piece count and completion status. Completion means every piece is on the table and in one connected group.
- `returnToStash(id)` unjoins the whole group and returns its members to the stash.
- `reorderStash(id, targetIndex)` supports manual drag ordering; `sortStash(MANUAL)` preserves that ordering. Automatic sort uses numeric image-color keys or clockwise shape signatures.
- `scatter(areaWidth, areaHeight, seed)` starts an unjoined puzzle with all pieces spread across a table area.

The engine does not enforce table bounds or z-order. Keep board rendering order in UI (bring a dragged group to the front), and allow room around the solved board so groups can form anywhere. Rotation is intentionally absent in this first version.

Run core tests: `JAVA_HOME=/Users/stefan/.sdkman/candidates/java/current PATH=/Users/stefan/.sdkman/candidates/java/current/bin:$PATH ./gradlew :composeApp:jvmTest`.
