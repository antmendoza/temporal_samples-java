package io.antmendoza.samples.Murex;

import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;

public class TestUtilWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
  private TestUtilInterceptorTracker testUtilInterceptorTracker;

  public TestUtilWorkflowOutboundCallsInterceptor(
      WorkflowOutboundCallsInterceptor outboundCalls,
      TestUtilInterceptorTracker testUtilInterceptorTracker) {
    super(outboundCalls);
    this.testUtilInterceptorTracker = testUtilInterceptorTracker;
  }

  @Override
  public void continueAsNew(ContinueAsNewInput input) {
    this.testUtilInterceptorTracker.trackContinueAsNewInvocation(input);
    super.continueAsNew(input);
  }

  @Override
  public CancelWorkflowOutput cancelWorkflow(CancelWorkflowInput input) {
    this.testUtilInterceptorTracker.trackCancelWorkflow(input);
    return super.cancelWorkflow(input);
  }
}
