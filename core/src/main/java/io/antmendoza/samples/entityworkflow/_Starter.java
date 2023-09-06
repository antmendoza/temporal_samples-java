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

package io.antmendoza.samples.entityworkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;

public class _Starter {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityWorkflow";

  public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    // Create the workflow client stub. It is used to start our workflow execution.
    EntityWorkflow workflow =
        client.newWorkflowStub(
            EntityWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setWorkflowRunTimeout(Duration.ofSeconds(20))
                .setTaskQueue(TASK_QUEUE)
                .build());

    WorkflowClient.start(workflow::execute, new EntityInput());

    int value = 0;
    WorkflowStub workflowStub = client.newUntypedWorkflowStub(WORKFLOW_ID);
    for (int a = 0; a < 50; a++) {
      workflow.otherSignal("" + ++value);
      workflow.otherSignal("" + ++value);
      workflow.otherSignal("" + ++value);
    }

    workflow.updateEntityValue(new Value("updateEntityValue"));

    // workflow.exit();

    // Wait for the execution to finish
    Value result = workflowStub.getResult(Value.class);

    System.out.println("result :  " + result.getId());

    // Display workflow execution results
    System.exit(0);
  }
}
