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

package io.temporal.samples.asyncchild;

import static org.junit.Assert.assertNotNull;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class AsyncChildTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(ParentWorkflowImpl.class, ChildWorkflowImpl.class)
          .setDoNotStart(true)
          .build();

  @Test
  public void testAsyncChildWorkflow() {
    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(new ActivityImpl(workflowClient));

    testWorkflowRule.getTestEnvironment().start();

    ParentWorkflow parentWorkflow =
        workflowClient.newWorkflowStub(
            ParentWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowExecution childExecution = parentWorkflow.executeParent();

    DescribeWorkflowExecutionResponse childDescribeWorkflowExecution = parentWorkflow.queryChild();

    Assert.assertNotNull(childDescribeWorkflowExecution);

    assertNotNull(childExecution);
  }
}
