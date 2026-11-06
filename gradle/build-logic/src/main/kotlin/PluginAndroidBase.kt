import app.hikari.gradle.configurations.configureKotlin
import app.hikari.gradle.extensions.android
import app.hikari.gradle.extensions.configureTest
import app.hikari.gradle.extensions.coreLibraryDesugaring
import app.hikari.gradle.extensions.hikarix
import app.hikari.gradle.extensions.libs
import app.hikari.gradle.extensions.release
import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.DefaultConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        configureKotlin()
        configureTest()

        android {
            defaultConfig {
                minSdk = hikarix.versions.android.sdk.min.get().toInt()
                if (this is ApplicationDefaultConfig) {
                    targetSdk = hikarix.versions.android.sdk.target.get().toInt()
                }

                ndkVersion = hikarix.versions.android.ndk.get()
            }

            compileSdk {
                version = release(hikarix.versions.android.sdk.compile)
            }

            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun CommonExtension.defaultConfig(block: DefaultConfig.() -> Unit) {
    defaultConfig.apply(block)
}

private fun CommonExtension.compileOptions(block: CompileOptions.() -> Unit) {
    compileOptions.apply(block)
}
