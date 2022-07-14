package io.temporal.samples.asyncchild;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class ActivityImpl implements Activity {

  private final WorkflowClient workflowClient;
  private final WorkflowServiceStubs service;

  public ActivityImpl(final WorkflowClient workflowClient) {
    this.workflowClient = workflowClient;
    this.service = workflowClient.getWorkflowServiceStubs();
  }

  @Override
  public DescribeWorkflowExecutionResponse queryChild(final String childWorkflowId) {

    // WorkflowStub child = this.workflowClient.newUntypedWorkflowStub(childWorkflowId);
    // return child.query("querySomething", String.class);

    DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(workflowClient.getOptions().getNamespace())
            .setExecution(WorkflowExecution.newBuilder().setWorkflowId(childWorkflowId).build())
            .build();

    return service.blockingStub().describeWorkflowExecution(describeWorkflowExecutionRequest);
  }
}
