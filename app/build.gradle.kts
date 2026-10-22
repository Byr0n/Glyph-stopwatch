import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.byr0n.glyphstopwatch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.byr0n.glyphstopwatch"
        // The special use foreground service type needs Android 14; Glyph Matrix phones ship newer.
        minSdk = 34
        targetSdk = 35
        // GitHub Actions numbers each build, so the app can show which one is installed.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Nothing's licence does not allow redistributing the SDK, so fetch it from
// Nothing's developer GitHub the first time the project builds.
val glyphSdk = file("libs/glyph-matrix-sdk-2.0.aar")
val fetchGlyphSdk by tasks.registering {
    outputs.file(glyphSdk)
    onlyIf { !glyphSdk.exists() }
    doLast {
        glyphSdk.parentFile.mkdirs()
        URI("https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/raw/main/glyph-matrix-sdk-2.0.aar")
            .toURL().openStream().use { input -> glyphSdk.outputStream().use { input.copyTo(it) } }
    }
}
tasks.named("preBuild") { dependsOn(fetchGlyphSdk) }

dependencies {
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation("androidx.core:core-ktx:1.13.1")

    testImplementation("junit:junit:4.13.2")
}
