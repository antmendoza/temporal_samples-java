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

package io.temporal.samples.taskinteraction;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.updatabletimer.UpdatableTimer;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class TaskWorkflowImpl implements TaskWorkflow {

  private final Logger logger = Workflow.getLogger(UpdatableTimer.class);

  private final TaskService<String> taskService = new TaskService<>();

  private final TaskActivity activity =
      Workflow.newActivityStub(
          TaskActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public void execute() {

    // Schedule two "tasks" in parallel. The last parameter is the token the client needs
    // to change the task status, and ultimately to complete the task
    Promise<String> task1 =
        taskService.executeAsync(() -> activity.createTask("TODO 1"), generateToken());
    Promise<String> task2 =
        taskService.executeAsync(() -> activity.createTask("TODO 2"), generateToken());

    // Block execution until both tasks complete
    Promise.allOf(Arrays.asList(task1, task2)).get();

    // Blocking invocation
    taskService.execute(() -> activity.createTask("TODO 3"), generateToken());
  }

  private final AtomicInteger atomicInteger = new AtomicInteger(1);

  private String generateToken() {
    return Workflow.getInfo().getWorkflowId()
        + "-"
        + Workflow.currentTimeMillis()
        + "-"
        + atomicInteger.getAndIncrement();
  }
}
