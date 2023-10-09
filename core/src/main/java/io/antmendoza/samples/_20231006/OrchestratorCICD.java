package io.antmendoza.samples._20231006;

import io.antmendoza.samples._20231006.StageB.StageBRequest;
import io.antmendoza.samples._20231006.StageB.StageBResult;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.ChildWorkflowFailure;
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
  void manualVerificationStageB(StageB.VerificationStageBStatus verificationStageBStatus);

  class OrchestratorCICDImpl implements OrchestratorCICD {

    private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

    private StageA stageA;
    private StageB stageB;

    private static String getWorkflowId() {
      return Workflow.getInfo().getWorkflowId();
    }

    private static boolean retryFromStageA(StageBResult stageBResult) {
      return stageBResult.getVerificationStageBStatus().retryFromStageA();
    }

    @Override
    public void run(OrchestratorRequest request) {

      String workflowId = getWorkflowId();
      log.info("Starting with runId:" + Workflow.getInfo().getRunId());

      StageBResult stageBResult;

      boolean iterate = true;
      while (iterate) {
        // TODO watch workflow history and continueAsNew when required
        iterate = false;

        stageA(workflowId);

        try {
          stageBResult = getStageBResult(workflowId);

          if (retryFromStageA(stageBResult)) {
            iterate = true;
          }

        } catch (ChildWorkflowFailure childWorkflowFailure) {
          if (childWorkflowFailure.getCause() instanceof CanceledFailure) {
            // TODO Child was cancelled,
            // To compensate either run saga implementation
            // or start a new workflow in abandon mode to let this one complete
          }
        }
      }
    }

    private StageBResult getStageBResult(String workflowId) {
      StageBResult stageBResult;
      stageB =
          Workflow.newChildWorkflowStub(
              StageB.class,
              ChildWorkflowOptions.newBuilder()
                  .setWorkflowId(StageB.buildWorkflowId(workflowId))
                  .build());
      final Promise<StageBResult> resultStageB = Async.function(stageB::run, new StageBRequest());
      final Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageB);

      // Wait for child to start
      childExecution.get();

      // Wait for the stageA to complete
      Promise.allOf(resultStageB).get();

      // TODO handle
      stageBResult = resultStageB.get();
      return stageBResult;
    }

    private void stageA(String workflowId) {
      stageA =
          Workflow.newChildWorkflowStub(
              StageA.class,
              ChildWorkflowOptions.newBuilder()
                  .setWorkflowId(StageA.buildWorkflowId(workflowId))
                  .build());
      final Promise<Void> resultStageA = Async.procedure(stageA::run, new StageA.StageARequest());
      final Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageA);

      // Wait for child to start
      childExecution.get();

      // Wait for the stageA to complete
      Promise.allOf(resultStageA).get();
    }

    @Override
    public void manualVerificationStageA(StageA.VerificationStageARequest request) {

      Workflow.newExternalWorkflowStub(StageA.class, StageA.buildWorkflowId(getWorkflowId()))
          .manualVerificationStageA(request);
    }

    @Override
    public void manualVerificationStageB(StageB.VerificationStageBStatus verificationStageBStatus) {

      Workflow.newExternalWorkflowStub(StageB.class, StageB.buildWorkflowId(getWorkflowId()))
          .manualVerificationStageB(verificationStageBStatus);
    }
  }

  class OrchestratorRequest {}
}
