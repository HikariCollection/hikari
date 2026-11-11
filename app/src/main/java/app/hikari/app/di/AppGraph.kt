package app.hikari.app.di

import android.content.Context
import app.hikari.App
import app.hikari.core.metro.IsDebugBuild
import app.hikari.core.security.SecurityPreferences
import app.hikari.data.backup.create.BackupCreateWorker
import app.hikari.data.backup.restore.BackupRestoreWorker
import app.hikari.data.cache.ChapterCache
import app.hikari.data.cache.CoverCache
import app.hikari.data.download.DownloadCache
import app.hikari.data.download.DownloadManager
import app.hikari.data.download.DownloadWorker
import app.hikari.data.library.LibraryUpdateWorker
import app.hikari.data.library.MetadataUpdateWorker
import app.hikari.data.notification.NotificationReceiver
import app.hikari.data.track.TrackerManager
import app.hikari.data.updater.AppUpdateChecker
import app.hikari.domain.backup.service.BackupPreferences
import app.hikari.domain.base.BasePreferences
import app.hikari.domain.category.interactor.GetCategories
import app.hikari.domain.category.interactor.ResetCategoryFlags
import app.hikari.domain.download.service.DownloadPreferences
import app.hikari.domain.extension.interactor.GetExtensionStoreCountAsFlow
import app.hikari.domain.extension.interactor.TrustExtension
import app.hikari.domain.extension.repository.ExtensionStoreRepository
import app.hikari.domain.library.service.LibraryPreferences
import app.hikari.domain.manga.interactor.GetFavorites
import app.hikari.domain.manga.interactor.GetHiddenManga
import app.hikari.domain.manga.interactor.ResetViewerFlags
import app.hikari.domain.source.service.SourceManager
import app.hikari.domain.source.service.SourcePreferences
import app.hikari.domain.storage.service.StoragePreferences
import app.hikari.domain.track.interactor.AddTracks
import app.hikari.domain.track.interactor.UpsertTrack
import app.hikari.domain.track.service.DelayedTrackingUpdateWorker
import app.hikari.domain.track.service.TrackPreferences
import app.hikari.domain.ui.UiPreferences
import app.hikari.extension.ExtensionManager
import app.hikari.network.JavaScriptEngine
import app.hikari.network.NetworkHelper
import app.hikari.network.NetworkPreferences
import app.hikari.ui.base.delegate.SecureActivityDelegateImpl
import app.hikari.ui.main.MainActivity
import app.hikari.ui.reader.ReaderActivity
import app.hikari.ui.reader.setting.ReaderPreferences
import app.hikari.ui.setting.track.BaseOAuthLoginActivity
import app.hikari.ui.webview.WebViewActivity
import app.hikari.util.CrashLogUtil
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import nl.adaptivity.xmlutil.serialization.XML

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [AppBindings::class],
)
interface AppGraph : ViewModelGraph {
    fun inject(app: App)
    fun inject(mainActivity: MainActivity)
    fun inject(readerActivity: ReaderActivity)
    fun inject(webViewActivity: WebViewActivity)
    fun inject(baseOAuthLoginActivity: BaseOAuthLoginActivity)
    fun inject(libraryUpdateWorker: LibraryUpdateWorker)
    fun inject(metadataUpdateWorker: MetadataUpdateWorker)
    fun inject(backupRestoreWorker: BackupRestoreWorker)
    fun inject(backupCreateWorker: BackupCreateWorker)
    fun inject(delayedTrackingUpdateWorker: DelayedTrackingUpdateWorker)
    fun inject(downloadWorker: DownloadWorker)
    fun inject(notificationReceiver: NotificationReceiver)
    fun inject(notificationReceiver: SecureActivityDelegateImpl)

    val context: Context

    val viewModelFactory: MetroViewModelFactory

    val basePreferences: BasePreferences
    val uiPreferences: UiPreferences
    val readerPreferences: ReaderPreferences
    val networkPreferences: NetworkPreferences
    val libraryPreferences: LibraryPreferences
    val sourcePreferences: SourcePreferences
    val trackPreferences: TrackPreferences
    val backupPreferences: BackupPreferences
    val storagePreferences: StoragePreferences
    val securityPreferences: SecurityPreferences
    val downloadPreferences: DownloadPreferences

    val crashLogUtil: CrashLogUtil

    val downloadManager: DownloadManager

    val updateChecker: AppUpdateChecker

    val trustExtension: TrustExtension
    val extensionStoreRepository: ExtensionStoreRepository

    val sourceManager: SourceManager
    val trackerManager: TrackerManager
    val extensionManager: ExtensionManager
    val chapterCache: ChapterCache
    val coverCache: CoverCache
    val downloadCache: DownloadCache

    val json: Json
    val protoBuf: ProtoBuf
    val xml: XML
    val networkHelper: NetworkHelper
    val javaScriptEngine: JavaScriptEngine

    val getFavorites: GetFavorites
    val getHiddenManga: GetHiddenManga
    val getCategories: GetCategories
    val resetViewerFlags: ResetViewerFlags
    val resetCategoryFlags: ResetCategoryFlags
    val addTracks: AddTracks
    val upsertTrack: UpsertTrack

    val getExtensionStoreCountAsFlow: GetExtensionStoreCountAsFlow

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context, @Provides @IsDebugBuild isDebugBuild: Boolean): AppGraph
    }
}
