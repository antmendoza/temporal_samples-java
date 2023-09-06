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
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HelloCancelParallel {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityWorkflow";

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE, WorkerOptions.newBuilder().build());

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

    WorkflowClient.start(workflow::getGreeting, "world");

    sleep(2000);
    WorkflowStub.fromTyped(workflow).cancel();

    // Display workflow execution results
    // ystem.out.println("result " + result);
    // System.exit(0);
  }

  private static void sleep(int l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod
    String startAndWaitSecondsWithHeartbeat(int sleepSeconds);

    @ActivityMethod
    String other(int sleepSeconds);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build());

    private boolean activitiesExecuted;

    @Override
    public String getGreeting(String name) {

      final List<Promise> promises = new ArrayList<>();

      CancellationScope scope =
          Workflow.newCancellationScope(
              () -> {
                promises.add(
                    Async.procedure(
                        () -> {
                          activities.startAndWaitSecondsWithHeartbeat(50);
                          activities.startAndWaitSecondsWithHeartbeat(3);
                          activities.startAndWaitSecondsWithHeartbeat(3);
                          activitiesExecuted = true;
                        }));
              });

      scope.run();

      boolean processExecuted = Workflow.await(Duration.ofSeconds(4), () -> activitiesExecuted);

      if (!processExecuted) {
        System.out.println("Cancelling scope....");
        scope.cancel();
      }

      try {
        promises.get(0).get();
      } catch (ActivityFailure e) {

        if (!(e.getCause() instanceof CanceledFailure)) {

          CanceledFailure c = ((CanceledFailure) e.getCause());

          // We might want to fail the workflow or something.
          throw e;
        }
      }

      return "done";
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String startAndWaitSecondsWithHeartbeat(int sleepSeconds) {

      try {
        for (int a = 0; a < sleepSeconds; a++) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          System.out.println("hearbeat .... ");
          Activity.getExecutionContext().heartbeat("");
        }
      } catch (ActivityCompletionException e) {
        System.out.println("activity cancelled: " + e);
        throw e;
      }

      return "time sleep " + sleepSeconds;
    }

    @Override
    public String other(int sleepSeconds) {
      return this.startAndWaitSecondsWithHeartbeat(sleepSeconds);
    }
  }
}
