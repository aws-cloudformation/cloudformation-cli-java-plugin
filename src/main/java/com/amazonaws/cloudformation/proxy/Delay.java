package com.amazonaws.cloudformation.proxy;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This interface defines the {@link Delay} that you needed between invocations
 * of a specific call chain. Provides a simple interface to define different types of delay
 * implementations like {@link Constant}, {@link Exponential}.
 *
 * {@link Constant}, provides the {@link Constant#nextDelay(int)} that waves constantly
 * for the next attempt. When it exceeds {@link Constant#maxDelay} it return -1 to
 * indicate end of delay.
 *
 * {@link Exponential}, provide exponential values between
 * [{@link Exponential#startRange}, {@link Exponential#endRange}]
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
     * Provides constant fixed delay seconds for each attempt until {@link #maxDelay}
     * has been reached. After which it will return -1
     *
     * {@code
     *     final Delay delay = new Delay.Constant(10, 5*10, TimeUnit.SECONDS);
     *     long next = 0L, accrued = 0L;
     *     int attempt = 1;
     *     while ((next = fixed.nextDelay(attempt++)) > 0) {
     *         accrued += next;
     *     }
     *     Assertions.assertEquals(5*10, accrued);
     * }
     */
    class Constant implements Delay {

        final long maxDelay;
        final long delay;
        final TimeUnit unit;
        public Constant(long delay,
                        long maxDelay,
                        TimeUnit unit) {
            this.maxDelay = maxDelay;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public long nextDelay(int attempt) {
            return delay * attempt <= maxDelay ? delay : -1L;
        }

        @Override
        public TimeUnit unit() {
            return unit;
        }
    }

    /**
     * Provides blended delay of seconds for each attempt until all
     * delays in the order start to return -1. This is useful to model
     * blends in the delays where on can be quick for the first set of
     * delays using {@link Constant} and then become {@link MultipleOf}
     * or {@link Exponential} there after.
     *
     * {@code
     *
     *     final Delay delay =
     *         new Blended(
     *           new Constant(5, 20, TimeUnit.Seconds),
     *           new MultipleOf(10, 220, 2, TimeUnit.Seconds));
     * }
     *
     * The above delay provides the following set of 5, 10, 15, 20, 40, 90, 150, 220
     */
    class Blended implements Delay {

        private final List<Delay> inOrder;
        private int index = 0;
        public Blended(Delay... delays) {
            inOrder = Arrays.asList(delays);
        }

        @Override
        public long nextDelay(int attempt) {
            long next = -1L;
            while (index < inOrder.size()) {
                next = inOrder.get(index).nextDelay(attempt);
                if (next > 0) {
                    break;
                }
                ++index;
            }
            return next;
        }

        @Override
        public TimeUnit unit() {
            return inOrder.get(index).unit();
        }
    }

    /**
     * Provides constant fixed delay seconds which is a multiple of the delay for each
     * attempt until {@link #maxDelay} has been reached. After which it will return -1
     * Fixed is the same as multiple = 1;
     *
     */
    class MultipleOf extends Constant {
        private final int multiple;
        private long previous;
        public MultipleOf(long delay,
                          long maxDelay,
                          int multiple,
                          TimeUnit unit) {
            super(delay, maxDelay, unit);
            Preconditions.checkArgument(multiple > 1, "multiple must be > 1");
            this.multiple = multiple;
        }

        @Override
        public long nextDelay(int attempt) {
            if (attempt < 2) return (previous = delay);
            previous = previous + delay * (attempt - 1) * multiple;
            return previous <= maxDelay ? previous : -1L;
        }
    }

     /**
      * {@link Exponential}, provides waves starting with minimum delay of {@link Exponential#startRange}
      * until {@link Exponential#endRange} is exceeded
     */
    class Exponential implements Delay {
        private final long startRange;
        private final long endRange;
        private final TimeUnit unit;
        private final int powerBy;
        private final boolean isPowerOf2;
        private long accrued = 0L;

        public Exponential(long startRange,
                           long endRange,
                           TimeUnit unit) {
            this(startRange, endRange, unit, 2);
        }

        public Exponential(long startRange,
                           long endRange,
                           TimeUnit unit,
                           int powerBy) {

            Preconditions.checkArgument(startRange > 0, "startRange > 0");
            Preconditions.checkArgument(endRange > 0, "endRange > 0");
            Preconditions.checkArgument(startRange < endRange, "startRange < endRange");
            Preconditions.checkArgument(powerBy >= 2, "powerBy >= 2");
            this.startRange = startRange;
            this.endRange = endRange;
            this.unit = unit;
            this.powerBy = powerBy;
            this.isPowerOf2 = Integer.bitCount(powerBy) == 1;
        }

        @Override
        public long nextDelay(int attempt) {
            long nextDelay = startRange;
            if (isPowerOf2) {
                nextDelay = 1L << attempt;
            }
            else {
                double next = Math.pow(powerBy, attempt);
                nextDelay = Math.round(next);
            }
            if (nextDelay < startRange) {
                nextDelay = startRange;
            }
            accrued += nextDelay;
            return accrued <= endRange ? nextDelay : -1L;
        }

        @Override
        public TimeUnit unit() {
            return unit;
        }
    }
}
