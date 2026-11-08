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


class TorrServeViewedHistoryTest {
    @Test fun viewedStateIsGroupedByTorrentAndFile() {
        val marks = listOf(
            TorrServeViewedMark("ABC", 1, 120.0),
            TorrServeViewedMark("abc", 2, 0.0),
            TorrServeViewedMark("DEF", 1, 20.0),
        )
        val state = mergeTorrServeViewedState(emptyMap(), marks, now = 1000L)
        val history = buildTorrServeHistory(marks, state)

        assertEquals(2, history.size)
        assertEquals("abc", history.first().hash)
        assertEquals(listOf(1, 2), history.first().fileIndices)
        assertEquals(120.0, history.first().timecodes[1] ?: -1.0, 0.001)
    }

    @Test fun observedTimeChangesOnlyWhenTrackedTimecodeChanges() {
        val first = listOf(TorrServeViewedMark("abc", 1, 10.0))
        val firstState = mergeTorrServeViewedState(emptyMap(), first, now = 100L)
        val sameState = mergeTorrServeViewedState(firstState, first, now = 200L)
        val updated = mergeTorrServeViewedState(
            sameState,
            listOf(TorrServeViewedMark("abc", 1, 13.0)),
            now = 300L,
        )

        assertEquals(100L, sameState.getValue("abc:1").lastObservedAt)
        assertEquals(300L, updated.getValue("abc:1").lastObservedAt)
    }

    @Test fun invalidFileIndexesAreIgnored() {
        val history = buildTorrServeHistory(
            listOf(TorrServeViewedMark("abc", 0, 2.0), TorrServeViewedMark("", 1, 2.0)),
            emptyMap(),
        )
        assertTrue(history.isEmpty())
    }
}


class TorrServeSearchResultTest {
    @Test fun duplicateSearchMatchesCollapseByInfoHash() {
        val first = TorrServeSearchResult("Film", "Film.1080p", "Tracker A", 2024, "8 GB", 12, "magnet:first", "ABC")
        val duplicate = first.copy(torrentName = "Film.WEB-DL", tracker = "Tracker B", magnet = "magnet:second", hash = "abc")
        val distinct = first.copy(title = "Another film", magnet = "magnet:third", hash = "DEF")

        val result = deduplicateTorrServeSearchResults(listOf(first, duplicate, distinct))

        assertEquals(2, result.size)
        assertEquals("magnet:first", result.first().magnet)
    }

    @Test fun searchResultsWithoutMagnetAreNotSelectable() {
        val invalid = TorrServeSearchResult("Film", "", "", 0, "", 0, "", "abc")
        val valid = invalid.copy(magnet = "magnet:valid")

        assertEquals(listOf(valid), deduplicateTorrServeSearchResults(listOf(invalid, valid)))
    }
}
