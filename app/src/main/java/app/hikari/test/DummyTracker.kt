package app.hikari.test

import app.hikari.R
import app.hikari.data.track.RefreshResult
import app.hikari.data.track.Tracker
import app.hikari.data.track.model.TrackSearch
import app.hikari.domain.track.model.Track
import app.hikari.i18n.MR
import app.hikari.i18n.StringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient

data class DummyTracker(
    override val id: Long,
    override val name: String,
    override val supportsReadingDates: Boolean = false,
    override val supportsPrivateTracking: Boolean = false,
    override val isLoggedIn: Boolean = false,
    override val isLoggedInFlow: Flow<Boolean> = flowOf(false),
    override val isRefreshingFlow: StateFlow<Boolean> = MutableStateFlow(false),
    override val refreshResultFlow: SharedFlow<RefreshResult> = MutableSharedFlow<RefreshResult>().asSharedFlow(),
    val valLogo: Int = R.drawable.brand_anilist,
    val valStatuses: List<Long> = (1L..6L).toList(),
    val valReadingStatus: Long = 1L,
    val valRereadingStatus: Long = 1L,
    val valCompletionStatus: Long = 2L,
    val valScoreList: List<String> = (0..10).map(Int::toString),
    val val10PointScore: Double = 5.4,
    val valSearchResults: List<TrackSearch> = listOf(),
) : Tracker {

    override val client: OkHttpClient
        get() = TODO("Not yet implemented")

    override fun getLogo(): Int = valLogo

    override fun getStatusList(): List<Long> = valStatuses

    override fun getStatus(status: Long): StringResource? = when (status) {
        1L -> MR.strings.reading
        2L -> MR.strings.plan_to_read
        3L -> MR.strings.completed
        4L -> MR.strings.on_hold
        5L -> MR.strings.dropped
        6L -> MR.strings.repeating
        else -> null
    }

    override fun getReadingStatus(): Long = valReadingStatus

    override fun getRereadingStatus(): Long = valRereadingStatus

    override fun getCompletionStatus(): Long = valCompletionStatus

    override fun getScoreList(): List<String> = valScoreList

    override fun get10PointScore(track: Track): Double = val10PointScore

    override fun indexToScore(index: Int): Double = getScoreList()[index].toDouble()

    override fun displayScore(track: Track): String =
        track.score.toString()

    override suspend fun update(
        track: app.hikari.data.database.models.Track,
        didReadChapter: Boolean,
    ): app.hikari.data.database.models.Track = track

    override suspend fun bind(
        track: app.hikari.data.database.models.Track,
        hasReadChapters: Boolean,
    ): app.hikari.data.database.models.Track = track

    override suspend fun search(query: String): List<TrackSearch> = valSearchResults

    override suspend fun refresh(
        track: app.hikari.data.database.models.Track,
    ): app.hikari.data.database.models.Track = track

    override suspend fun refreshUser() = Unit

    override suspend fun login(username: String, password: String) = Unit

    override fun logout() = Unit

    override fun getUsername(): String = "username"

    override fun getDisplayUsername(): String = "UserName"

    override fun saveDisplayUsername(displayName: String): Unit = Unit

    override fun getPassword(): String = "passw0rd"

    override fun saveCredentials(username: String, password: String) = Unit

    override suspend fun register(
        item: app.hikari.data.database.models.Track,
        mangaId: Long,
    ) = Unit

    override suspend fun setRemoteStatus(
        track: app.hikari.data.database.models.Track,
        status: Long,
    ) = Unit

    override suspend fun setRemoteLastChapterRead(
        track: app.hikari.data.database.models.Track,
        chapterNumber: Int,
    ) = Unit

    override suspend fun setRemoteScore(
        track: app.hikari.data.database.models.Track,
        scoreString: String,
    ) = Unit

    override suspend fun setRemoteStartDate(
        track: app.hikari.data.database.models.Track,
        epochMillis: Long,
    ) = Unit

    override suspend fun setRemoteFinishDate(
        track: app.hikari.data.database.models.Track,
        epochMillis: Long,
    ) = Unit

    override suspend fun setRemotePrivate(
        track: app.hikari.data.database.models.Track,
        private: Boolean,
    ) = Unit
}
