package com.amazonaws.cloudformation.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DelayTest {

    @Test
    public void fixedDelay() {
        final Delay fixed = new Delay.Fixed(5, 50, TimeUnit.MILLISECONDS);
        int[] attempt = {1};
        final Executable fn = () -> {
            long next = 0L;
            while ((next = fixed.nextDelay(attempt[0])) > 0) {
                attempt[0]++;
                fixed.unit().sleep(next);
            }
        };
        //
        // Small 5ms jitter to ensure we complete within time
        //
        Assertions.assertTimeout(Duration.ofMillis(6 * 50), fn);
        Assertions.assertEquals(6, attempt[0]);
        Assertions.assertTrue(fixed.nextDelay(8) < 0);
        Assertions.assertTrue(fixed.nextDelay(10) < 0);
        Assertions.assertTrue(fixed.nextDelay(15) < 0);
    }

    @Test
    public void fixedDelayIter() {
        final Delay fixed = new Delay.Fixed(5, 50, TimeUnit.MILLISECONDS);
        try {
            int attempt = 1;
            long next = 0L, accured = 0L, jitter = 2L;
            Duration later = Duration.ZERO;
            while ((next = fixed.nextDelay(attempt)) > 0) {
                attempt++;
                accured += next;
                fixed.unit().sleep(next);
                later = later.plusMillis(50);
                long total = later.toMillis();
                boolean range = accured >= (total - jitter) && accured <= (total + jitter);
                Assertions.assertTrue(range);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void fixedDelays() {
        final Delay fixed = new Delay.Fixed(5, 10, TimeUnit.MILLISECONDS);
        long next = 0L, accured = 0L;
        int attempt = 1;
        while ((next = fixed.nextDelay(attempt++)) > 0) {
            Assertions.assertEquals(10, next);
            accured += next;
        }
        Assertions.assertEquals(5*10, accured);
    }

    @Test
    public void exponentialDelays() {
        final Delay exponential = new Delay.Exponential(2, Math.round(Math.pow(2, 9)), TimeUnit.SECONDS);
        int attempt = 1;
        long next = 0L, accured = 0L;
        while ((next = exponential.nextDelay(attempt)) > 0) {
            Assertions.assertEquals(Math.round(Math.pow(2, attempt)), next);
            attempt++;
            accured += next;
        }
        Assertions.assertEquals(10, attempt);

        final Delay delay = new Delay.Exponential(10, Math.round(Math.pow(10, 9)), TimeUnit.SECONDS, 10);
        attempt = 1;
        while ((next = delay.nextDelay(attempt)) > 0) {
            Assertions.assertEquals(Math.round(Math.pow(10, attempt)), next);
            attempt++;
            accured += next;
        }
        Assertions.assertEquals(10, attempt);

    }
}
