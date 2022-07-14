package io.temporal.samples.asyncchild;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;

@ActivityInterface
public interface Activity {

  @ActivityMethod
  DescribeWorkflowExecutionResponse queryChild(String childWorkflowId);
}
