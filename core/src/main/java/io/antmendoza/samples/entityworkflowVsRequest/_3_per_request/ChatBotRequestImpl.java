package io.antmendoza.samples.entityworkflowVsRequest._3_per_request;

import static io.antmendoza.samples.entityworkflowVsRequest.Taskqueue.TASK_QUEUE;

import io.antmendoza.samples.entityworkflowVsRequest.ProcessMessage;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ChatBotRequestImpl implements ChatBotRequest {
  private final ProcessMessage processMessage =
      Workflow.newActivityStub(
          ProcessMessage.class,
          ActivityOptions.newBuilder()
              .setTaskQueue(TASK_QUEUE)
              .setStartToCloseTimeout(Duration.ofSeconds(10))
              .build());

  @Override
  public String start(String input) {

    return processMessage.process(input);
  }
}
