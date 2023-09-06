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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.TemporalFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;

public class HelloCancelTimerExternalCancellation {

  // Define the task queue name
  final String TASK_QUEUE = "tq+" + this.getClass().getName();

  // Define our workflow unique id
  final String WORKFLOW_ID = this.getClass().getName();

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    HelloCancelTimerExternalCancellation this_ = new HelloCancelTimerExternalCancellation();
    Worker worker = factory.newWorker(this_.TASK_QUEUE, WorkerOptions.newBuilder().build());

    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(this_.WORKFLOW_ID)
                .setTaskQueue(this_.TASK_QUEUE)
                .build());

    WorkflowClient.start(workflow::getGreeting, "world");

    sleep(2000);

    WorkflowStub.fromTyped(workflow).cancel();

    sleep(2000);

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

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @Override
    public String getGreeting(String name) {

      Promise<Void> timer = Workflow.newTimer(Duration.ofSeconds(3));

      try {
        timer.get();
      } catch (Exception e) {
        if (e instanceof TemporalFailure) {
          // Timer cancelled
          System.out.println("Timer cancelled");
        }

        System.out.println(e);
      }

      return "done";
    }
  }
}
