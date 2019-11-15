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
 * Base builder implementation to capture the maximum bound timout specified by
 * {@link Duration}
 */
abstract class BaseBuilder<R extends Delay, T extends BaseBuilder<R, T>> implements Builder<R> {
    protected Duration timeout;

    BaseBuilder() {
    }

    @SuppressWarnings("unchecked")
    public T timeout(Duration timeout) {
        this.timeout = timeout;
        return (T) this;
    }

}
