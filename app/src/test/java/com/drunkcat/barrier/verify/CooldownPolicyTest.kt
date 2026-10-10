package com.drunkcat.barrier.verify

import org.junit.Assert.assertEquals
import org.junit.Test

class CooldownPolicyTest {

    @Test
    fun `cooldown ladder matches spec`() {
        assertEquals(0L, CooldownPolicy.cooldownFor(0))
        assertEquals(30_000L, CooldownPolicy.cooldownFor(1))
        assertEquals(2 * 60_000L, CooldownPolicy.cooldownFor(2))
        assertEquals(10 * 60_000L, CooldownPolicy.cooldownFor(3))
        assertEquals(30 * 60_000L, CooldownPolicy.cooldownFor(4))
    }

    @Test
    fun `fifth failure locks until next day`() {
        assertEquals(CooldownPolicy.LOCK_UNTIL_NEXT_DAY, CooldownPolicy.cooldownFor(5))
        assertEquals(CooldownPolicy.LOCK_UNTIL_NEXT_DAY, CooldownPolicy.cooldownFor(6))
    }
}
