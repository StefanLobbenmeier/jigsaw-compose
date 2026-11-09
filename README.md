# Jigsaw

A quiet puzzle table built with Kotlin and Compose Multiplatform, sharing UI and puzzle logic between browser and desktop.

## Development

Use Java 17 or newer. On the original development machine:

```sh
export JAVA_HOME=/Users/stefan/.sdkman/candidates/java/current
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :composeApp:jsBrowserDevelopmentRun
./gradlew :composeApp:run
```

Build a static website with `./gradlew :composeApp:jsBrowserDistribution`. Upload the contents of `composeApp/build/dist/js/productionExecutable` to static hosting, including GitHub Pages. All asset paths are relative to support project Pages URLs.

## Structure

- `composeApp/src/commonMain/kotlin/app/jigsaw`: shared UI and puzzle logic.
- `composeApp/src/jsMain`: browser entry point, file import and browser integrations.
- `composeApp/src/jvmMain`: desktop entry point and integrations.
- `composeApp/src/commonTest`: platform independent puzzle tests.

## Toolchain

Kotlin 2.4.21, Compose Multiplatform 1.12.2, Gradle 9.3.1. Compose Multiplatform shares the Jetpack Compose UI model across targets. Browser uses Kotlin/JS and Canvas-based Compose UI.

## Image rights

Bundled or downloaded gallery photographs must include source and license attribution. User-selected images stay on the device and must only be used with appropriate rights.

Toolchain references: [Compose 1.12.2 release](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2), [Kotlin 2.4.21 release](https://github.com/JetBrains/kotlin/releases/tag/v2.4.21), and [official build guide](https://kotlinlang.org/docs/multiplatform/build-and-run-kmp.html).

## Playing

Choose a bundled landscape or **Your image** (JPEG, PNG, or WebP; up to 20 MB and 36 megapixels). Imports are decoded locally and never uploaded. Select a grid size to start a new puzzle. Tap a stash thumbnail to place it on the table, then drag it into position. True neighbors snap together within roughly one fifth of a piece width, and connected groups move together. Groups can form anywhere on the table; the optional translucent guide is a reference, not an anchor.

Sort the stash by sampled image color or tab/socket shape. The arrows on thumbnails move them manually. Select a table piece and use **Return group** to send its connected group back to the stash. **Scatter** spreads all pieces on the table, while **Restart** generates fresh cuts and returns every piece to the stash. Puzzles have no rotation or saved progress in this first version.

See [image source and license details](IMAGE_CREDITS.md). The UI and engine are shared between desktop and browser; file picking is implemented separately for each platform.
