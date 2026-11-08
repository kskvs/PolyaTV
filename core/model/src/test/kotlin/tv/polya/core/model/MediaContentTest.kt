package tv.polya.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaContentTest {
    @Test fun sourceIdentitySurvivesNormalization() {
        val item = MediaContent("movie:1", "Example", "torrserve", "abc123", contentType = ContentType.MOVIE)
        assertEquals("torrserve", item.sourceId)
        assertEquals("abc123", item.sourceContentId)
        assertEquals(ContentType.MOVIE, item.contentType)
    }
}
