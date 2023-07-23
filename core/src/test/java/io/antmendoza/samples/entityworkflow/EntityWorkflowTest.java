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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

public class EntityWorkflowTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(EntityWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testImpl() {

    ActivityEntityWorkflow activityEntityWorkflow = mock(ActivityEntityWorkflow.class);

    testWorkflowRule.getWorker().registerActivitiesImplementations(activityEntityWorkflow);
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    String workflowId = "test_execute_workflow";
    final WorkflowOptions.Builder builder =
        WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(testWorkflowRule.getTaskQueue());

    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final EntityWorkflow workflow =
        workflowClient.newWorkflowStub(EntityWorkflow.class, builder.build());

    WorkflowClient.start(workflow::execute, new EntityInput(new Value("my-id")));

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        getListOpenExecutions().get(0).getStatus());

    workflow.doX("doX");

    sleep(200);

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        getListOpenExecutions().get(0).getStatus());

    verify(activityEntityWorkflow, times(1)).doX();

    workflow.doY("doY");
    sleep(200);

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        getListOpenExecutions().get(0).getStatus());

    verify(activityEntityWorkflow, times(1)).doY();

    workflow.exit();

    sleep(200);

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        getListClosedExecutions().get(0).getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  private static void sleep(int sleepMillisecond) {
    try {
      Thread.sleep(sleepMillisecond);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  private List<WorkflowExecutionInfo> getListClosedExecutions() {
    return testWorkflowRule
        .getTestEnvironment()
        .getWorkflowServiceStubs()
        .blockingStub()
        .listClosedWorkflowExecutions(ListClosedWorkflowExecutionsRequest.newBuilder().build())
        .getExecutionsList();
  }

  @NotNull
  private List<WorkflowExecutionInfo> getListOpenExecutions() {
    return testWorkflowRule
        .getTestEnvironment()
        .getWorkflowServiceStubs()
        .blockingStub()
        .listOpenWorkflowExecutions(ListOpenWorkflowExecutionsRequest.newBuilder().build())
        .getExecutionsList();
  }
}
