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

public class Metrics {

    public final static String METRIC_NAMESPACE_ROOT = "AWS_TMP/CloudFormation";
    public final static String METRIC_NAME_HANDLER_EXCEPTION = "HandlerException";
    public final static String METRIC_NAME_HANDLER_DURATION = "HandlerInvocationDuration";
    public final static String METRIC_NAME_HANDLER_INVOCATION_COUNT = "HandlerInvocationCount";

    public final static String DIMENSION_KEY_ACTION_TYPE = "Action";
    public final static String DIMENSION_KEY_EXCEPTION_TYPE = "ExceptionType";
    public final static String DIMENSION_KEY_RESOURCE_TYPE = "ResourceType";
}
