package tv.polya.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSearchResultTest {
    @Test fun duplicateMatchesCollapseByProviderContentId() {
        val first = MediaSearchResult("Film", "Film.1080p", "Tracker A", 2024, "8 GB", 12, "magnet:first", "ABC")
        val duplicate = first.copy(
            providerName = "Film.WEB-DL",
            providerLabel = "Tracker B",
            magnetUri = "magnet:second",
            providerContentId = "abc",
        )
        val distinct = first.copy(title = "Another film", magnetUri = "magnet:third", providerContentId = "DEF")

        val result = deduplicateMediaSearchResults(listOf(first, duplicate, distinct))

        assertEquals(2, result.size)
        assertEquals("magnet:first", result.first().magnetUri)
    }

    @Test fun resultsWithoutMagnetAreNotSelectable() {
        val invalid = MediaSearchResult("Film", "", "", 0, "", 0, "", "abc")
        val valid = invalid.copy(magnetUri = "magnet:valid")

        assertEquals(listOf(valid), deduplicateMediaSearchResults(listOf(invalid, valid)))
    }
}
