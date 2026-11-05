package app.hikari.data.updater

import dev.zacsweers.metro.Inject
import app.hikari.BuildConfig
import app.hikari.util.system.isFossBuildType
import app.hikari.util.system.isNightlyBuildType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.hikari.domain.release.interactor.GetApplicationRelease

@Inject
class AppUpdateChecker(
    private val getApplicationRelease: GetApplicationRelease,
) {

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        return GetApplicationRelease.Result.NoNewUpdate
    }
}

val GITHUB_REPO: String by lazy {
    if (isNightlyBuildType) {
        "mihonapp/app.hikari-preview"
    } else {
        "mihonapp/app.hikari"
    }
}

val RELEASE_TAG: String by lazy {
    if (isNightlyBuildType) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
