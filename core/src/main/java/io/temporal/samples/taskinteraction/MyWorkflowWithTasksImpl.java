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

import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class MyWorkflowWithTasksImpl implements MyWorkflowWithTasks {

  private final Logger logger = Workflow.getLogger(MyWorkflowWithTasksImpl.class);

  private final TaskService<String> taskService = new TaskService<>();

  @Override
  public void execute() {
    final TaskToken taskToken = new TaskToken();

    // Schedule two "tasks" in parallel. The last parameter is the token the client needs
    // to change the task state, and to complete the task eventually

    // Blocking invocation
    taskService.executeTask(new Task(taskToken.getNext(), new Task.TaskTitle("TODO 1")));
    logger.info("Task completed");
    logger.info("Completing workflow");
  }

  private static class TaskToken {

    private int taskToken = 1;

    public String getNext() {

      return Workflow.getInfo().getWorkflowId()
          + "-"
          + Workflow.currentTimeMillis()
          + "-"
          + taskToken++;
    }
  }
}
