package app.hikari.domain.track.service

import app.hikari.core.common.preference.Preference
import app.hikari.core.common.preference.PreferenceStore
import app.hikari.core.common.preference.getEnum
import app.hikari.data.track.Tracker
import app.hikari.data.track.anilist.Anilist
import app.hikari.domain.track.model.AutoTrackState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class TrackPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun trackUsername(tracker: Tracker) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_username_${tracker.id}"),
        "",
    )

    fun trackDisplayUsername(tracker: Tracker) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_displayname_${tracker.id}"),
        "",
    )

    fun trackPassword(tracker: Tracker) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_password_${tracker.id}"),
        "",
    )

    fun trackAuthExpired(tracker: Tracker) = preferenceStore.getBoolean(
        Preference.privateKey("pref_tracker_auth_expired_${tracker.id}"),
        false,
    )

    fun setCredentials(tracker: Tracker, username: String, password: String) {
        trackUsername(tracker).set(username)
        trackPassword(tracker).set(password)
        trackAuthExpired(tracker).set(false)
    }

    fun trackToken(tracker: Tracker) = preferenceStore.getString(Preference.privateKey("track_token_${tracker.id}"), "")

    val anilistScoreType: Preference<String> = preferenceStore.getString("anilist_score_type", Anilist.POINT_10)

    val autoUpdateTrack: Preference<Boolean> = preferenceStore.getBoolean("pref_auto_update_manga_sync_key", true)

    val autoUpdateTrackOnMarkRead: Preference<AutoTrackState> = preferenceStore.getEnum(
        "pref_auto_update_manga_on_mark_read",
        AutoTrackState.ALWAYS,
    )
}
