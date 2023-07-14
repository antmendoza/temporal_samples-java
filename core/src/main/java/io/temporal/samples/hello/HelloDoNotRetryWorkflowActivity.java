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

import static io.temporal.internal.sync.WorkflowInternal.DEFAULT_VERSION;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
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

/** Sample Temporal Workflow Definition that executes a single Activity. */
public class HelloDoNotRetryWorkflowActivity {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloDoNotRetryWorkflowActivity";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloDoNotRetryWorkflowActivity";

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
                // .setWorkflowRunTimeout(Duration.ofMinutes(2))
                .setWorkflowId(WORKFLOW_ID)
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setMaximumAttempts(2)
                        .setDoNotRetry(NullPointerException.class.getSimpleName())
                        .build())
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */
    String greeting = workflow.getGreeting("World");

    // Display workflow execution results
    System.out.println(greeting);
    System.exit(0);
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
    @ActivityMethod(name = "greet")
    String composeGreeting(String greeting, String name);
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
                .setHeartbeatTimeout(Duration.ofSeconds(3))
                .setStartToCloseTimeout(Duration.ofMinutes(40))
                .build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      String hello = activities.composeGreeting("Helloee", name);

      String b = null;
      // System.out.println(b.toString());

      ApplicationFailure.newFailure("", NullPointerException.class.getSimpleName());

      int version = Workflow.getVersion("test", Workflow.DEFAULT_VERSION, 1);

      if (version == DEFAULT_VERSION) {
        activities.composeGreeting("Bye", name);
      }

      int version2 = Workflow.getVersion("version2", Workflow.DEFAULT_VERSION, 2);

      if (version2 == 2) {
        activities.composeGreeting("Bye version2", name);
      }

      int version44 = Workflow.getVersion("test44", Workflow.DEFAULT_VERSION, 10);

      if (version44 == 10) {
        activities.composeGreeting("Bye", name);
      }

      Workflow.sleep(Duration.ofMinutes(2));

      return hello;
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      log.info("Composing greeting...");
      //
      //      for (int i = 0; i < 200; i++) {
      //
      //        if (i < 5 || i > 10) {
      //          try {
      //
      //            System.out.println("Thread: " + Thread.currentThread().getId() + " heartbeat ...
      // ");
      //            Activity.getExecutionContext().heartbeat("");
      //
      //          } catch (Exception e) {
      //            e.printStackTrace();
      //          }
      //        } else {
      //          System.out.println("Thread: " + Thread.currentThread().getId() + " Skip heartbeat
      // ... ");
      //        }
      //
      //        try {
      //          Thread.sleep(1000);
      //        } catch (InterruptedException e) {
      //          throw new RuntimeException(e);
      //        }
      //      }

      return greeting + " " + name + "!";
    }
  }
}
