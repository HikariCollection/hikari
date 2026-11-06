import app.hikari.gradle.extensions.alias
import app.hikari.gradle.extensions.android
import app.hikari.gradle.extensions.api
import app.hikari.gradle.extensions.debugApi
import app.hikari.gradle.extensions.implementation
import app.hikari.gradle.extensions.libs
import app.hikari.gradle.extensions.plugins
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginComposeAndroid : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.compose.compiler)
        }

        android {
            buildFeatures {
                compose = true
            }
        }

        dependencies {
            implementation(platform(libs.androidx.compose.bom))

            // Compose @Preview tooling
            api(libs.androidx.compose.uiToolingPreview)
            debugApi(libs.androidx.compose.uiTooling)
        }
    }
}

private fun CommonExtension.buildFeatures(block: BuildFeatures.() -> Unit) {
    buildFeatures.apply(block)
}
