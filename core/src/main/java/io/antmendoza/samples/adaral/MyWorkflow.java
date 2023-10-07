package io.antmendoza.samples.adaral;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MyWorkflow {

  @WorkflowMethod
  void execute();
}
