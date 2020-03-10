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
import java.util.ArrayList;
import java.util.List;
import software.amazon.cloudformation.proxy.Delay;

/**
 * Provides blended delay of seconds for each attempt until all delays in the
 * order start to return {@link Duration#ZERO}. This is useful to model blends
 * in the delays where on can be quick for the first set of delays using
 * {@link Constant} and then become {@link MultipleOf} or {@link Exponential}
 * thereafter.
 *
 * {@code
 *
 *     final Delay delay = Blended.of()
 *        .add(
 *            Constant.of().delay(Duration.ofSeconds(5))
 *                .timeout(Duration.ofSeconds(20)).build())
 *        .add(ShiftByMultipleOf.shiftedOf()
 *            .delay(Duration.ofSeconds(5)).timeout(Duration.ofSeconds(220))
 *            .build())
 *        .build();
 * }
 *
 * The above delay provides the following set of 5, 10, 15, 20, 40, 90, 150, 220
 */
public class Blended implements Delay {

    private int index;
    private final List<Delay> inOrder;

    private Blended(List<Delay> delays) {
        index = 0;
        inOrder = delays;
    }

    public static BlendedBuilder of() {
        return new BlendedBuilder();
    }

    public static final class BlendedBuilder implements Builder<Blended> {
        private final List<Delay> inOrder = new ArrayList<>();

        public BlendedBuilder add(Delay delay) {
            inOrder.add(delay);
            return this;
        }

        public Blended build() {
            return new Blended(inOrder);
        }
    }

    @Override
    public Duration nextDelay(int attempt) {
        Duration next = Duration.ZERO;
        while (index < inOrder.size()) {
            next = inOrder.get(index).nextDelay(attempt);
            if (next != Duration.ZERO) {
                break;
            }
            ++index;
        }
        return next;
    }
}
