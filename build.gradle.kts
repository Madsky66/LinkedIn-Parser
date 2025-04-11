import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

group = "com.madsky"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)

    // Kotlin X
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.swing)

    // Material Icons Extended
    implementation(libs.material.icons.extended)

    // OkHttp3
    implementation(libs.okhttp)

    // Java WebSocket
    implementation(libs.java.websocket)

    // JSONObject
    implementation(libs.json)

    // Apache POI
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Poink DSL
//        implementation("com.github.nwillc:poink:0.1.0")

    // Google
    implementation("com.google.api-client:google-api-client:1.16")
    implementation("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Client OAuth Google Cloud
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.39.0")
    implementation("com.google.oauth-client:google-oauth-client-java6:1.39.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "LinkedInParser"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "LinkedInParser"
                shortcut = true
                iconFile.set(project.file("src/main/composeResources/drawable/icon.ico"))
                upgradeUuid = "938f329d-3585-430d-bbca-304ff14f3dda"
                dirChooser = true
                perUserInstall = true
            }

            fromFiles(
//                "src/jvmMain/composeResources/LICENSE.txt",
                "src/main/composeResources/drawable/icon.ico",
                "src/main/resources/file/client_secret.json"
            )
            description = "LinkedIn Profile Scraper"
            vendor = "Madsky"
            copyright = "© 2025 Madsky. All rights reserved."
//            licenseFile.set(project.file("src/main/resources/LICENSE.txt"))
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = auto
}