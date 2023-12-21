/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.taskinteraction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.temporal.workflow.Workflow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskService {

  private final Map<String, Task> tasks = new HashMap();
  // The listener expose signal and query methods that
  // will allow us to interact with the workflow execution if needed
  final TaskClient listener =
      new TaskClient() {

        @Override
        public void changeStatus(TaskRequest taskRequest) {

          tasks.get(taskRequest.getToken()).setResult(taskRequest.getData());

          Task t = tasks.get(taskRequest.getToken());

          t.setStatus(STATUS.COMPLETED);

          tasks.put(t.getToken(), t);
        }

        @Override
        public List<Task> getPendingTasks() {
          return tasks.values().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
        }
      };

  public TaskService() {
    Workflow.registerListener(listener);
  }

  public <T> T execute(Callback<T> callback, String token) {

    final Task task = new Task(token);
    tasks.put(token, task);

    callback.execute();

    // Block the
    Workflow.await(() -> task.isCompleted());

    // Workflow.await(() -> !pendingTasks.values().stream().filter(t -> t.isCompleted());

    return (T) task.result(String.class);
  }

  public List<Task> getPendingTasks() {
    return listener.getPendingTasks();
  }

  public enum STATUS {
    PENDING,
    STARTED,
    COMPLETED
  }

  public interface Callback<T> {
    T execute();
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
