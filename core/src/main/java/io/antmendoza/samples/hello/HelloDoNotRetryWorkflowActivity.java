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
                .setHeartbeatTimeout(Duration.ofSeconds(3))
                .setStartToCloseTimeout(Duration.ofMinutes(40))
                .build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      String hello = activities.composeGreeting("Helloee", name);

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
