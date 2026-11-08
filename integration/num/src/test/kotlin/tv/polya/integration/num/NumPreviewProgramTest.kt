package tv.polya.integration.num

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumPreviewProgramTest {
    @Test fun searchableRecommendationMapsToNumLaunchReference() {
        val result = NumPreviewProgram(
            rowId = 17L,
            contentId = "num-content-17",
            title = "Интерстеллар",
            description = "2014 · фантастика",
            posterUri = "https://images.example/interstellar.jpg",
            intentUri = "intent://num/17#Intent;package=ru.yourok.num;end",
            searchable = true,
        ).toMediaSearchResult()

        assertEquals("num", result?.sourceId)
        assertEquals("num-content-17", result?.providerContentId)
        assertEquals("tv_provider", result?.launchType)
        assertEquals("Интерстеллар", result?.title)
        assertEquals(2014, result?.year)
        assertEquals("https://images.example/interstellar.jpg", result?.posterUrl)
        assertEquals("", result?.magnetUri)
        assertEquals("intent://num/17#Intent;package=ru.yourok.num;end", result?.sourceLaunchUri)
    }

    @Test fun hiddenOrIncompleteProgramsAreRejected() {
        assertNull(
            NumPreviewProgram(1, "id", "Film", "", null, "intent://x", false).toMediaSearchResult(),
        )
        assertNull(
            NumPreviewProgram(1, "id", " ", "", null, "intent://x", true).toMediaSearchResult(),
        )
        assertNull(
            NumPreviewProgram(1, "id", "Film", "", null, "", true).toMediaSearchResult(),
        )
    }

    @Test fun unsupportedPosterSchemesAreIgnored() {
        val result = NumPreviewProgram(
            1, "id", "Film", "", "content://private/poster", "intent://num/1", true,
        ).toMediaSearchResult()
        assertNull(result?.posterUrl)
    }
}
