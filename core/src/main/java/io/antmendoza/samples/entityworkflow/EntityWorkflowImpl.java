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

package io.antmendoza.samples.entityworkflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class EntityWorkflowImpl implements EntityWorkflow {

  private final List<String> signals = new ArrayList<>();
  private final List<String> signalsNotProcessed = new ArrayList<>();
  private boolean exit = false;

  @Override
  public void execute(EntityInput input) {

    final ActivityOptions options =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build();

    ActivityEntityWorkflow activity =
        Workflow.newActivityStub(ActivityEntityWorkflow.class, options);

    boolean b = !this.exit;
    while (b) {
      Workflow.await(() -> !this.signals.isEmpty() || this.exit);

      if (this.exit) {
        return;
      }

      final String messageToProcess = this.signals.remove(0);
      switch (messageToProcess) {
        case "doX":
          System.out.println("call doX : " + messageToProcess);
          activity.doX();
          break;
        case "doY":
          System.out.println("call doY : " + messageToProcess);
          activity.doY();
          break;
        default:
          signalsNotProcessed.add(messageToProcess);
          break;
      }
    }
  }

  @Override
  public void doX(String doX) {
    this.signals.add(doX);
  }

  @Override
  public void doY(String doY) {
    this.signals.add(doY);
  }

  @Override
  public void exit() {
    this.exit = true;
  }
}
