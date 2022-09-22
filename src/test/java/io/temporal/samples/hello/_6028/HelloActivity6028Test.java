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

package io.temporal.samples.hello._6028;

import static org.junit.Assert.assertEquals;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.hello._6028.HelloActivity6028.GreetingActivitiesImpl;
import io.temporal.samples.hello._6028.HelloActivity6028.GreetingWorkflow;
import io.temporal.samples.hello._6028.HelloActivity6028.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HelloActivity6028Test {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(GreetingWorkflowImpl.class)
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder()
                  //                  .setDataConverter(new DefaultDataConverter(new
                  // JacksonJsonPayloadConverter()))
                  //                  .setDataConverter(new DefaultDataConverter(new
                  // JacksonJsonPayloadConverter()))
                  .build())
          .setDoNotStart(true)
          .build();

  @Test
  public void test() {

    testWorkflowRule.getWorker().registerActivitiesImplementations(new GreetingActivitiesImpl());

    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
    // Execute a workflow waiting for it to complete.
    String greeting =
        workflow.getGreeting(
            new BGreeting("keyStoredInSuperClass", "keyStoredInBGreeting", "keyStoredInCGreeting"));
    assertEquals(
        "keyStoredInSuperClass keyStoredInBGreeting keyStoredInSuperClass keyStoredInCGreeting",
        greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }
}
