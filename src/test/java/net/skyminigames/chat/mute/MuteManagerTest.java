package net.skyminigames.chat.mute;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteManagerTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    /** No plugin: nothing is written to disk. */
    private final MuteManager mutes = new MuteManager(null, now::get);
    private final UUID player = UUID.randomUUID();

    @Test
    void timedMuteExpires() {
        mutes.mute(player, 10);
        assertTrue(mutes.isMuted(player));
        assertEquals(10, mutes.minutesLeft(player));

        now.addAndGet(TimeUnit.MINUTES.toMillis(9) + 1);
        assertEquals(1, mutes.minutesLeft(player));

        now.addAndGet(TimeUnit.MINUTES.toMillis(1));
        assertFalse(mutes.isMuted(player));
        assertEquals(0, mutes.minutesLeft(player));
    }

    @Test
    void zeroMinutesIsPermanent() {
        mutes.mute(player, 0);
        now.addAndGet(TimeUnit.DAYS.toMillis(365));
        assertTrue(mutes.isMuted(player));
        assertEquals(-1, mutes.minutesLeft(player));
        assertTrue(mutes.unmute(player));
        assertFalse(mutes.unmute(player));
    }

    @Test
    void muteAllSwitch() {
        assertFalse(mutes.isAllMuted());
        mutes.setAllMuted(true);
        assertTrue(mutes.isAllMuted());
    }
}
