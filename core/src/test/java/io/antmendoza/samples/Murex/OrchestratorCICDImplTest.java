package io.antmendoza.samples.Murex;

import static io.antmendoza.samples.Murex.StageB.VerificationStageBRequest.*;
import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import java.util.function.Predicate;
import org.junit.Rule;
import org.junit.Test;

public class OrchestratorCICDImplTest {

  // set to true if you want to run the test against a real server
  private final boolean useExternalService = true;

  @Rule public TestWorkflowRule testWorkflowRule = createTestRule().build();

  @Test(timeout = 2000)
  public void testExecuteTwoStagesAndSignalStages() {
    final String namespace = testWorkflowRule.getTestEnvironment().getNamespace();
    final String workflowId = "my-orchestrator-" + Math.random();
    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final OrchestratorCICD orchestratorCICD = createWorkflowStub(workflowId, workflowClient);

    final WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    final WorkflowStub workflowStubStageA =
        workflowClient.newUntypedWorkflowStub(StageA.buildWorkflowId(workflowId));

    // Wait for stageA to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowStubStageA.getExecution());

    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        workflowStubStageA.getExecution());

    WorkflowStub workflowStubStageB =
        workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId));

    // Wait for stageB to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowStubStageB.getExecution());

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBRequest(STATUS_OK));

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

  @Test(timeout = 4000)
  public void testRetryStageB() {
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final String workflowId = "my-orchestrator-" + Math.random();
    final OrchestratorCICD orchestratorCICD = createWorkflowStub(workflowId, workflowClient);

    final WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    final WorkflowStub workflowStubStageA =
        workflowClient.newUntypedWorkflowStub(StageA.buildWorkflowId(workflowId));

    // Wait for stageA to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowStubStageA.getExecution());
    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(workflowStubStageA.getExecution(), namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // Wait for stageB to start
    WorkflowStub workflowStubStageB =
        workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId));

    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowStubStageB.getExecution());

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBRequest(STATUS_KO));

    // wait stageB to change its status !running
    OrchestratorCICD.OrchestratorCICDImpl.StagesDescription.StageDescription stageBDescription =
        workflowClient
            .newWorkflowStub(OrchestratorCICD.class, workflowId)
            .stagesDescription()
            .getStageBDescription();

    waitUntilTrue(
        new Awaitable(
            new Awaitable.Condition() {
              @Override
              public boolean check() {

                Predicate<WorkflowExecutionInfo> filterWorkflowExecutionStageB =
                    l -> {
                      return l.getExecution()
                          .getWorkflowId()
                          .equals(StageB.buildWorkflowId(workflowId));
                    };

                WorkflowExecutionInfo firstStageBWorkflowExecution =
                    workflowClient
                        .getWorkflowServiceStubs()
                        .blockingStub()
                        .listOpenWorkflowExecutions(
                            ListOpenWorkflowExecutionsRequest.newBuilder()
                                .setNamespace(namespace)
                                .build())
                        .getExecutionsList()
                        .stream()
                        .filter(filterWorkflowExecutionStageB)
                        .findFirst()
                        .get();

                return firstStageBWorkflowExecution != null;
              }
            }));
    waitUntilTrue(
        new Awaitable(
            () -> {
              WorkflowExecutionStatus status =
                  describeWorkflowExecution(
                          WorkflowExecution.newBuilder()
                              .setWorkflowId(stageBDescription.getWorkflowId())
                              .setRunId(stageBDescription.getRunId())
                              .build(),
                          namespace)
                      .getWorkflowExecutionInfo()
                      .getStatus();

              return !WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING.equals(status);
            }));

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW,
        describeWorkflowExecution(
                WorkflowExecution.newBuilder()
                    .setWorkflowId(stageBDescription.getWorkflowId())
                    .setRunId(stageBDescription.getRunId())
                    .build(),
                namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // new child workflow is created
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        describeWorkflowExecution(
                WorkflowExecution.newBuilder()
                    .setWorkflowId(stageBDescription.getWorkflowId())
                    .build(),
                namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // signal de new child workflow
    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBRequest(STATUS_OK));

    // wait for main workflow to continueAsNew
    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  private OrchestratorCICD createWorkflowStub(String workflowId, WorkflowClient workflowClient) {
    final WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowId(workflowId)
            .build();
    final OrchestratorCICD orchestratorCICD =
        workflowClient.newWorkflowStub(OrchestratorCICD.class, options);
    return orchestratorCICD;
  }

  private void waitUntilExecutionIsInStatus(
      String namespace,
      WorkflowExecutionStatus workflowExecutionStatus,
      WorkflowExecution executionB) {
    waitUntilTrue(
        new Awaitable(
            () -> {
              return workflowExecutionStatus.equals(
                  describeWorkflowExecution(executionB, namespace)
                      .getWorkflowExecutionInfo()
                      .getStatus());
            }));
  }

  private void waitUntilTrue(Awaitable r) {
    r.returnWhenTrue();
  }

  private DescribeWorkflowExecutionResponse describeWorkflowExecution(
      WorkflowExecution execution, String namespace) {

    DescribeWorkflowExecutionRequest.Builder builder =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(namespace)
            // .setExecution(
            //   WorkflowExecution.newBuilder()
            //       .setWorkflowId(execution.getWorkflowId())
            //       .setRunId(execution.getRunId())
            //       .build())
            .setExecution(execution);
    DescribeWorkflowExecutionResponse result =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(builder.build());
    return result;
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
          .setUseExternalService(useExternalService)
          .setTarget("127.0.0.1:7233") // default 127.0.0.1:7233
          .setNamespace("default"); // default
    }

    return builder;
  }

  public static class Awaitable {

    private final Condition condition;

    private Awaitable(Condition condition) {
      this.condition = condition;
    }

    public <T> void returnWhenTrue() {
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
}
