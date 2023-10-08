package io.antmendoza.samples.Murex;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.workflow.*;
import org.slf4j.Logger;

@WorkflowInterface
public interface OrchestratorCICD {

  @WorkflowMethod
  void run(OrchestratorRequest request);

  // We use the main workflow as interface for the rest of the workflows / child workflows.
  // there is nothing wrong signaling childworkflows directly
  @SignalMethod
  void manualVerificationStageA(StageA.VerificationStageARequest request);

  @SignalMethod
  void manualVerificationStageB(StageB.VerificationStageBRequest verificationStageBRequest);

  class OrchestratorCICDImpl implements OrchestratorCICD {

    private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

    private StageA stageA;
    private StageB stageB;

    private static String getWorkflowId() {
      return Workflow.getInfo().getWorkflowId();
    }

    @Override
    public void run(OrchestratorRequest request) {

      String workflowId = getWorkflowId();
      log.info("Starting with runId:" + Workflow.getInfo().getRunId());

      {
        stageA =
            Workflow.newChildWorkflowStub(
                StageA.class,
                ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(StageA.buildWorkflowId(workflowId))
                    .build());
        final Promise<Void> resultStageA = Async.procedure(stageA::run, new StageA.StageARequest());
        final Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageA);

        // Wait for child to start
        WorkflowExecution stageAExecution = childExecution.get();

        // Wait for the stageA to complete
        Promise.allOf(resultStageA).get();
      }

      {
        stageB =
            Workflow.newChildWorkflowStub(
                StageB.class,
                ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(StageB.buildWorkflowId(workflowId))
                    .build());
        final Promise<Void> resultStageB = Async.procedure(stageB::run, new StageB.StageBRequest());
        final Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageB);

        // Wait for child to start
        final WorkflowExecution stageBExecution = childExecution.get();

        // Wait for the stageA to complete
        Promise.allOf(resultStageB).get();
      }
    }

    @Override
    public void manualVerificationStageA(StageA.VerificationStageARequest request) {

      Workflow.newExternalWorkflowStub(StageA.class, StageA.buildWorkflowId(getWorkflowId()))
          .manualVerificationStageA(request);
    }

    @Override
    public void manualVerificationStageB(
        StageB.VerificationStageBRequest verificationStageBRequest) {

      Workflow.newExternalWorkflowStub(StageB.class, StageB.buildWorkflowId(getWorkflowId()))
          .manualVerificationStageB(verificationStageBRequest);
    }
  }

  class OrchestratorRequest {}
}
