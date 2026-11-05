plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.compose)

    alias(hikarix.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

android {
    namespace = "app.hikari.icons.simpleicons"
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

valkyrie {
    packageName = "app.hikari.icons.simpleicons"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "SimpleIcons"
        targetSourceSet = "main"
    }
}

dependencies {
    api(libs.androidx.compose.ui)
}
