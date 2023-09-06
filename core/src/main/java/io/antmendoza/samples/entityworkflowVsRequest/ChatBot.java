package io.antmendoza.samples.entityworkflowVsRequest;

import static io.antmendoza.samples.entityworkflowVsRequest.Taskqueue.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@WorkflowInterface
public interface ChatBot {

  static void startAndCompleteWorkflow(WorkflowClient client) {
    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();

    ChatBot workflow = client.newWorkflowStub(ChatBot.class, workflowOptions);

    // Start
    WorkflowClient.start(workflow::start);

    IntStream.rangeClosed(1, 20)
        .boxed()
        .forEach(
            n -> {
              CompletableFuture.runAsync(
                  () -> {
                    String response = workflow.processMessage("message " + n);
                    System.out.println("Response " + response);

                    if (n == 20) {
                      workflow.exit();
                    }
                  });

              try {
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @WorkflowMethod
  void start();

  @UpdateMethod
  String processMessage(String name);

  @SignalMethod
  void exit();
}
