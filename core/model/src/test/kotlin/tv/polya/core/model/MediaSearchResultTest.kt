package tv.polya.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSearchResultTest {
    @Test fun torrentDuplicatesCollapseByHash() {
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

    @Test fun lampaSuggestionsWithoutMagnetRemainSelectable() {
        val movie = MediaSearchResult(
            title = "Film", providerName = "Lampa", providerLabel = "Фильм · Lampa",
            year = 2024, sizeLabel = "", seedCount = 0, magnetUri = "",
            providerContentId = "550", sourceId = "lampa", launchType = "movie",
        )
        val duplicate = movie.copy(description = "alternate metadata")
        assertEquals(1, deduplicateMediaSearchResults(listOf(movie, duplicate)).size)
    }

    @Test fun providersWithSameIdKeepSeparateLaunchPaths() {
        val torrent = MediaSearchResult("Film", "Tracker", "Torrent", 2024, "", 1, "magnet:1", "550")
        val lampa = MediaSearchResult(
            "Film", "Lampa", "Фильм · Lampa", 2024, "", 0, "", "550",
            sourceId = "lampa", launchType = "movie",
        )
        assertEquals(2, deduplicateMediaSearchResults(listOf(torrent, lampa)).size)
    }

    @Test fun invalidSearchRowsAreDropped() {
        val invalidTorrent = MediaSearchResult("Film", "", "", 0, "", 0, "", "hash")
        val invalidLampa = MediaSearchResult("Film", "Lampa", "", 0, "", 0, "", "", sourceId = "lampa")
        assertEquals(emptyList<MediaSearchResult>(), deduplicateMediaSearchResults(listOf(invalidTorrent, invalidLampa)))
    }
}
