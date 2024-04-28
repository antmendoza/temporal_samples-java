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

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.taskinteraction.activity.ActivityTaskImpl;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class WorkflowWithTasksImplTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          // .setNamespace("default")
          // .setUseExternalService(true)
          .setDoNotStart(true)
          .build();

  @Test
  public void testEnd2End() {

    final WorkflowClient workflowClient = testWorkflowRule.getTestEnvironment().getWorkflowClient();
    testWorkflowRule
        .getWorker()
        .registerWorkflowImplementationTypes(
            WorkflowWithTasksImpl.class, WorkflowTaskManagerImpl.class);

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new ActivityTaskImpl(workflowClient));
    testWorkflowRule.getTestEnvironment().start();

    WorkflowWithTasks workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                WorkflowWithTasks.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId(WorkflowWithTasks.WORKFLOW_ID)
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // TODO
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(2));

    WorkflowTaskManager workflowManager =
        workflowClient.newWorkflowStub(WorkflowTaskManager.class, WorkflowTaskManager.WORKFLOW_ID);

    final List<Task> pendingTask = getPendingTask(workflowManager);
    assertEquals(2, pendingTask.size());

    // Let's complete the two pending task. Send update to the workflow that holds and keep tasks
    // state
    workflowManager.completeTaskByToken(pendingTask.get(0).getToken());
    workflowManager.completeTaskByToken(pendingTask.get(1).getToken());

    // TODO
    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(2));

    assertEquals(1, getPendingTask(workflowManager).size());
    workflowManager.completeTaskByToken(getPendingTask(workflowManager).get(0).getToken());

    // Wait workflow to complete
    workflowClient.newUntypedWorkflowStub(execution.getWorkflowId()).getResult(Void.class);

    final DescribeWorkflowExecutionResponse describeWorkflowExecutionResponse =
        getDescribeWorkflowExecutionResponse(workflowClient, execution);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecutionResponse.getWorkflowExecutionInfo().getStatus());
  }

  private DescribeWorkflowExecutionResponse getDescribeWorkflowExecutionResponse(
      final WorkflowClient workflowClient, final WorkflowExecution execution) {
    return workflowClient
        .getWorkflowServiceStubs()
        .blockingStub()
        .describeWorkflowExecution(
            DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(testWorkflowRule.getTestEnvironment().getNamespace())
                .setExecution(execution)
                .build());
  }

  private static List<Task> getPendingTask(final WorkflowTaskManager workflowManager) {
    return workflowManager.getPendingTask();
  }
}
