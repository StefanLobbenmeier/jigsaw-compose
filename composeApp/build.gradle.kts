plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}
kotlin {
    jvm()
    js {
        browser { commonWebpackConfig { outputFileName = "jigsaw.js" } }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation(compose.desktop.currentOs) }
    }
}
compose.desktop { application { mainClass = "app.jigsaw.MainKt" } }
