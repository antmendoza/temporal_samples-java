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

package io.temporal.samples.humaninteraction;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MyWorkflowImpl implements MyWorkflow {

  final HumanTaskService humanTaskService = new HumanTaskService();
  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());

  @Override
  public void execute() {

    List<Promise<String>> activities = new ArrayList<>();

    activities.add(
        Async.function(
            () -> {
              String result =
                  humanTaskService.execute(
                      () -> activity.execute(), humanTaskService.generateToken());
              return result;
            }));

    activities.add(
        Async.function(
            () -> {
              String result =
                  humanTaskService.execute(
                      () -> activity.execute(), humanTaskService.generateToken());
              return result;
            }));

    Promise.allOf(activities).get();

    humanTaskService.execute(() -> activity.execute(), humanTaskService.generateToken());
  }
}
