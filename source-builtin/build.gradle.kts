plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)

    alias(libs.plugins.metro)
    kotlin("plugin.serialization")
}

android {
    namespace = "app.hikari.source.builtin"
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.core.common)
    implementation(projects.domain)
    implementation(projects.i18n)

    implementation(libs.metro.runtime)
    implementation(libs.bundles.serialization)
    implementation(libs.jsoup)
    implementation(libs.injekt)
    implementation(libs.kotlinx.datetime)
    implementation(libs.okhttp.brotli)
}
