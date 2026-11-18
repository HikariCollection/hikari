package app.hikari.data.chapter

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChapterSanitizerTest {

    @Test
    fun `sanitize strips manga title prefix and surrounding separators`() {
        with(ChapterSanitizer) {
            "  One Piece - Chapter 100: Gear 5  ".sanitize("One Piece") shouldBe "Chapter 100: Gear 5"
            "One Piece_Ch. 12_".sanitize("One Piece") shouldBe "Ch. 12"
            "One Piece, Vol. 1 - Ch. 1".sanitize("One Piece") shouldBe "Vol. 1 - Ch. 1"
            "One Piece: Prologue".sanitize("One Piece") shouldBe "Prologue"
        }
    }

    @Test
    fun `sanitize strips unicode whitespace and preserves non-prefix title`() {
        with(ChapterSanitizer) {
            "\u3000Chapter 5 - The One Piece\u00A0".sanitize("One Piece") shouldBe "Chapter 5 - The One Piece"
            "Naruto - Chapter 1".sanitize("One Piece") shouldBe "Naruto - Chapter 1"
        }
    }
}
