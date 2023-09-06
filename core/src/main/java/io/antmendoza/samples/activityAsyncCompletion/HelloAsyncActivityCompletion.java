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

package io.antmendoza.samples.activityAsyncCompletion;

import io.temporal.activity.*;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/** Sample Temporal Workflow Definition that demonstrates asynchronous Activity Execution */
public class HelloAsyncActivityCompletion {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloAsyncActivityCompletionTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloAsyncActivityCompletionWorkflow";

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {

    /** Define the activity method which can be called during workflow execution */
    String composeGreeting(String greeting, String name);
  }

  // Define the workflow implementation which implements the getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10)).build());

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  static class GreetingActivitiesImpl implements GreetingActivities {

    private final ActivityCompletionClient completionClient;

    GreetingActivitiesImpl(ActivityCompletionClient completionClient) {

      WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

      WorkflowClient client = WorkflowClient.newInstance(service);

      this.completionClient = client.newActivityCompletionClient();
    }

    @Override
    public String composeGreeting(String greeting, String name) {

      // Get the activity execution context
      ActivityExecutionContext context = Activity.getExecutionContext();

      // Set a correlation token that can be used to complete the activity asynchronously
      byte[] taskToken = context.getTaskToken();

      sleep(2000);

      context.doNotCompleteOnReturn();

      sleep(2000);

      ForkJoinPool.commonPool().execute(() -> composeGreetingAsync(taskToken, greeting, name));

      // Since we have set doNotCompleteOnReturn(), the workflow action method return value is
      // ignored.
      return "ignored";
    }

    // Method that will complete action execution using the defined ActivityCompletionClient
    private void composeGreetingAsync(byte[] taskToken, String greeting, String name) {
      String result = greeting + " " + name + "!!";

      String token = Base64.getEncoder().encodeToString(taskToken);

      System.out.println("token: [" + token + "]");
      System.out.println("token: [" + token + "]");

      sleep(2000);

      System.out.println("token: [" + token + "]");
      System.out.println("token: [" + token + "]");

      // Complete our workflow activity using ActivityCompletionClient
      completionClient.complete(Base64.getDecoder().decode(token), result);
    }
  }

  private static void sleep(int l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    ActivityCompletionClient completionClient = client.newActivityCompletionClient();
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl(completionClient));

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");

    // Wait for workflow execution to complete and display its results.
    System.out.println(greeting.get());
    System.exit(0);
  }
}
