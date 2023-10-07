package io.antmendoza.samples.Murex;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class OrchestratorCICDImplTest {

  // set to true if want to run the test against a real server
  private final boolean useExternalService = false;

  @Rule public TestWorkflowRule testWorkflowRule = createTestRule().build();

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

  @Test(timeout = 1000)
  public void testExecution() {

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();

    String workflowId = "my-orchestrator" + Math.random();
    final WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();

    OrchestratorCICD orchestratorCICD =
        workflowClient.newWorkflowStub(OrchestratorCICD.class, options);

    WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    //        testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(1));

    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test(timeout = 1000)
  public void testExecuteTwoStages() {

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();

    String workflowId = "my-orchestrator" + Math.random();
    final WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();

    OrchestratorCICD orchestratorCICD =
        workflowClient.newWorkflowStub(OrchestratorCICD.class, options);

    WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  @Test(timeout = 1000)
  public void testExecuteTwoStagesAndSignalFirstStage() {

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();

    String workflowId = "my-orchestrator" + Math.random();
    final WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();

    OrchestratorCICD orchestratorCICD =
        workflowClient.newWorkflowStub(OrchestratorCICD.class, options);

    WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBRequest());

    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
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
}
