package tv.polya.integration.torrserve

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrServeSnapshotTest {
    @Test fun transientlyMissingCardsAreKeptForTwoPolls() {
        val original = listOf(TorrServeEntry("abc", "Film", timestamp = 10L))
        val first = mergeTorrServeEntries(original, emptyList())
        assertEquals(1, first.size)
        assertEquals(1, first.single().missingPolls)
        val second = mergeTorrServeEntries(first, emptyList())
        assertEquals(1, second.size)
        assertEquals(2, second.single().missingPolls)
        assertTrue(mergeTorrServeEntries(second, emptyList()).isEmpty())
    }

    @Test fun incomingEntriesAreDeduplicatedAndResetMissingPolls() {
        val result = mergeTorrServeEntries(
            listOf(TorrServeEntry("abc", "Old", missingPolls = 2)),
            listOf(TorrServeEntry("ABC", "New"), TorrServeEntry("abc", "Duplicate")),
        )
        assertEquals(1, result.size)
        assertEquals("New", result.single().title)
        assertEquals(0, result.single().missingPolls)
    }

    @Test fun snapshotIsLimited() {
        val entries = (1..12).map { TorrServeEntry("h$it", "Film $it", timestamp = it.toLong()) }
        assertEquals(5, mergeTorrServeEntries(emptyList(), entries, maxItems = 5).size)
    }
}
