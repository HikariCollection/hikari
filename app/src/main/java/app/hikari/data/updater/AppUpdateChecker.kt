package app.hikari.data.updater

import app.hikari.BuildConfig
import app.hikari.domain.base.BasePreferences
import app.hikari.domain.release.interactor.GetApplicationRelease
import app.hikari.util.system.isFossBuildType
import app.hikari.util.system.isNightlyBuildType
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours

@Inject
class AppUpdateChecker(
    private val getApplicationRelease: GetApplicationRelease,
    private val preferences: BasePreferences,
) {

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceCheck && now - preferences.lastAppCheck.get() < CHECK_INTERVAL_MS) {
                return@withContext GetApplicationRelease.Result.NoNewUpdate
            }

            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFoss = isFossBuildType,
                    isNightly = isNightlyBuildType,
                    commitCount = BuildConfig.COMMIT_COUNT.toIntOrNull() ?: 0,
                    versionName = BuildConfig.VERSION_NAME,
                    repository = GITHUB_REPO,
                    forceCheck = forceCheck,
                ),
            )
            preferences.lastAppCheck.set(now)
            result
        }
    }

    companion object {
        private val CHECK_INTERVAL_MS = 6.hours.inWholeMilliseconds
    }
}

val GITHUB_REPO = "HikariCollection/hikari"

val RELEASE_TAG: String by lazy {
    if (isNightlyBuildType) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
