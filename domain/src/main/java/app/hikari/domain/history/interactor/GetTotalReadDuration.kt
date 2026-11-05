package app.hikari.domain.history.interactor

import dev.zacsweers.metro.Inject
import app.hikari.domain.history.repository.HistoryRepository

@Inject
class GetTotalReadDuration(
    private val repository: HistoryRepository,
) {

    suspend fun await(): Long {
        return repository.getTotalReadDuration()
    }
}
