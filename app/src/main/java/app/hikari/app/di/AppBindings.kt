package app.hikari.app.di

import app.hikari.core.common.util.system.logcat
import app.hikari.core.metro.AppCoroutineScope
import app.hikari.core.metro.IsDebugBuild
import app.hikari.data.backup.models.backupProtoBuf
import app.hikari.network.AndroidNetworkHelper
import app.hikari.network.NetworkHelper
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML

@BindingContainer
object AppBindings {

    @Provides
    @SingleIn(AppScope::class)
    @AppCoroutineScope
    fun providesAppCoroutineScope(@IsDebugBuild isDebugBuild: Boolean): CoroutineScope {
        // Work here outlives the screen that started it, so a failure would crash the app on whatever screen is open
        // by then. Debug builds still crash so the bug gets noticed.
        val handler = CoroutineExceptionHandler { _, throwable ->
            if (isDebugBuild) {
                val thread = Thread.currentThread()
                thread.uncaughtExceptionHandler?.uncaughtException(thread, throwable)
            } else {
                logcat(LogPriority.ERROR, throwable) { "Uncaught exception in the app scope" }
            }
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesXML(): XML = XML.v1 {
        policy {
            ignoreUnknownChildren()
            autoPolymorphic = true
        }
        xmlDeclMode = XmlDeclMode.Charset
        xmlVersion = XmlVersion.XML10
        setIndent(2)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesProtoBuf(): ProtoBuf = backupProtoBuf

    @Provides
    @SingleIn(AppScope::class)
    fun providesNetworkHelper(impl: AndroidNetworkHelper): NetworkHelper = impl
}
