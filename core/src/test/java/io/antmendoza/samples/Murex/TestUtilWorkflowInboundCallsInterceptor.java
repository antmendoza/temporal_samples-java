package io.antmendoza.samples.Murex;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;

public class TestUtilWorkflowInboundCallsInterceptor extends WorkflowInboundCallsInterceptorBase {
  public TestUtilWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
    super(next);
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    super.init(new TestUtilWorkflowOutboundCallsInterceptor(outboundCalls));
  }

  @Override
  public WorkflowOutput execute(WorkflowInput input) {
    return super.execute(input);
  }

  @Override
  public void handleSignal(SignalInput input) {
    super.handleSignal(input);
  }

  @Override
  public QueryOutput handleQuery(QueryInput input) {
    return super.handleQuery(input);
  }
}
