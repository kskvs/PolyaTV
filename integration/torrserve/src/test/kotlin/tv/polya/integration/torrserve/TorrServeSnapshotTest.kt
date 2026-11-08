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

class TorrServeHistoryMergeTest {
    @Test fun historySurvivesAnEmptyViewedResponse() {
        val previous = listOf(
            TorrServeHistoryEntry("abc", listOf(1), mapOf(1 to 120.0), 500L),
        )
        val merged = mergeTorrServeHistory(previous, emptyList())
        assertEquals(previous, merged)
    }

    @Test fun mergeCombinesEpisodesAndKeepsLatestObservedTime() {
        val previous = listOf(
            TorrServeHistoryEntry("ABC", listOf(1), mapOf(1 to 120.0), 500L),
        )
        val incoming = listOf(
            TorrServeHistoryEntry("abc", listOf(2), mapOf(2 to 45.0), 900L),
        )
        val merged = mergeTorrServeHistory(previous, incoming).single()
        assertEquals(listOf(1, 2), merged.fileIndices)
        assertEquals(120.0, merged.timecodes[1] ?: -1.0, 0.001)
        assertEquals(45.0, merged.timecodes[2] ?: -1.0, 0.001)
        assertEquals(900L, merged.lastObservedAt)
    }

    @Test fun historyIsDeduplicatedByTorrentHashAndBounded() {
        val previous = (1..8).map {
            TorrServeHistoryEntry("h$it", listOf(1), mapOf(1 to 0.0), it * 10L)
        }
        val incoming = listOf(TorrServeHistoryEntry("H1", listOf(2), mapOf(2 to 30.0), 100L))
        val merged = mergeTorrServeHistory(previous, incoming, maxItems = 5)
        assertEquals(5, merged.size)
        assertEquals(1, merged.count { it.hash == "h1" })
        assertEquals(listOf(1, 2), merged.first { it.hash == "h1" }.fileIndices)
    }
}
