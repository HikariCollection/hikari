package app.hikari.feature.upcoming

import kotlinx.datetime.LocalDate
import app.hikari.domain.manga.model.Manga

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Manga) : UpcomingUIModel
}
