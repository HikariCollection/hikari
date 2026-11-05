import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import app.hikari.gradle.configurations.configureKotlin
import app.hikari.gradle.extensions.alias
import app.hikari.gradle.extensions.configureTest
import app.hikari.gradle.extensions.coreLibraryDesugaring
import app.hikari.gradle.extensions.libs
import app.hikari.gradle.extensions.hikarix
import app.hikari.gradle.extensions.plugins
import app.hikari.gradle.extensions.release
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("UNUSED")
class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.kmp.library)
            alias(libs.plugins.kotlin.multiplatform)
        }

        configureKotlin()
        configureTest()

        kotlin {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate()

            android {
                minSdk = hikarix.versions.android.sdk.min.get().toInt()
                compileSdk {
                    version = release(hikarix.versions.android.sdk.compile)
                }
                enableCoreLibraryDesugaring = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
