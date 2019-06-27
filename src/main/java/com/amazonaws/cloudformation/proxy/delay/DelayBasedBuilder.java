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

import java.time.Duration;

/**
 * Builder pattern for {@link com.amazonaws.cloudformation.proxy.Delay}s that
 * are based on some constant multiple or shift of delay
 * 
 * @param <R> the final delay object that needs to be built.
 * @param <T> the derived builder object.
 */
abstract class DelayBasedBuilder<R extends Delay, T extends DelayBasedBuilder<R, T>> extends BaseBuilder<R, T> {
    protected Duration delay;

    @SuppressWarnings("unchecked")
    public T delay(Duration delay) {
        this.delay = delay;
        return (T) this;
    }
}
