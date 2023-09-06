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
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;

public class HelloCancelAsyncWithExternalCancellation {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloCancelAsyncWithExternalCancellation";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloCancelAsyncWithExternalCancellation";

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
                .setWorkflowRunTimeout(Duration.ofSeconds(60))
                .build());

    WorkflowClient.start(workflow::getGreeting, "world");

    sleep(10000);
    System.out.println("cancelling workflow ... ");

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

    @ActivityMethod
    String startAndThrowIfAttemptsLessThan(int sleepSeconds);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(20))
                        .setBackoffCoefficient(1)
                        .build())
                .setHeartbeatTimeout(Duration.ofSeconds(2))
                // .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build());

    @Override
    public String getGreeting(String name) {

      Promise jobMonitor = Async.function(activities::startAndThrowIfAttemptsLessThan, 3);

      try {
        jobMonitor.get();
      } catch (ActivityFailure e) {

        System.out.println("getCause ... " + e.getCause());

        if (!(e.getCause() instanceof CanceledFailure)) {
          // We might want to fail the workflow or something.

          System.out.println("Rethrowing ...  " + e.getCause());

          throw e;
        }
      }

      System.out.println("Result... ");

      return "done";
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String startAndThrowIfAttemptsLessThan(int sleepSeconds) {

      Activity.getExecutionContext().heartbeat("");

      if (Activity.getExecutionContext().getInfo().getAttempt() < 3) {
        throw ApplicationFailure.newFailure("", "");
      }

      return "time sleep " + sleepSeconds;
    }
  }
}
