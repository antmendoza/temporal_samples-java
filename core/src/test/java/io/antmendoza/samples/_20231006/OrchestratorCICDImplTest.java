package io.antmendoza.samples._20231006;

import static io.antmendoza.samples._20231006.StageB.VerificationStageBStatus.*;
import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import org.junit.*;

public class OrchestratorCICDImplTest {

  private static TestUtilInterceptorTracker testUtilInterceptorTracker =
      new TestUtilInterceptorTracker();

  // set to true if you want to run the test against a real server
  private final boolean useExternalService = true;
  @Rule public TestWorkflowRule testWorkflowRule = createTestRule().build();

  @After
  public void after() {
    testWorkflowRule.getTestEnvironment().shutdown();
    testUtilInterceptorTracker = new TestUtilInterceptorTracker();
  }

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

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBStatus(STATUS_OK));

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
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageA.class.getSimpleName(), 1)));

    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(workflowStubStageA.getExecution(), namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // Wait for stageB to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId)).getExecution());

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBStatus(STATUS_KO));

    // wait stageB to change its status to continueAsNew
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasContinuedAsNewTimes(
                    StageB.class.getSimpleName(), 1)));

    // new stageB is created
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageB.class.getSimpleName(), 2)));

    // signal stageB to complete
    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBStatus(STATUS_OK));

    // wait for main workflow to complete
    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());
  }

  @Test(timeout = 4000)
  public void testRetryStageAFromStageB() {
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final String workflowId = "my-orchestrator-" + Math.random();
    final OrchestratorCICD orchestratorCICD = createWorkflowStub(workflowId, workflowClient);

    final WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    final WorkflowStub workflowStubStageA =
        workflowClient.newUntypedWorkflowStub(StageA.buildWorkflowId(workflowId));

    // Wait for stageA to start
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageA.class.getSimpleName(), 1)));
    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(workflowStubStageA.getExecution(), namespace)
            .getWorkflowExecutionInfo()
            .getStatus());

    // Wait for stageB to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId)).getExecution());

    orchestratorCICD.manualVerificationStageB(
        new StageB.VerificationStageBStatus(RETRY_FROM_STAGE_A));

    // we expect another workflow type stageA
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageA.class.getSimpleName(), 2)));

    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageB.class.getSimpleName(), 2)));

    orchestratorCICD.manualVerificationStageB(new StageB.VerificationStageBStatus(STATUS_OK));

    // wait for main workflow to complete
    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());
  }

  @Test(timeout = 4000)
  public void testCancelStageB() {
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    testWorkflowRule.getTestEnvironment().start();

    final WorkflowClient workflowClient = testWorkflowRule.getWorkflowClient();
    final String workflowId = "my-orchestrator-" + Math.random();
    final OrchestratorCICD orchestratorCICD = createWorkflowStub(workflowId, workflowClient);

    final WorkflowExecution execution = WorkflowClient.start(orchestratorCICD::run, null);

    final WorkflowStub workflowStubStageA =
        workflowClient.newUntypedWorkflowStub(StageA.buildWorkflowId(workflowId));

    // Wait for stageA to start
    waitUntilTrue(
        new Awaitable(
            () ->
                testUtilInterceptorTracker.hasNewWorkflowInvocationTimes(
                    StageA.class.getSimpleName(), 1)));
    orchestratorCICD.manualVerificationStageA(new StageA.VerificationStageARequest());

    // wait stageA to complete
    workflowStubStageA.getResult(Void.class);

    // Wait for stageB to start
    waitUntilExecutionIsInStatus(
        namespace,
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId)).getExecution());

    workflowClient.newUntypedWorkflowStub(StageB.buildWorkflowId(workflowId)).cancel();

    // wait for main workflow to complete
    workflowClient.newUntypedWorkflowStub(workflowId).getResult(Void.class);
    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());
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
            .setWorkerFactoryOptions(
                WorkerFactoryOptions.newBuilder()
                    .setWorkerInterceptors(
                        new TestUtilWorkerInterceptor(testUtilInterceptorTracker))
                    .build())
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
