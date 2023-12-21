package io.temporal.samples.taskteraction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskService {

  public interface Callback<T> {
    T execute();
  }

  private final Map<String, Task> pendingTasks = new HashMap();

  // The listener expose signal and query methods that
  // will allow us to interact with the workflow execution if needed
  final TaskClient listener =
      new TaskClient() {

        @Override
        public void changeStatus(TaskRequest taskRequest) {

          pendingTasks.get(taskRequest.getToken()).setResult(taskRequest.getData());

          Task t = pendingTasks.get(taskRequest.getToken());

          t.setStatus(STATUS.COMPLETED);
          if (taskRequest.status == STATUS.COMPLETED) {
            pendingTasks.remove(taskRequest.getToken());
          }
        }

        @Override
        public List<Task> getPendingTasks() {
          return new ArrayList(pendingTasks.values());
        }
      };

  public TaskService() {
    Workflow.registerListener(listener);
  }

  public <T> T execute(Callback<T> callback, String token) {

    final Task<T> task = new Task(token);
    pendingTasks.put(token, task);

    callback.execute();

    // Block the
    Workflow.await(() -> !pendingTasks.containsValue(task));
    Workflow.await(() -> !task.isCompleted());

    // Workflow.await(() -> !pendingTasks.values().stream().filter(t -> t.isCompleted());

    return task.result();
  }

  public List<Task> getPendingTasks() {
    return listener.getPendingTasks();
  }

  public enum STATUS {
    PENDING,
    STARTED,
    COMPLETED
  }

  public static class TaskRequest {

    private STATUS status;
    private String data;
    private String token;

    public TaskRequest() {}

    public TaskRequest(STATUS status, String data, String token) {
      this.status = status;
      this.data = data;
      this.token = token;
    }

    @JsonIgnore
    public boolean isCompleted() {
      return this.status == STATUS.COMPLETED;
    }

    public String getToken() {
      return token;
    }

    public String getData() {
      return data;
    }
  }
}
