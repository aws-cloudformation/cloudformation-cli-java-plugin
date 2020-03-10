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
import software.amazon.cloudformation.proxy.Delay;

/**
 * All delays that have a min delay specified with a maximum time, effectively
 * range based
 *
 * @param <R> the delay type that is being created.
 * @param <T> the derived builder type
 */
abstract class MinDelayBasedBuilder<R extends Delay, T extends MinDelayBasedBuilder<R, T>> extends BaseBuilder<R, T> {
    protected Duration minDelay = Duration.ZERO;

    @SuppressWarnings("unchecked")
    public T minDelay(Duration minDelay) {
        this.minDelay = minDelay;
        return (T) this;
    }

}
