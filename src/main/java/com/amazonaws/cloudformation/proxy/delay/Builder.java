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

/**
 * Build the final {@link com.amazonaws.cloudformation.proxy.Delay} based
 * timeout and other specifications.
 * 
 * @param <R>, the delay object that is finally built. Can never be null
 */
public interface Builder<R extends Delay> {
    R build();
}
