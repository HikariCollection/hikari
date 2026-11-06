package app.hikari.domain.track.interactor

import app.hikari.domain.track.model.Track
import app.hikari.domain.track.repository.TrackRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class GetTracksPerManga(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(): Map<Long, List<Track>> {
        return trackRepository.getTracks().groupBy { it.mangaId }
    }

    fun subscribe(): Flow<Map<Long, List<Track>>> {
        return trackRepository.getTracksAsFlow().map { tracks -> tracks.groupBy { it.mangaId } }
    }
}
