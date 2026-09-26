package com.inklazy.routeverge.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCacheTest {
    @Test fun recentTimestampIsAccepted() {
        assertTrue(LocationCache.isFresh(1000L, 1000L + LocationCache.MAX_AGE_MILLIS))
    }

    @Test fun missingFutureOrExpiredTimestampIsRejected() {
        val now = LocationCache.MAX_AGE_MILLIS + 1000L
        assertFalse(LocationCache.isFresh(0L, now))
        assertFalse(LocationCache.isFresh(now + 1L, now))
        assertFalse(LocationCache.isFresh(1L, now + 1L))
    }
}
