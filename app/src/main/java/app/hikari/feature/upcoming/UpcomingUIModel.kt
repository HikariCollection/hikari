package app.hikari.feature.upcoming

import app.hikari.domain.manga.model.Manga
import kotlinx.datetime.LocalDate

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Manga) : UpcomingUIModel
}
