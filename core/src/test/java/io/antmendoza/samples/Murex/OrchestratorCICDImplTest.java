package io.antmendoza.samples.Murex;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class OrchestratorCICDImplTest {

  // set to true if you want to run the test against a real server
  private final boolean useExternalService = true;

  @Rule public TestWorkflowRule testWorkflowRule = createTestRule().build();

  @Test(timeout = 2000)
  public void testExecuteTwoStagesAndSignalStages() {
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final String workflowId = "my-orchestrator-" + Math.random();
    final WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();

    final OrchestratorCICD orchestratorCICD =
        workflowClient.newWorkflowStub(OrchestratorCICD.class, options);

    final WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    final WorkflowStub workflowStubStageA =
        workflowClient.newUntypedWorkflowStub(StageA.BuildWorkflowId(workflowId));

    // Wait for stageA to start
    waitUntilTrue(
        new Awaitable(
            () -> {
              return WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING.equals(
                  describeWorkflowExecution(workflowStubStageA.getExecution(), namespace)
                      .getWorkflowExecutionInfo()
                      .getStatus());
            }));
    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(workflowStubStageA.getExecution(), namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    WorkflowStub workflowStubStageB =
        workflowClient.newUntypedWorkflowStub(StageB.BuildWorkflowId(workflowId));

    // Wait for stageB to start
    waitUntilTrue(
        new Awaitable(
            () -> {
              return WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING.equals(
                  describeWorkflowExecution(workflowStubStageB.getExecution(), namespace)
                      .getWorkflowExecutionInfo()
                      .getStatus());
            }));

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBRequest());

    // wait stageB to complete
    workflowStubStageB.getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(workflowStubStageB.getExecution(), namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // wait for main workflow to complete
    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  private void waitUntilTrue(Awaitable r) {
    r.returnWhenTrue();
  }

  private DescribeWorkflowExecutionResponse describeWorkflowExecution(
      WorkflowExecution execution, String namespace) {
    DescribeWorkflowExecutionRequest.Builder builder =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(namespace)
            .setExecution(execution);
    DescribeWorkflowExecutionResponse result =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(builder.build());
    return result;
  }

  public static class Awaitable {

    private final Condition condition;

    private Awaitable(Condition condition) {
      this.condition = condition;
    }

    public void returnWhenTrue() {
      while (true) {
        try {
          final boolean result = this.condition.check();
          if (result) {
            return;
          }
        } catch (Exception e) {
          // do nothing
        }

        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
    }

    interface Condition {
      boolean check();
    }
  }

  private TestWorkflowRule.Builder createTestRule() {
    TestWorkflowRule.Builder builder =
        TestWorkflowRule.newBuilder()
            .setWorkflowTypes(
                OrchestratorCICD.OrchestratorCICDImpl.class,
                StageB.StageBImpl.class,
                StageA.StageAImpl.class)
            .setDoNotStart(true);

    if (useExternalService) {
      builder
          .setUseExternalService(useExternalService) // to run the test against a "real" cluster
          .setTarget("127.0.0.1:7233") // default 127.0.0.1:7233
          .setNamespace("default"); // default
    }

    return builder;
  }
}
