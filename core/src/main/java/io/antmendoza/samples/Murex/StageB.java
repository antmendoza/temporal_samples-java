package io.antmendoza.samples.Murex;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StageB {

  @WorkflowMethod
  void run(StageBRequest stageBRequest);

  class StageBRequest {
    public final String id = "";
  }

  class StageBImpl implements StageB {
    @Override
    public void run(StageBRequest stageBRequest) {}
  }
}
