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
 * Provides constant fixed delay seconds for each attempt until {@link #timeout}
 * has been reached. After which it will return -1
 *
 * {@code
 *     final Delay delay =
 *         Constant.of().delay(Duration.ofSeconds(5))
 *             .timeout(Duration.ofSeconds(50)).build();
 *     Duration next = Duration.ZERO;
 *     int attempt = 1;
 *     while ((next = fixed.nextDelay(attempt++)) != Duration.ZERO) { accrued +=
 * next; } Assertions.assertEquals(5*10, accrued); }
 */
public class Constant extends AbstractDelay {

    final Duration delay;

    Constant(Duration timeout,
             Duration delay) {
        super(timeout);
        Preconditions.checkArgument(delay != null && delay.toMillis() > 0, "delay must be > 0");
        this.delay = delay;
    }

    public static Builder of() {
        return new Builder();
    }

    public static final class Builder extends DelayBasedBuilder<Constant, Builder> {
        @Override
        public Constant build() {
            return new Constant(timeout, delay);
        }
    }

    @Override
    public Duration nextDelay(int attempt) {
        return enforceBounds(delay.multipliedBy(attempt), delay);
    }

}
