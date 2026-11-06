import app.hikari.gradle.tasks.GenerateMRTask

plugins {
    alias(hikarix.plugins.android.library)
    alias(hikarix.plugins.spotless)
}

android {
    namespace = "app.hikari.i18n"
}

androidComponents {
    onVariants { variant ->
        val javaSource = variant.sources.java ?: return@onVariants
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<GenerateMRTask>("generate${variantName}MR") {
            stringsFile.set(layout.projectDirectory.file("src/main/res/values/strings.xml"))
            pluralsFile.set(layout.projectDirectory.file("src/main/res/values/plurals.xml"))
        }
        javaSource.addGeneratedSourceDirectory(task) { it.outputDir }
    }
}

dependencies {
    api(libs.androidx.annotation)
}
