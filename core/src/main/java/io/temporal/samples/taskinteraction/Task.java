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

public class Task {

  private String token;
  private TaskTitle title;

  public Task() {}

  public Task(String token, TaskTitle title) {
    this.token = token;
    this.title = title;
  }

  public String getToken() {
    return token;
  }

  public TaskTitle getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return "Task{" + "token='" + token + '\'' + ", title=" + title + '}';
  }

  public static class TaskTitle {
    private String value;

    public TaskTitle() {}

    public TaskTitle(final String value) {

      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "TaskTitle{" + "value='" + value + '\'' + '}';
    }
  }
}
