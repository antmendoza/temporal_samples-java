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
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

public class TaskService<R> {

  private final TaskActivity activity =
      Workflow.newActivityStub(
          TaskActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  private final Map<String, CompletablePromise<R>> pendingPromises =
      Collections.synchronizedMap(new HashMap<>());

  // Exposes signal and query methods that
  // allow us to interact with the workflow execution
  private final TaskClient listener =
      new TaskClient() {

        @Override
        public void completeByTaskToken(String taskToken) {

          logger.info("Completing task with token: " + taskToken);

          final CompletablePromise<R> completablePromise = pendingPromises.get(taskToken);
          completablePromise.complete(null);
        }
      };

  public TaskService() {
    Workflow.registerListener(listener);
  }

  private final Logger logger = Workflow.getLogger(TaskService.class);

  public R executeTask(Task task) {

    logger.info("Before creating task : " + task);
    final String token = task.getToken();
    activity.createTask(task);

    logger.info("Task created: " + task);

    final CompletablePromise<R> promise = Workflow.newPromise();
    pendingPromises.put(token, promise);

    return promise.get();
  }

  public interface Callback<T> {
    T execute();
  }
}
