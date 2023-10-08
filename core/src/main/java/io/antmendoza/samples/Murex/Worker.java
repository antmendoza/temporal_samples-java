package io.antmendoza.samples.Murex;

import io.antmendoza.samples.hello.HelloActivityCancelToRetry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

public class Worker {
  private static String TASK_QUEUE = "TASK_QUEUE";
  private static String WORKFLOW_ID = "WORKFLOW_ID";

  public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    WorkerOptions options = WorkerOptions.newBuilder().build();
    io.temporal.worker.Worker worker = factory.newWorker(TASK_QUEUE, options);

    worker.registerWorkflowImplementationTypes(OrchestratorCICD.OrchestratorCICDImpl.class);

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    HelloActivityCancelToRetry.GreetingWorkflow workflow =
        client.newWorkflowStub(
            HelloActivityCancelToRetry.GreetingWorkflow.class,
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
