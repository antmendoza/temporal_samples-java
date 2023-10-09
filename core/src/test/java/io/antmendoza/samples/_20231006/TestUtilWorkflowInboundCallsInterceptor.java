package io.antmendoza.samples._20231006;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;

public class TestUtilWorkflowInboundCallsInterceptor extends WorkflowInboundCallsInterceptorBase {
  private TestUtilInterceptorTracker testUtilInterceptorTracker;

  public TestUtilWorkflowInboundCallsInterceptor(
      WorkflowInboundCallsInterceptor next, TestUtilInterceptorTracker testUtilInterceptorTracker) {
    super(next);
    this.testUtilInterceptorTracker = testUtilInterceptorTracker;
  }

  @Override
  public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
    super.init(
        new TestUtilWorkflowOutboundCallsInterceptor(outboundCalls, testUtilInterceptorTracker));
  }

  @Override
  public WorkflowOutput execute(WorkflowInput input) {

    WorkflowInfo info = Workflow.getInfo();
    String workflowType = info.getWorkflowType();
    testUtilInterceptorTracker.trackNewWorkflowInvocation(
        new TestUtilInterceptorTracker.NewWorkflowInvocation(workflowType, input));
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
