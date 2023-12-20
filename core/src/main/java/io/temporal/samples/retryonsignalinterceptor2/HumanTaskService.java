package io.temporal.samples.retryonsignalinterceptor2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class HumanTaskService {

  private final Map<String, HumanTask> pendingTasks = new HashMap();
  private final AtomicInteger atomicInteger = new AtomicInteger(1);

  final HumanTaskClient listener =
      new HumanTaskClient() {

        @Override
        public void changeStatus(TaskRequest taskRequest) {
          if (taskRequest.status == STATUS.COMPLETED) {
            pendingTasks.remove(taskRequest.getToken());
          }
        }

        @Override
        public List<HumanTask> getPendingTasks() {
          return new ArrayList(pendingTasks.values());
        }
      };

  public HumanTaskService() {

    Workflow.registerListener(listener);
  }

  public <T> T execute(Supplier<T> supplier, String token) {

    final HumanTask humanTask = new HumanTask(supplier, token);
    pendingTasks.put(token, humanTask);

    humanTask.start();

    Workflow.await(() -> !pendingTasks.containsValue(humanTask));
    // TODO
    return null;
  }

  public String generateToken() {
    return Workflow.getInfo().getWorkflowId()
        + "-"
        + Workflow.currentTimeMillis()
        + "-"
        + atomicInteger.getAndIncrement();
  }

  public List<HumanTask> getPendingTasks() {
    return listener.getPendingTasks();
  }

  public enum STATUS {
    PENDING,
    STARTED,
    COMPLETED
  }

  public static class TaskRequest {

    private STATUS status;
    private String token;

    public TaskRequest() {}

    public TaskRequest(STATUS status, String token) {
      this.status = status;
      this.token = token;
    }

    @JsonIgnore
    public boolean isCompleted() {
      return this.status == STATUS.COMPLETED;
    }

    public String getToken() {
      return token;
    }
  }
}
