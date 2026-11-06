plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.spotless)
    `java-gradle-plugin`
}

// Configuration should be synced with [/gradle/build-logic/src/main/kotlin/PluginSpotless.kt]
val ktlintVersion = libs.ktlint.bom.get().version
val editorConfigFile = rootProject.file("../../.editorconfig")
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.kts")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradle)
    compileOnly(libs.android.gradle.runtimeDependency("sdk-common"))
    compileOnly(libs.kotlin.compose.compiler.gradle)
    compileOnly(libs.kotlin.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.tapmoc.gradle)

    // These allow us to reference the dependency catalog inside our compiled plugins
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(hikarix::class.java.superclass.protectionDomain.codeSource.location))
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

gradlePlugin {
    plugins {
        register("android-application") {
            id = hikarix.plugins.android.application.get().pluginId
            implementationClass = "PluginAndroidApplication"
        }
        register("android-base") {
            id = hikarix.plugins.android.base.get().pluginId
            implementationClass = "PluginAndroidBase"
        }
        register("android-library") {
            id = hikarix.plugins.android.library.get().pluginId
            implementationClass = "PluginAndroidLibrary"
        }
        register("android-test") {
            id = hikarix.plugins.android.test.get().pluginId
            implementationClass = "PluginAndroidTest"
        }
        register("compose-android") {
            id = hikarix.plugins.compose.get().pluginId
            implementationClass = "PluginComposeAndroid"
        }
        register("spotless") {
            id = hikarix.plugins.spotless.get().pluginId
            implementationClass = "PluginSpotless"
        }
    }
}

fun Provider<MinimalExternalModuleDependency>.runtimeDependency(name: String): Provider<String> = map { dependency ->
    configurations.detachedConfiguration(dependencies.create(dependency))
        .attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
        .incoming.resolutionResult.allComponents
        .firstOrNull { it.moduleVersion?.name == name }
        ?.moduleVersion
        ?.toString()
        ?: error("${dependency.module} has no runtime dependency named $name")
}
