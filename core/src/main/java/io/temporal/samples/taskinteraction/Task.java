package io.temporal.samples.taskinteraction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Task {

  private String token;
  private Object result;
  private TaskService.STATUS status;

  public Task() {}

  public Task(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public <T> T result(Class<T> tClass) {
    return (T) result;
  }

  public void setStatus(TaskService.STATUS status) {
    this.status = status;
  }

  @JsonIgnore
  public boolean isCompleted() {
    return TaskService.STATUS.COMPLETED == this.status;
  }
}
