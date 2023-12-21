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
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class TaskWorkflowImpl implements TaskWorkflow {

  private final Logger logger = Workflow.getLogger(UpdatableTimer.class);

  private final TaskService taskService = new TaskService();

  private final TaskActivity activity =
      Workflow.newActivityStub(
          TaskActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public void execute() {

    final List<Promise<String>> tasks = new ArrayList<>();

    // Schedule two "tasks" in parallel. The last parameter is the token the client needs
    // to change the task status
    tasks.add(
        Async.function(
            () -> taskService.execute(() -> activity.createTask("TODO 1"), generateToken())));

    tasks.add(
        Async.function(
            () -> taskService.execute(() -> activity.createTask("TODO 2"), generateToken())));

    // Wait for async "tasks" to be in "COMPLETED" state
    Promise.allOf(tasks).get();

    tasks.forEach((t) -> logger.info("Task result: " + t.get()));

    // Block workflow code until the "task" is in "COMPLETED" state
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
