package app.hikari.domain.track.interactor

import app.hikari.data.track.Tracker
import app.hikari.data.track.TrackerManager
import app.hikari.domain.track.interactor.GetTracks
import app.hikari.domain.track.interactor.UpsertTrack
import app.hikari.domain.track.model.toDbTrack
import app.hikari.domain.track.model.toDomainTrack
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

@Inject
class RefreshTracks(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val upsertTrack: UpsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     *
     * @return Failed updates.
     */
    suspend fun await(mangaId: Long): List<Pair<Tracker?, Throwable>> {
        return supervisorScope {
            return@supervisorScope getTracks.await(mangaId)
                .map { it to trackerManager.get(it.trackerId) }
                .filter { (_, service) -> service?.isLoggedIn == true }
                .map { (track, service) ->
                    async(Dispatchers.IO) {
                        return@async try {
                            val serviceNonNull =
                                service ?: return@async (null to IllegalStateException("Tracker service missing"))
                            val refreshed = serviceNonNull.refresh(track.toDbTrack())
                            val updatedTrack = refreshed.toDomainTrack()
                                ?: return@async (
                                    serviceNonNull to
                                        IllegalStateException("Failed to convert refreshed track")
                                    )
                            upsertTrack.await(updatedTrack)
                            syncChapterProgressWithTrack.await(mangaId, updatedTrack, serviceNonNull)
                            null
                        } catch (e: Throwable) {
                            service to e
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }
}
