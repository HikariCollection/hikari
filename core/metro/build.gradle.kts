plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "app.hikari.core.metro"
}

dependencies {
    implementation(libs.metro.runtime)
}
