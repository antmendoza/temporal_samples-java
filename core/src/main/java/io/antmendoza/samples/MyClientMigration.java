package io.antmendoza.samples;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class MyClientMigration {

  public WorkflowClient getClient(String workflowId) {

    if (workflowId == null) {
      return cloudClient();
    }

    if (workflowId.startsWith("tc-")) {
      return cloudClient();
    }
    return onPremClient();
  }

  public WorkflowExecution start(String workflowId, Object ob) {
    String workflowId1 = "tc-" + workflowId;
    return getClient(workflowId1).newUntypedWorkflowStub(workflowId1).start(ob);
  }

  public void signalWorkflow(String workflowId, String signalName, Object... args) {
    getClient(workflowId).newUntypedWorkflowStub(workflowId).signal("signalName", args);
  }

  public Object queryWorkflow(String workflowId, String queryName, Object... args) {
    // TODO
    return null;
  }

  private WorkflowClient onPremClient() {
    // TODO implemento to prem
    // cache the object to prevent creating it anytime
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    return client;
  }

  private WorkflowClient cloudClient() {
    // TODO implemento to connect to cloud
    // cache the object to prevent creating it anytime
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    return client;
  }
}
