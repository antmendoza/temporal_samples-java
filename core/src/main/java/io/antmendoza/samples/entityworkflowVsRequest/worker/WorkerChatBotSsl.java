package io.antmendoza.samples.entityworkflowVsRequest.worker;

import static io.antmendoza.samples.entityworkflowVsRequest.Taskqueue.TASK_QUEUE;

import io.antmendoza.samples.entityworkflowVsRequest.ChatBotImpl;
import io.antmendoza.samples.entityworkflowVsRequest.ProcessMessageImpl;
import io.antmendoza.samples.entityworkflowVsRequest.ServiceFactory;
import io.antmendoza.samples.entityworkflowVsRequest._3_per_request.ChatBotRequestImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;

public class WorkerChatBotSsl {

  private int workflowCacheSize = 20;
  private int maxConcurrentActivityExecutionSize = 20;
  private int maxConcurrentLocalActivityExecutionSize = 20;
  private int maxConcurrentWorkflowTaskExecutionSize = 20;

  private int maxWorkflowThreadCount = 10;

  public WorkerChatBotSsl(
      int workflowCacheSize,
      int maxConcurrentActivityExecutionSize,
      int maxConcurrentLocalActivityExecutionSize,
      int maxConcurrentWorkflowTaskExecutionSize,
      int maxWorkflowThreadCount) {
    this.workflowCacheSize = workflowCacheSize;
    this.maxConcurrentActivityExecutionSize = maxConcurrentActivityExecutionSize;
    this.maxConcurrentLocalActivityExecutionSize = maxConcurrentLocalActivityExecutionSize;
    this.maxConcurrentWorkflowTaskExecutionSize = maxConcurrentWorkflowTaskExecutionSize;
    this.maxWorkflowThreadCount = maxWorkflowThreadCount;
  }

  public void start(WorkflowServiceStubs service) {

    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder().setNamespace(ServiceFactory.getNamespace()).build());

    WorkerFactoryOptions build =
        WorkerFactoryOptions.newBuilder()
            .setWorkflowCacheSize(workflowCacheSize)
            .setMaxWorkflowThreadCount(maxWorkflowThreadCount)
            .build();

    WorkerFactory factory = WorkerFactory.newInstance(client, build);

    WorkerOptions workerOptions =
        WorkerOptions.newBuilder()
            // .setMaxConcurrentWorkflowTaskPollers(1)
            // .setMaxConcurrentLocalActivityExecutionSize(1)
            .setMaxConcurrentActivityExecutionSize(maxConcurrentActivityExecutionSize)
            .setMaxConcurrentLocalActivityExecutionSize(maxConcurrentLocalActivityExecutionSize)
            .setMaxConcurrentWorkflowTaskExecutionSize(maxConcurrentWorkflowTaskExecutionSize)

            //                .setMaxConcurrentActivityTaskPollers(maxConcurrentActivityTaskPollers)
            //                .setMaxConcurrentWorkflowTaskPollers(maxConcurrentWorkflowTaskPollers)
            .build();
    Worker worker = factory.newWorker(TASK_QUEUE, workerOptions);

    worker.registerWorkflowImplementationTypes(ChatBotImpl.class, ChatBotRequestImpl.class);
    worker.registerActivitiesImplementations(new ProcessMessageImpl());

    factory.start();
  }
}
