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

package io.temporal.samples.taskinteraction.client;

import static io.temporal.samples.taskinteraction.client.StartWorkflow.WORKFLOW_ID;

import io.temporal.client.WorkflowClient;
import io.temporal.samples.taskinteraction.Task;
import io.temporal.samples.taskinteraction.TaskClient;
import io.temporal.samples.taskinteraction.TaskManagerWorkflow;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;

public class CompleteNextTask {

  public static void main(String[] args) {

    final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    final WorkflowClient client = WorkflowClient.newInstance(service);

    final TaskClient taskClient = client.newWorkflowStub(TaskClient.class, WORKFLOW_ID);

    // WorkflowTaskManager keeps and manage workflow task lifecycle
    final TaskManagerWorkflow taskManagerWorkflow =
        client.newWorkflowStub(TaskManagerWorkflow.class, TaskManagerWorkflow.WORKFLOW_ID);

    final List<Task> pendingTask = taskManagerWorkflow.getPendingTask();

    final Task nextOpenTask = pendingTask.get(0);

    taskManagerWorkflow.completeTaskByTaskToken(nextOpenTask.getToken());

    System.exit(0);
  }
}
