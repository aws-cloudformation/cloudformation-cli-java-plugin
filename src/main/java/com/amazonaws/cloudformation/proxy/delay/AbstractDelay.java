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
package com.amazonaws.cloudformation.proxy.delay;

import com.amazonaws.cloudformation.proxy.Delay;
import com.google.common.base.Preconditions;

import java.time.Duration;

/**
 * Base delay class that hosts the maximum timeout for {@link Delay} after which
 * we return the duration to be {@link Duration#ZERO} to indicate timeout has
 * been reached.
 */
abstract class AbstractDelay implements Delay {
    final Duration timeout;

    AbstractDelay(Duration timeout) {
        Preconditions.checkArgument(timeout != null && timeout.toMillis() > 0, "timeout must be > 0");
        this.timeout = timeout;
    }

    protected Duration enforceBounds(Duration boundsCheck, Duration delayTime) {
        if (boundsCheck.compareTo(timeout) > 0) {
            return Duration.ZERO;
        }
        return delayTime;
    }
}
