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
