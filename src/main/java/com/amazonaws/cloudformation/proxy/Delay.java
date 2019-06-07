package com.amazonaws.cloudformation.proxy;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

/**
 * This interface defines the {@link Delay} that you needed between invocations
 * of the a lambda. Provides a simple interface to define different types of delay
 * implementations like {@link Fixed}, {@link Exponential}.
 *
 * {@link Fixed}, provides the {@link Fixed#nextDelay(int)} that waves constantly
 * for the next attempt. When it exceeds {@link Fixed#maxAttempts} it return -1 to
 * indicate end of delay.
 *
 * {@link Exponential}, provide exponential values between
 * [{@link Exponential#minDelay}, {@link Exponential#maxDelay}]
 */
public interface Delay {
    /**
     * Returns the new delay amount to stabilize as defined by {@link #unit()} time.
     * This returns -1 to indicate that we are done with delays from this instance
     * Different implementations can return different values
     * @param attempt, starts with 1
     * @return the next amount to stabilize for. return -1 to indicate delay is complete
     */
    long nextDelay(int attempt);

    /**
     * @return define the unit of time like {@link TimeUnit#SECONDS} or {@link TimeUnit#HOURS}
     *         etc. to define the per unit stabilize time between delays.
     */
    TimeUnit unit();

    /**
     * Provides constant fixed delay seconds for each attempt until {@link #maxAttempts}
     * has been reached. After which it will return -1
     */
    class Fixed implements Delay {

        private final int maxAttempts;
        private final int delay;
        private final TimeUnit unit;
        public Fixed(int maxAttempts,
                     int delay,
                     TimeUnit unit) {
            this.maxAttempts = maxAttempts;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public long nextDelay(int attempt) {
            if (attempt < maxAttempts) {
                return delay;
            }
            return -1L;
        }

        @Override
        public TimeUnit unit() {
            return unit;
        }
    }

     /**
      * {@link Exponential}, provides waves starting with minimum delay of {@link Exponential#minDelay}
      * until {@link Exponential#maxDelay} is exceeded
     */
    class Exponential implements Delay {
        private final long minDelay;
        private final long maxDelay;
        private final TimeUnit unit;
        private final int powerBy;
        private final boolean isPowerOf2;

        public Exponential(long start,
                           long maxDelay,
                           TimeUnit unit) {
            this(start, maxDelay, unit, 2);
        }

        public Exponential(long start,
                           long maxDelay,
                           TimeUnit unit,
                           int powerBy) {

            Preconditions.checkArgument(start > 0, "minDelay > 0");
            Preconditions.checkArgument(maxDelay > 0, "maxDelay > 0");
            Preconditions.checkArgument(start < maxDelay, "minDelay < maxDelay");
            Preconditions.checkArgument(powerBy >= 2, "powerBy >= 2");
            this.minDelay = start;
            this.maxDelay = maxDelay;
            this.unit = unit;
            this.powerBy = powerBy;
            this.isPowerOf2 = Integer.bitCount(powerBy) == 1;
        }

        @Override
        public long nextDelay(int attempt) {
            long nextValue = minDelay;
            if (isPowerOf2) {
                nextValue = 1L << (attempt - 1);
            }
            else {
                double next = Math.pow(powerBy, attempt - 1);
                nextValue = Math.round(next);
            }
            if (nextValue < minDelay) {
                nextValue = minDelay;
            }
            return nextValue <= maxDelay ? nextValue : -1L;
        }

        @Override
        public TimeUnit unit() {
            return unit;
        }
    }
}
