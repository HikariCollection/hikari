package app.hikari.presentation.track.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.hikari.data.track.Tracker
import app.hikari.test.DummyTracker

internal class TrackLogoIconPreviewProvider : PreviewParameterProvider<Tracker> {

    override val values: Sequence<Tracker>
        get() = sequenceOf(
            DummyTracker(
                id = 1L,
                name = "Dummy Tracker",
            ),
        )
}
