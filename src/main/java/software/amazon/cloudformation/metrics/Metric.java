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

public class Metric {

    public static final String METRIC_NAMESPACE_ROOT = "AWS/CloudFormation";
    public static final String METRIC_NAME_HANDLER_EXCEPTION = "HandlerException";
    public static final String METRIC_NAME_HANDLER_EXCEPTION_BY_ERROR_CODE = "HandlerExceptionByErrorCode";
    public static final String METRIC_NAME_HANDLER_EXCEPTION_BY_EXCEPTION_COUNT = "HandlerExceptionByExceptionCount";
    public static final String METRIC_NAME_HANDLER_DURATION = "HandlerInvocationDuration";
    public static final String METRIC_NAME_HANDLER_INVOCATION_COUNT = "HandlerInvocationCount";

    public static final String DIMENSION_KEY_ACTION_TYPE = "Action";
    public static final String DIMENSION_KEY_EXCEPTION_TYPE = "ExceptionType";
    public static final String DIMENSION_KEY_RESOURCE_TYPE = "ResourceType";
    public static final String DIMENSION_KEY_HANDLER_ERROR_CODE = "HandlerErrorCode";

    private Metric() {
    }
}
