package io.antmendoza.samples._20231006;

import io.temporal.activity.ActivityExecutionContext;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;

public class TestUtilActivityInboundCallsInterceptor implements ActivityInboundCallsInterceptor {
  public TestUtilActivityInboundCallsInterceptor(ActivityInboundCallsInterceptor next) {}

  @Override
  public void init(ActivityExecutionContext context) {}

  @Override
  public ActivityOutput execute(ActivityInput input) {
    return null;
  }
}
