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

package io.antmendoza.samples.hello;

import io.temporal.activity.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloActivityCancelToRetry {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityCancelToRetry";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityCancelToRetry";

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod(name = "greet")
    String composeGreeting(String greeting, String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setRetryOptions(RetryOptions.newBuilder().setDoNotRetry().build())
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setStartToCloseTimeout(Duration.ofSeconds(20))
                .setHeartbeatTimeout(Duration.ofSeconds(10))
                .build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.

      try {

        activities.composeGreeting("Hello", name);

      } catch (Exception e) {

        System.out.println("Cancelled >>  " + ((CanceledFailure) e.getCause()));
      }
      // String hello = activities.composeGreeting("Hello", name);

      return "hello";
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      log.info("Composing greeting...");

      try {
        for (int i = 0; i < 10; i++) {
          Activity.getExecutionContext().heartbeat("");

          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }

      } catch (Exception e) {
        log.info(">>> dentro del activity: " + e);
      }

      log.info("Composing greeting...return ");

      return null;
    }
  }

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    WorkerOptions options = WorkerOptions.newBuilder().build();
    Worker worker = factory.newWorker(TASK_QUEUE, options);

    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    WorkflowClient.start(workflow::getGreeting, "World");

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    client.newUntypedWorkflowStub(WORKFLOW_ID).cancel();

    // Display workflow execution results
    // System.out.println(greeting);
    // System.exit(0);
  }
}
