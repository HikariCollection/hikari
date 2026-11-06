import app.hikari.gradle.extensions.alias
import app.hikari.gradle.extensions.hikarix
import app.hikari.gradle.extensions.libs
import app.hikari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidLibrary : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.library)
            alias(hikarix.plugins.android.base)
        }
    }
}
