/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;

import java.time.Instant;

public interface MetricsPublisher {

    /**
     * On Lambda re-invoke we need to supply a new set of client credentials so this
     * function must be called whenever credentials are refreshed/changed in the
     * owning entity
     */
    void refreshClient();

    String getResourceTypeName();

    void setResourceTypeName(String resourceTypeName);

    void publishExceptionMetric(final Instant timestamp, final Action action, final Throwable e);

    void publishInvocationMetric(final Instant timestamp, final Action action);

    void publishDurationMetric(final Instant timestamp, final Action action, long milliseconds);
}
