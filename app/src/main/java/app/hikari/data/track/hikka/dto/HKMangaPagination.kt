package app.hikari.data.track.hikka.dto

import kotlinx.serialization.Serializable

@Serializable
data class HKMangaPagination(
    val pagination: HKPagination,
    val list: List<HKManga>,
)
