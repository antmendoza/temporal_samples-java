package io.antmendoza.samples.Murex;

import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;

public class TestUtilWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
  public TestUtilWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor outboundCalls) {
    super(outboundCalls);
  }

  @Override
  public void continueAsNew(ContinueAsNewInput input) {

    System.out.println(">>>>>>>>>> " + input.getWorkflowType());

    super.continueAsNew(input);
  }
}
