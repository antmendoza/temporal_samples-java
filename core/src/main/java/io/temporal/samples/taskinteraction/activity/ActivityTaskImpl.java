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

package io.temporal.samples.taskinteraction.activity;

import static io.temporal.samples.taskinteraction.worker.Worker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.taskinteraction.Task;
import io.temporal.samples.taskinteraction.WorkflowTaskManager;
import java.util.ArrayList;

public class ActivityTaskImpl implements ActivityTask {

  private WorkflowClient workflowClient;

  public ActivityTaskImpl(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @Override
  public void createTask(Task task) {

    WorkflowStub taskManager =
        workflowClient.newUntypedWorkflowStub(
            WorkflowTaskManager.class.getSimpleName(),
            WorkflowOptions.newBuilder()
                .setWorkflowId(WorkflowTaskManager.WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    taskManager.signalWithStart(
        "createTask", new Object[] {task}, new Object[] {new ArrayList<>(), new ArrayList<>()});
  }
}
