package io.antmendoza.samples.Murex;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StageA {

  @WorkflowMethod
  void run(StageARequest request);

  class StageARequest {
    public final String id = "";
  }

  class StageAImpl implements StageA {

    @Override
    public void run(StageARequest request) {}
  }
}
