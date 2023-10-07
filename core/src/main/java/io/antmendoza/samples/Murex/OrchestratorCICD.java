package io.antmendoza.samples.Murex;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrchestratorCICD {

  @WorkflowMethod
  void run(OrchestratorRequest request);
}
