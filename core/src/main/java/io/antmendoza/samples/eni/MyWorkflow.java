package io.antmendoza.samples.eni;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {

  @WorkflowMethod
  void execute(boolean parallel);
}
