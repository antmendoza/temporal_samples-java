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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityInput {

  private Value value;
  private List<String> signals;
  private boolean exitSignal;

  public EntityInput() {
    this(new Value(), new ArrayList<>(), false);
  }

  public EntityInput(Value value, List<String> signals, boolean exitSignal) {
    this.value = value;
    this.signals = signals;
    this.exitSignal = exitSignal;
  }

  public EntityInput(Value value) {
    this(value, new ArrayList<>(), false);
  }

  public Value getValue() {
    return value;
  }

  public void setValue(Value value) {
    this.value = value;
  }

  public List<String> getSignals() {
    return signals;
  }

  public void setSignals(List<String> signals) {
    this.signals = signals;
  }

  public boolean isExitSignal() {
    return exitSignal;
  }

  public void setExitSignal(boolean exitSignal) {
    this.exitSignal = exitSignal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EntityInput)) return false;
    EntityInput that = (EntityInput) o;
    return exitSignal == that.exitSignal
        && Objects.equals(value, that.value)
        && Objects.equals(signals, that.signals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, signals, exitSignal);
  }

  @Override
  public String toString() {
    return "EntityInput{"
        + "value="
        + value
        + ", signals="
        + signals
        + ", exitSignal="
        + exitSignal
        + '}';
  }
}
