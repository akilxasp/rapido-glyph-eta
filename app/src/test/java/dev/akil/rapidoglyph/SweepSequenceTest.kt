package dev.akil.rapidoglyph

import org.junit.Assert.assertEquals
import org.junit.Test

class SweepSequenceTest {
    @Test
    fun advancesFromOneThroughNinetyNineAndLoops() {
        var minute = 1
        val sequence = buildList {
            repeat(100) {
                add(minute)
                minute = EtaStore.nextSweepMinute(minute)
            }
        }

        assertEquals((1..99).toList() + 1, sequence)
    }
}
