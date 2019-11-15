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

import java.time.Duration;

/**
 * Provides delay seconds which is a multiple of the delay shifted from previous
 * attempt's accrual until {@link #timeout} has been reached. After which it
 * will return {@link Duration#ZERO}
 */
public class ShiftByMultipleOf extends MultipleOf {

    ShiftByMultipleOf(Duration timeout,
                      Duration delay,
                      int multiple) {
        super(timeout, delay, multiple);
    }

    public static class Builder extends MultipleOf.Builder {
        @Override
        public ShiftByMultipleOf build() {
            return new ShiftByMultipleOf(timeout, delay, multiple);
        }
    }

    public static Builder shiftedOf() {
        return new Builder();
    }

    @Override
    public Duration nextDelay(int attempt) {
        Duration next = super.nextDelay(attempt);
        return next == Duration.ZERO ? next : accrued;
    }
}
