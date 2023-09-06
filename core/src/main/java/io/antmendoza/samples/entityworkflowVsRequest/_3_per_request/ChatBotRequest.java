package io.antmendoza.samples.entityworkflowVsRequest._3_per_request;

import static io.antmendoza.samples.entityworkflowVsRequest.Taskqueue.TASK_QUEUE;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ChatBotRequest {

  static void startAndCompleteWorkflow(WorkflowClient client) {
    WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();

    ChatBotRequest workflow = client.newWorkflowStub(ChatBotRequest.class, workflowOptions);

    String response = workflow.start("message ");
    System.out.println("Response " + response);
  }

  @WorkflowMethod
  String start(String input);
}
