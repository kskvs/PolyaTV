package tv.polya.integration.lampa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LampaSearchSuggestionTest {
    @Test fun movieSuggestionMapsToLampaLaunchReference() {
        val result = LampaSearchSuggestion(
            tmdbId = "550",
            mediaType = "movie",
            title = "Бойцовский клуб",
            description = "1999 · драма",
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
        ).toMediaSearchResult()

        assertEquals("lampa", result?.sourceId)
        assertEquals("550", result?.providerContentId)
        assertEquals("movie", result?.launchType)
        assertEquals("Бойцовский клуб", result?.title)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", result?.posterUrl)
        assertEquals(1999, result?.year)
        assertEquals("", result?.magnetUri)
    }

    @Test fun tvSuggestionIsKeptAsSeries() {
        val result = LampaSearchSuggestion(
            tmdbId = "1399",
            mediaType = "tv",
            title = "Игра престолов",
            description = "2011 · фэнтези",
            posterUrl = null,
        ).toMediaSearchResult()

        assertEquals("tv", result?.launchType)
        assertEquals("Сериал · Lampa", result?.providerLabel)
        assertEquals(2011, result?.year)
    }

    @Test fun invalidIdsAndMediaTypesAreRejected() {
        assertNull(LampaSearchSuggestion("0", "movie", "Film", "", null).toMediaSearchResult())
        assertNull(LampaSearchSuggestion("42", "person", "Actor", "", null).toMediaSearchResult())
        assertNull(LampaSearchSuggestion("42", "tv", " ", "", null).toMediaSearchResult())
    }
}
