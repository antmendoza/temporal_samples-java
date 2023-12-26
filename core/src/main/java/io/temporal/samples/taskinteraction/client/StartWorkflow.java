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

import static io.temporal.samples.taskinteraction.worker.Worker.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.taskinteraction.TaskWorkflow;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class StartWorkflow {

  static final String WORKFLOW_ID = "TaskWorkflow";

  public static void main(String[] args) {

    final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    final WorkflowClient client = WorkflowClient.newInstance(service);

    final TaskWorkflow workflow =
        client.newWorkflowStub(
            TaskWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    System.out.println("Starting workflow " + WORKFLOW_ID);

    // Execute workflow waiting for it to complete.
    workflow.execute();

    System.out.println("Workflow completed");
    System.exit(0);
  }
}
