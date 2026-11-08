package tv.polya.integration.torrserve

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosterUrlTest {
    @Test fun relativePathUsesConfiguredServer() {
        assertEquals(
            "http://192.168.1.10:8090/poster/abc.jpg",
            resolvePosterUrl("http://192.168.1.10:8090/", "/poster/abc.jpg"),
        )
    }

    @Test fun absoluteHttpsUrlIsPreserved() {
        assertEquals(
            "https://cdn.example/poster.jpg",
            resolvePosterUrl("http://server:8090", "https://cdn.example/poster.jpg"),
        )
    }

    @Test fun blankReferenceReturnsNull() {
        assertNull(resolvePosterUrl("http://server:8090", " "))
        assertNull(resolvePosterUrl("", "poster.jpg"))
    }
}