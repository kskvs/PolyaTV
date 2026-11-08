package tv.polya.integration.torrserve

import org.junit.Assert.assertEquals
import org.junit.Test

class TorrServeSettingsTest {
    @Test fun blankSavedAddressUsesLocalTvServer() {
        assertEquals(DEFAULT_TORRSERVE_SERVER_URL, effectiveTorrServeServerUrl(null))
        assertEquals(DEFAULT_TORRSERVE_SERVER_URL, effectiveTorrServeServerUrl("  "))
    }

    @Test fun savedServerIsTrimmedAndPreserved() {
        assertEquals("http://192.168.1.20:8090", effectiveTorrServeServerUrl(" http://192.168.1.20:8090/ "))
    }
}
