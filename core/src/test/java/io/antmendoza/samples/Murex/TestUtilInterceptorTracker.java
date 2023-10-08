package io.antmendoza.samples.Murex;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import java.util.ArrayList;
import java.util.List;

public class TestUtilInterceptorTracker {
  private List<WorkflowOutboundCallsInterceptor.ContinueAsNewInput> continueAsNewInvocations =
      new ArrayList<>();
  private List<NewWorkflowInvocation> newInvocations = new ArrayList<>();

  public void trackContinueAsNewInvocation(
      WorkflowOutboundCallsInterceptor.ContinueAsNewInput input) {
    this.continueAsNewInvocations.add(input);
  }

  public boolean hasContinuedAsNewTimes(String workflowType, int times) {
    return continueAsNewInvocations.stream()
            .filter(l -> workflowType.equals(l.getWorkflowType()))
            .count()
        == times;
  }

  public boolean hasNewWorkflowInvocationTimes(String workflowType, int times) {
    return newInvocations.stream().filter(l -> workflowType.equals(l.workflowType)).count()
        == times;
  }

  public void trackNewWorkflowInvocation(NewWorkflowInvocation newWorkflowInvocation) {
    this.newInvocations.add(newWorkflowInvocation);
  }

  public static class NewWorkflowInvocation {
    private String workflowType;
    private WorkflowInboundCallsInterceptor.WorkflowInput input;

    public NewWorkflowInvocation(
        String workflowType, WorkflowInboundCallsInterceptor.WorkflowInput input) {
      this.workflowType = workflowType;
      this.input = input;
    }
  }
}
