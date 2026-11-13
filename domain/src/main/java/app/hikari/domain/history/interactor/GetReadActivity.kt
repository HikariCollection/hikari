package app.hikari.domain.history.interactor

import app.hikari.domain.history.model.ReadActivityEntry
import app.hikari.domain.history.repository.HistoryRepository
import dev.zacsweers.metro.Inject

@Inject
class GetReadActivity(
    private val repository: HistoryRepository,
) {

    suspend fun await(): List<ReadActivityEntry> {
        return repository.getReadActivity()
    }
}
