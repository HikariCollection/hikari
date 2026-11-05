plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.hikari.core.metadata"
}

dependencies {
    implementation(projects.sourceApi)

    implementation(libs.bundles.serialization)
}
