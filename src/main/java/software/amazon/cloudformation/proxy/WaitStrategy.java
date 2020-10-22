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

import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface WaitStrategy {
    <ModelT, CallbackT>
        ProgressEvent<ModelT, CallbackT>
        await(long operationElapsedTime, Duration nextAttempt, CallbackT context, ModelT model);

    static WaitStrategy newLocalLoopAwaitStrategy(final Supplier<Long> remainingTimeToExecute) {
        return new WaitStrategy() {
            @Override
            public <ModelT, CallbackT>
                ProgressEvent<ModelT, CallbackT>
                await(long operationElapsedTime, Duration next, CallbackT context, ModelT model) {
                long remainingTime = remainingTimeToExecute.get();
                long localWait = next.toMillis() + 2 * operationElapsedTime + 100;
                if (remainingTime > localWait) {
                    Uninterruptibles.sleepUninterruptibly(next.getSeconds(), TimeUnit.SECONDS);
                    return null;
                }
                return ProgressEvent.defaultInProgressHandler(context, (int) next.getSeconds(), model);
            }
        };
    }

    static WaitStrategy scheduleForCallbackStrategy() {
        return new WaitStrategy() {
            @Override
            public <ModelT, CallbackT>
                ProgressEvent<ModelT, CallbackT>
                await(long operationElapsedTime, Duration next, CallbackT context, ModelT model) {
                return ProgressEvent.defaultInProgressHandler(context, (int) next.getSeconds(), model);
            }
        };
    }
}
