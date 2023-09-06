package io.antmendoza.samples.entityworkflowVsRequest;

import static io.antmendoza.samples.entityworkflowVsRequest.Taskqueue.TASK_QUEUE;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

public class ChatBotImpl implements ChatBot {
  private final ProcessMessage processMessage =
      Workflow.newActivityStub(
          ProcessMessage.class,
          ActivityOptions.newBuilder()
              .setTaskQueue(TASK_QUEUE)
              .setStartToCloseTimeout(Duration.ofSeconds(10))
              .build());
  private final List<String> pendingMessages = new ArrayList<>();
  private final Logger log = Workflow.getLogger("ChatBotImpl");
  private boolean exit = false;
  private String response;

  @Override
  public void start() {

    while (!this.exit) {
      Workflow.await(Duration.ofMinutes(5), () -> !this.pendingMessages.isEmpty() || this.exit);

      if (!this.pendingMessages.isEmpty()) {

        String remove = this.pendingMessages.remove(0);
        String response = processMessage.process(remove);
        this.response = "response to message: " + remove + "\n" + "    " + response;
      }

      if (this.exit && this.pendingMessages.isEmpty()) {
        return;
      }
    }
  }

  @Override
  public String processMessage(String message) {
    this.response = null;
    log.info("Receiving message " + message);
    this.pendingMessages.add(message);
    Workflow.await(() -> this.response != null);
    log.info("Completing message " + message);
    return response;
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
