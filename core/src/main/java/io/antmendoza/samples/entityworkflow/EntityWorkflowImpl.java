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

  private List<String> signals = new ArrayList<>();
  private boolean exit = false;

  private Value entityValue;

  @Override
  public void execute(EntityInput input) {

    //    this.input = input;
    this.signals = addSignals(input);
    this.exit = input.isExitSignal() || this.exit;
    this.entityValue = this.entityValue != null ? this.entityValue : input.getValue();

    final ActivityOptions options =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build();

    ActivityEntityWorkflow activity =
        Workflow.newActivityStub(ActivityEntityWorkflow.class, options);

    while (true) {

      Workflow.await(() -> !this.signals.isEmpty() || this.exit);

      // Wait for all the signals to be processed
      if (this.exit && this.signals.isEmpty()) {
        return;
      }

      final String messageToProcess = this.signals.remove(0);

      switch (messageToProcess) {
        case "doX":
          activity.doX();
          break;
        case "doY":
          activity.doY();
          break;
        default:
          activity.other(messageToProcess);
          break;
      }

      checkAndContinueAsNew();
    }
  }

  private List<String> addSignals(EntityInput input) {
    ArrayList<String> arrayList = new ArrayList<>(input.getSignals());
    arrayList.addAll(this.signals);
    return arrayList;
  }

  @Override
  public void doX(String doX) {

    this.signals.add(doX);

    //  checkAndContinueAsNew();
  }

  @Override
  public void doY(String doY) {
    this.signals.add(doY);

    //  checkAndContinueAsNew();
  }

  private void checkAndContinueAsNew() {
    long historyLength = Workflow.getInfo().getHistoryLength();
    // System.out.println("historyLength:  " + historyLength);

    if (historyLength > 10) {

      EntityInput entityInput = new EntityInput(this.entityValue, this.signals, this.exit);

      System.out.println("EntityInput:  " + entityInput);

      // System.out.println("Thread " + Thread.currentThread().getId());
      Workflow.continueAsNew(entityInput);
    }
  }

  @Override
  public void exit() {
    this.exit = true;
    // System.out.println("Workflow:exit: " + this.exit);
  }

  @Override
  public void otherSignal(String value) {

    // System.out.println("Thread " + Thread.currentThread().getId());

    // System.out.println("Workflow:otherSignal [" + value + "]; Workflow.isReplaying?: " +
    // Workflow.isReplaying());
    this.signals.add(value);
  }

  @Override
  public void updateEntityValue(Value value) {
    System.out.println("updateEntityValue:  " + value);

    this.entityValue = value;
  }

  @Override
  public Value getEntityValue() {
    return this.entityValue;
  }
}
