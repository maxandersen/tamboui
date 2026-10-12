/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenderClockTest {

    @Test
    @DisplayName("the first tick returns zero and later ticks return the time since the previous tick")
    void firstTickIsZeroThenMeasuresBetweenTicks() {
        FakeClock clock = new FakeClock(5_000_000_000L);
        RenderClock renderClock = new RenderClock(clock);

        assertThat(renderClock.tick()).isEqualTo(Duration.ZERO);

        clock.advance(Duration.ofMillis(16));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(16));

        clock.advance(Duration.ofMillis(40));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(40));
    }

    @Test
    @DisplayName("several ticks without time passing advance an animation by nothing")
    void ticksWithoutTimeDoNotAdvance() {
        FakeClock clock = new FakeClock(0L);
        RenderClock renderClock = new RenderClock(clock);
        renderClock.tick();

        // Redraws forced by input between two ticks, with no real time passing
        assertThat(renderClock.tick()).isEqualTo(Duration.ZERO);
        assertThat(renderClock.tick()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("the time below a millisecond is carried over, so there is no drift")
    void subMillisecondTimeIsCarried() {
        FakeClock clock = new FakeClock(0L);
        RenderClock renderClock = new RenderClock(clock);
        renderClock.tick();

        // 16.7 ms frames: 16 then 17, so two frames cover the full 33.4 ms
        clock.advance(Duration.ofNanos(16_700_000));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(16));
        clock.advance(Duration.ofNanos(16_700_000));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(17));

        // Over 100 such 16.7 ms frames the carried time adds up to the exact 1670 ms, no drift
        long total = 33;
        for (int i = 0; i < 98; i++) {
            clock.advance(Duration.ofNanos(16_700_000));
            total += renderClock.tick().toMillis();
        }
        assertThat(total).isEqualTo(1670L);
    }

    @Test
    @DisplayName("a gap longer than the limit is clamped, so an animation does not jump far ahead")
    void longGapIsClamped() {
        FakeClock clock = new FakeClock(0L);
        RenderClock renderClock = new RenderClock(clock);
        renderClock.tick();

        clock.advance(Duration.ofHours(1));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(RenderClock.MAX_ELAPSED_MILLIS));
    }

    @Test
    @DisplayName("reset starts a fresh animation, so the next tick is zero")
    void resetStartsFresh() {
        FakeClock clock = new FakeClock(0L);
        RenderClock renderClock = new RenderClock(clock);
        renderClock.tick();
        clock.advance(Duration.ofMillis(16));
        renderClock.tick();

        renderClock.reset();
        clock.advance(Duration.ofMillis(500));
        assertThat(renderClock.tick()).as("first tick after reset").isEqualTo(Duration.ZERO);
        clock.advance(Duration.ofMillis(16));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(16));
    }

    @Test
    @DisplayName("the elapsed time is never negative when the clock value overflows")
    void overflowDoesNotProduceNegativeElapsed() {
        FakeClock clock = new FakeClock(Long.MAX_VALUE - Duration.ofMillis(10).toNanos());
        RenderClock renderClock = new RenderClock(clock);
        renderClock.tick();

        clock.advance(Duration.ofMillis(16));
        assertThat(renderClock.tick()).isEqualTo(Duration.ofMillis(16));
    }

    /**
     * A clock that only moves when the test advances it.
     */
    private static final class FakeClock implements LongSupplier {
        private long nanos;

        FakeClock(long nanos) {
            this.nanos = nanos;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long getAsLong() {
            return nanos;
        }
    }
}
