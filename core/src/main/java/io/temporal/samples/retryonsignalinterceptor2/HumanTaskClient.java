/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.retryonsignalinterceptor2;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import java.util.List;

/** Interface used to dynamically register signal and query handlers from the interceptor. */
public interface HumanTaskClient {

  /** Requests retry of the activities waiting after failure. */
  @SignalMethod
  void changeStatus(HumanTaskService.TaskRequest task);

  /** Requests no more retries of the activities waiting after failure. */

  /** Returns human status of the pending activities. */
  @QueryMethod
  List<HumanTask> getHumanTasks();
}
