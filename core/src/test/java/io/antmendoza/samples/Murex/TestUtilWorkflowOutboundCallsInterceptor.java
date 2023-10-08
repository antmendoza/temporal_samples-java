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

    System.out.println(">>>>>>>>>> " + input.getWorkflowType());

    super.continueAsNew(input);
  }
}
