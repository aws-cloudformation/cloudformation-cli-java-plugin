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
package software.amazon.cloudformation.metrics;

import java.time.Instant;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public abstract class MetricsPublisher {

    protected String resourceTypeName;
    protected String resourceNamespace;

    public MetricsPublisher(final String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
        this.resourceNamespace = resourceTypeName.replace("::", "/");
    }

    /**
     * On Lambda re-invoke we need to supply a new set of client credentials so this
     * function must be called whenever credentials are refreshed/changed in the
     * owning entity
     */
    public void refreshClient() {
    }

    public void publishExceptionMetric(final Instant timestamp,
                                       final Action action,
                                       final Throwable e,
                                       final HandlerErrorCode handlerErrorCode) {
    }

    public void publishExceptionByErrorCodeAndCountBulkMetrics(final Instant timestamp,
                                                               final Action action,
                                                               final HandlerErrorCode handlerErrorCode) {
    }

    public void publishInvocationMetric(final Instant timestamp, final Action action) {
    }

    public void publishDurationMetric(final Instant timestamp, final Action action, final long milliseconds) {
    }

    public void publishProviderLogDeliveryExceptionMetric(final Instant timestamp, final Throwable exception) {
    }
}
