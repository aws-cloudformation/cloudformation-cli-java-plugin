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
package software.amazon.cloudformation.proxy;

import java.time.Duration;

/**
 * This interface defines the {@link Delay} that you needed between invocations
 * of a specific call chain. Provides a simple interface to define different
 * types of delay implementations like
 * {@link software.amazon.cloudformation.proxy.delay.Constant},
 * {@link software.amazon.cloudformation.proxy.delay.Exponential}.
 *
 * {@link software.amazon.cloudformation.proxy.delay.Constant}, provides the
 * {@link software.amazon.cloudformation.proxy.delay.Constant#nextDelay(int)}
 * that waves constantly for the next attempt. When it exceeds
 * {@link software.amazon.cloudformation.proxy.delay.Constant}#timeout it return
 * {@link Duration#ZERO} to indicate end of delay.
 *
 * {@link software.amazon.cloudformation.proxy.delay.Exponential}, provide
 * exponential values between
 * [{@link software.amazon.cloudformation.proxy.delay.Exponential}#minDelay,
 * {@link software.amazon.cloudformation.proxy.delay.Exponential}#timeout
 *
 * @see software.amazon.cloudformation.proxy.delay.Constant
 * @see software.amazon.cloudformation.proxy.delay.Exponential
 * @see software.amazon.cloudformation.proxy.delay.MultipleOf
 * @see software.amazon.cloudformation.proxy.delay.ShiftByMultipleOf
 */
public interface Delay {
    /**
     * Returns the new delay amount to stabilize as defined by
     * {@link java.time.Duration} time. This returns -1 to indicate that we are done
     * with delays from this instance Different implementations can return different
     * values
     *
     * @param attempt, starts with 1
     * @return the next amount to wait for or {@link Duration#ZERO} to indicate
     *         delay is complete
     */
    Duration nextDelay(int attempt);

}
