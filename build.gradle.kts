// Versiones verificadas (ver README, sección "Decisiones técnicas"):
//   AGP 8.13.0  -> compatible con Chaquopy 17.0.0 (AGP 8.9 a 8.13) y Gradle 8.14.x
//   Kotlin 2.2.20 + plugin Compose Compiler de Kotlin
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.chaquo.python") version "17.0.0" apply false
}
