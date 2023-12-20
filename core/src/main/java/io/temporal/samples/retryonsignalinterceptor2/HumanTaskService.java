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

  private final Map<String, HumanTask> tasks = new HashMap();
  private final AtomicInteger atomicInteger = new AtomicInteger(1);

  public HumanTaskService() {

    Workflow.registerListener(
        new HumanTaskClient() {

          @Override
          public void changeStatus(TaskRequest taskRequest) {

            if (taskRequest.status == HumanTaskService.STATUS.COMPLETED) {
              tasks.remove(taskRequest.getToken());
            }
          }

          @Override
          public List<HumanTask> getHumanTasks() {
            return new ArrayList(tasks.values());
          }
        });
  }

  public <T> T execute(Supplier<T> supplier, String token) {

    final HumanTask humanTask = new HumanTask(this, supplier, token);
    tasks.put(token, humanTask);

    humanTask.start();

    Workflow.await(() -> !tasks.containsValue(humanTask));
    // TODO
    return null;
  }

  public String generateToken() {
    // return Workflow.getInfo().getWorkflowId() + "" + Workflow.currentTimeMillis() +
    // atomicInteger.getAndIncrement();
    return "" + atomicInteger.getAndIncrement();
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
