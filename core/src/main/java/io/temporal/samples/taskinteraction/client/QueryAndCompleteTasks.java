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

import io.temporal.client.WorkflowClient;
import io.temporal.samples.taskinteraction.Task;
import io.temporal.samples.taskinteraction.WorkflowTaskManager;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.List;

public class QueryAndCompleteTasks {

  public static void main(String[] args) {

    final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    final WorkflowClient client = WorkflowClient.newInstance(service);

    // WorkflowTaskManager keeps and manage workflow task lifecycle
    final WorkflowTaskManager workflowTaskManager =
        client.newWorkflowStub(WorkflowTaskManager.class, WorkflowTaskManager.WORKFLOW_ID);

    //    while (true) {
    final List<Task> pendingTask = workflowTaskManager.getPendingTask();

    if (pendingTask.isEmpty()) {
      //  break;
    }
    final Task nextOpenTask = pendingTask.get(0);
    workflowTaskManager.completeTaskByToken(nextOpenTask.getToken());
    // }

    System.exit(0);
  }
}
