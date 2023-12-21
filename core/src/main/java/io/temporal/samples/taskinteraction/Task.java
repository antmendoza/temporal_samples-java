package io.temporal.samples.taskteraction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Task<T> {

  private String token;
  private T result;
  private TaskService.STATUS status;

  public Task() {}

  public Task(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
  }

  public void setResult(T result) {
    this.result = result;
  }

  public <T> T result() {
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
