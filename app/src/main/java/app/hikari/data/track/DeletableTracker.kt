package app.hikari.data.track

import app.hikari.domain.track.model.Track

/**
 * Tracker that support deleting am entry from a user's list.
 */
interface DeletableTracker {

    suspend fun delete(track: Track)
}
