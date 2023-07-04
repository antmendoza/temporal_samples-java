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

package io.temporal.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.hello.HelloQuery.GreetingWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

/** Unit test for {@link HelloQuery}. Doesn't use an external Temporal service. */
public class HelloQueryTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder().setWorkflowTypes(HelloQuery.GreetingWorkflowImpl.class).build();

  @Test(timeout = 5000000)
  public void testQuery() {
    // Get a workflow stub using the same task queue the worker uses.
    String workflowId = "my-workflowId";

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .build();
    WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    GreetingWorkflow workflow =
        workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to query.
    WorkflowClient.start(workflow::createGreeting, "World");

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub.
    assertEquals("Hello World!", workflow.queryGreeting());

    // Unit tests should call testWorkflowRule.getTestEnvironment().sleep.
    // It allows skipping the time if workflow is just waiting on a timer
    // and executing tests of long running workflows very fast.
    // Note that this unit test executes under a second and not
    // over 3 as it would if Thread.sleep(3000) was called.
    TestWorkflowEnvironment testEnvironment = testWorkflowRule.getTestEnvironment();

    testEnvironment.sleep(Duration.ofMillis(1));
    assertEquals("Hello World!", workflow.queryGreeting());

    testEnvironment.sleep(Duration.ofMillis(1));
    assertEquals("Hello World!", workflow.queryGreeting());

    testEnvironment.sleep(Duration.ofDays(1));
    assertEquals("Bye World!", workflow.queryGreeting());

    WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
    assertEquals("done", workflowStub.getResult(String.class));
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        workflowClient
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(
                DescribeWorkflowExecutionRequest.newBuilder()
                    .setExecution(workflowStub.getExecution())
                    .setNamespace(testEnvironment.getNamespace())
                    .build())
            .getWorkflowExecutionInfo()
            .getStatus());
  }
}
