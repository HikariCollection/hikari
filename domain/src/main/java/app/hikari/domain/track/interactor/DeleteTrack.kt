package app.hikari.domain.track.interactor

import app.hikari.core.common.util.system.logcat
import app.hikari.domain.track.repository.TrackRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class DeleteTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(mangaId: Long, trackerId: Long) {
        try {
            trackRepository.delete(mangaId, trackerId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
