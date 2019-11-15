/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy.delay;

import com.google.common.base.Preconditions;

import java.time.Duration;

/**
 * {@link Exponential}, provides waves starting with minimum delay of
 * {@link Exponential#timeout} is exceeded
 */
public class Exponential extends MinDelayAbstractBase {

    final int powerBy;
    private Duration accrued = Duration.ZERO;

    private Exponential(Duration timeout,
                        Duration minDelay,
                        int powerBy) {
        super(timeout, minDelay);
        Preconditions.checkArgument(powerBy >= 2, "powerBy >= 2");
        this.powerBy = powerBy;
    }

    public static Builder of() {
        return new Builder();
    }

    public static final class Builder extends MinDelayBasedBuilder<Exponential, Builder> {

        private int powerBy = 2;

        public Builder powerBy(int powerBy) {
            this.powerBy = powerBy;
            return this;
        }

        @Override
        public Exponential build() {
            return new Exponential(timeout, minDelay, powerBy);
        }
    }

    @Override
    public Duration nextDelay(int attempt) {
        long next = Math.round(Math.pow(powerBy, attempt));
        Duration nextDelay = Duration.ofSeconds(next);
        accrued = accrued.plus(nextDelay);
        return enforceBounds(accrued, nextDelay);
    }
}
