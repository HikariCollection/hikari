package app.hikari.domain.history.interactor

import app.hikari.domain.history.model.HistoryUpdate
import app.hikari.domain.history.repository.HistoryRepository
import dev.zacsweers.metro.Inject

@Inject
class UpsertHistory(
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(historyUpdate: HistoryUpdate) {
        historyRepository.upsertHistory(historyUpdate)
    }
}
