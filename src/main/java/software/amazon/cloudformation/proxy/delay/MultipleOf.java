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
 * Provides constant fixed delay seconds which is a multiple of the delay for
 * each attempt until {@link MultipleOf#timeout} has been reached. After which
 * it will return {@link Duration#ZERO}
 */
public class MultipleOf extends Constant {

    Duration accrued = Duration.ZERO;
    final int multiple;

    MultipleOf(Duration timeout,
               Duration delay,
               int multiple) {
        super(timeout, delay);
        Preconditions.checkArgument(multiple > 1, "multiple must be > 1");
        this.multiple = multiple;
    }

    public static class Builder extends DelayBasedBuilder<MultipleOf, Builder> {
        int multiple = 2;

        public Builder multiple(int multiple) {
            this.multiple = multiple;
            return this;
        }

        @Override
        public software.amazon.cloudformation.proxy.delay.MultipleOf build() {
            return new software.amazon.cloudformation.proxy.delay.MultipleOf(timeout, delay, multiple);
        }
    }

    public static Builder multipleOf() {
        return new Builder();
    }

    @Override
    public Duration nextDelay(int attempt) {
        if (attempt < 2) {
            accrued = delay;
            return delay;
        }
        Duration next = delay.multipliedBy((long) (attempt - 1) * multiple);
        accrued = accrued.plus(next);
        return enforceBounds(accrued, next);
    }
}
