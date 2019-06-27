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

import com.google.common.base.Preconditions;

import java.time.Duration;

/**
 * Base class for all [min, timeout) range based
 * {@link com.amazonaws.cloudformation.proxy.Delay}s
 * 
 * @see Exponential
 */
abstract class MinDelayAbstractBase extends AbstractDelay {
    final Duration minDelay;

    MinDelayAbstractBase(Duration timeout,
                         Duration minDelay) {
        super(timeout);
        Preconditions.checkArgument(minDelay != null && minDelay.toMillis() >= 0, "minDelay must be > 0");
        Preconditions.checkArgument(minDelay.compareTo(timeout) < 0, "minDelay < timeout");
        this.minDelay = minDelay;
    }

    @Override
    protected Duration enforceBounds(Duration boundsCheck, Duration delayTime) {
        if (boundsCheck.compareTo(minDelay) < 0) {
            return minDelay;
        }
        return super.enforceBounds(boundsCheck, delayTime);
    }
}
