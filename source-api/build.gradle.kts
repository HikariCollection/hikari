plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

android {
    namespace = "app.hikari.source"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "app.hikari"
            artifactId = "source-api"
            version = "1.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "buildDir"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

dependencies {
    api(libs.okhttp.core)
    api(libs.okio)
    api(libs.rxJava)
    api(libs.jsoup)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.jsonOkio)
    api(libs.injekt)
    api(libs.bundles.kotlinx.coroutines)

    api(libs.androidx.preference)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}
