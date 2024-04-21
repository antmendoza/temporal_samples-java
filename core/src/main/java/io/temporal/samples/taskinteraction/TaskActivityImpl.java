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

import static io.temporal.samples.taskinteraction.worker.Worker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.util.ArrayList;

public class TaskActivityImpl implements TaskActivity {

  private WorkflowClient workflowClient;

  public TaskActivityImpl(WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
  }

  @Override
  public void createTask(Task task) {

    WorkflowStub taskManager =
        workflowClient.newUntypedWorkflowStub(
            TaskManagerWorkflow.class.getSimpleName(),
            WorkflowOptions.newBuilder()
                .setWorkflowId(TaskManagerWorkflow.WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    taskManager.signalWithStart(
        "addTask", new Object[] {task}, new Object[] {new ArrayList<>(), new ArrayList<>()});
  }
}
