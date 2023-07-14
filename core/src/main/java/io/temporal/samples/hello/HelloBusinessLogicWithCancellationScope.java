package io.temporal.samples.hello;

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
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample Temporal Workflow Definition that executes a single Activity. */
public class HelloBusinessLogicWithCancellationScope {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloHelloBusinessLogicTimer";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloBusinessLogicTimer";

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE, WorkerOptions.newBuilder().build());

    /*
     * Register our workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /**
     * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
     * the Activity Type is a shared instance.
     */
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
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

    sleep(10000);

    String result = WorkflowStub.fromTyped(workflow).getResult(String.class);

    // Display workflow execution results
    System.out.println("result " + result);
    System.exit(0);
  }

  private static void sleep(int l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see WorkflowInterface
   * @see WorkflowMethod
   */
  @WorkflowInterface
  public interface GreetingWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /**
   * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
   * Workflow and contain any business logic that could perform long running computation, network
   * calls, etc.
   *
   * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
   *
   * @see ActivityInterface
   * @see ActivityMethod
   */
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

    /**
     * Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     *
     * <p>In the {@link ActivityOptions} definition the "setStartToCloseTimeout" option sets the
     * overall timeout that our workflow is willing to wait for activity to complete. For this
     * example it is set to 2 seconds.
     */
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

      List<Promise<String>> results = new ArrayList<>();

      AtomicBoolean activityCompleted = new AtomicBoolean(false);

      CancellationScope cancellationScope =
          Workflow.newCancellationScope(
              () -> {
                Promise<String> function =
                    Async.function(activities::startAndWaitSecondsWithHeartbeat, 6)
                        .thenApply(
                            result -> {
                              activityCompleted.set(true);
                              return result;
                            });
                results.add(function);
              });

      cancellationScope.run();

      boolean activityExecuted =
          Workflow.await(Duration.ofSeconds(3), () -> activityCompleted.get());

      if (!activityExecuted) {
        System.out.println("cancelling scope");
        cancellationScope.cancel();
      }

      for (Promise<String> activityResult : results) {
        try {

          activityResult.get(); // this should block

          activityResult.getFailure(); // we can check if this is not null
        } catch (ActivityFailure e) {
          System.out.println("ActivityFailure, scope cancelled");
          System.out.println(e.getCause());

          if (!(e.getCause() instanceof CanceledFailure)) {
            throw e;
          }
          throw e;
          // This workflow won't be marked as cancelled
          // because
          // throw new CanceledFailure("My cancelled");
        }
      }

      System.out.println("Before return...");

      return "done";
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

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
        System.out.println("Printed from inside: activity cancelled: " + e);
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
