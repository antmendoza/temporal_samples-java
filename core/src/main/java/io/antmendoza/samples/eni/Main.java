package io.antmendoza.samples.eni;

import io.antmendoza.samples.entityworkflowVsRequest.ServiceFactory;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

public class Main {

  private static String TASK_QUEUE = "anything";

  public static void main(String[] args) {

    WorkflowServiceStubs service = ServiceFactory.createService();

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace(ServiceFactory.getNamespace()).build());

    WorkerFactory factory = WorkerFactory.newInstance(client);

    WorkerOptions build =
        WorkerOptions.newBuilder()
            // slot
            .setMaxConcurrentActivityExecutionSize(20)
            .setMaxConcurrentLocalActivityExecutionSize(20)
            .setMaxConcurrentWorkflowTaskExecutionSize(20)

            /// num concurrent pollers,
            .setMaxConcurrentActivityTaskPollers(200)
            .setMaxConcurrentWorkflowTaskPollers(10)
            .build();

    Worker worker = factory.newWorker(TASK_QUEUE, build);

    worker.registerWorkflowImplementationTypes(MyWorkflowImpl.class);

    worker.registerActivitiesImplementations(new MyActivitiesImpl());

    factory.start();

    MyWorkflow workflow =
        client.newWorkflowStub(
            MyWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("WORKFLOW_ID")
                .setTaskQueue(TASK_QUEUE)
                .build());

    boolean parallelExecution = true;
    workflow.execute(parallelExecution);

    System.exit(0);
  }
}
