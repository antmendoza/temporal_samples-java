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

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HelloCancelLongRetries {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloCancelLongRetries";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloCancelLongRetries";

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

    sleep(23000);

    WorkflowStub.fromTyped(workflow).signal("signal");

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

    @SignalMethod
    void signal();
  }

  @ActivityInterface
  public interface GreetingActivities {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod
    String startAndFail(boolean fails);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setCancellationType(ActivityCancellationType.TRY_CANCEL)
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(1)
                        .build())
                .build());

    CancellationScope scope;

    @Override
    public String getGreeting(String name) {

      final List<Promise<String>> results = new ArrayList<>();

      scope =
          Workflow.newCancellationScope(
              () -> {
                results.add(Async.function(() -> activities.startAndFail(true)));
              });

      scope.run();

      try {
        results.get(0).get();
      } catch (Exception e) {
        System.out.println(Instant.now() + ">>>>>>>>>>>>>>>.....");
        e.printStackTrace();
        System.out.println(e);
        // Retries
        activities.startAndFail(false);
      }

      return "done";
    }

    @Override
    public void signal() {
      System.out.println(Instant.now() + "Receiving the signal... ");

      scope.cancel();
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String startAndFail(boolean fail) {
      System.out.println("retrying inside activity...");
      if (fail) {
        throw ApplicationFailure.newFailure("intended", "MyFailure");
      }
      return "done";
    }
  }
}
