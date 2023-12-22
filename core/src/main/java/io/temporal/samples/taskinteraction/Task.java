package io.temporal.samples.taskinteraction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Task {

  private String token;
  private Object data;
  private STATE state;

  public Task() {}

  public Task(String token) {
    this.token = token;
    this.state = STATE.PENDING;
  }

  public String getToken() {
    return token;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public <T> T result(Class<T> tClass) {
    return (T) data;
  }

  public void setState(STATE state) {
    this.state = state;
  }

  public STATE getState() {
    return state;
  }

  @JsonIgnore
  public boolean isCompleted() {
    return STATE.COMPLETED == this.state;
  }

  @Override
  public String toString() {
    return "Task{" + "token='" + token + '\'' + ", data=" + data + ", state=" + state + '}';
  }

  public enum STATE {
    PENDING,
    STARTED,
    COMPLETED
  }
}
