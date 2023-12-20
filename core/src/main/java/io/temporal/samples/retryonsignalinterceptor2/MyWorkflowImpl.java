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

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MyWorkflowImpl implements MyWorkflow {

  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              // disable server side retries. In most production applications the retries should be
              // done for a while before requiring an external operator signal.
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
              .build());

  @Override
  public void execute() {
    HumanTaskService humanTaskService = new HumanTaskService();

    String result =
        humanTaskService.execute(() -> activity.execute(), humanTaskService.generateToken());
  }
}
