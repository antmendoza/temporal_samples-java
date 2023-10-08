package io.antmendoza.samples.Murex;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.workflow.*;
import java.util.List;
import org.apache.commons.collections.ArrayStack;
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

  @QueryMethod
  OrchestratorCICDImpl.StagesDescription stagesDescription();

  @QueryMethod
  String currentStage();

  class OrchestratorCICDImpl implements OrchestratorCICD {

    private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

    private StageA stageA;
    private StageB stageB;
    private StagesDescription stagesDescription = new StagesDescription();

    private List<StageLog> stageLogs = new ArrayStack();

    @Override
    public void run(OrchestratorRequest request) {

      String workflowId = getWorkflowId();
      log.info("Starting workflow " + workflowId);

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
        stageLogs.add(new StageLog("STAGE_A_STARTED", Workflow.currentTimeMillis()));

        stagesDescription.setStageA(
            new StagesDescription.StageDescription(
                StageA.buildWorkflowId(workflowId), stageAExecution.getRunId()));
        // Wait for the stageA to complete
        Promise.allOf(resultStageA).get();
        stageLogs.add(new StageLog("STAGE_A_COMPLETED", Workflow.currentTimeMillis()));
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
        stageLogs.add(new StageLog("STAGE_B_STARTED", Workflow.currentTimeMillis()));
        stagesDescription.setStageB(
            new StagesDescription.StageDescription(
                StageB.buildWorkflowId(workflowId), stageBExecution.getRunId()));

        // Wait for the stageA to complete
        Promise.allOf(resultStageB).get();
        stageLogs.add(new StageLog("STAGE_B_COMPLETED", Workflow.currentTimeMillis()));
      }
    }

    private static String getWorkflowId() {
      return Workflow.getInfo().getWorkflowId();
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

    @Override
    public StagesDescription stagesDescription() {
      return this.stagesDescription;
    }

    @Override
    public String currentStage() {
      return stageLogs.get(stageLogs.size() - 1).getStage();
    }

    public static class StagesDescription {

      private StageDescription stageADescription;
      private StageDescription stageBDescription;

      public void setStageA(StageDescription stageDescription) {
        this.stageADescription = stageDescription;
      }

      public void setStageB(StageDescription stageDescription) {
        this.stageBDescription = stageDescription;
      }

      public StageDescription getStageADescription() {
        return stageADescription;
      }

      public StageDescription getStageBDescription() {
        return stageBDescription;
      }

      public static class StageDescription {
        private String workflowId;
        private String runId;

        public StageDescription() {}

        public StageDescription(String workflowId, String runId) {
          this.workflowId = workflowId;
          this.runId = runId;
        }

        public String getWorkflowId() {
          return workflowId;
        }

        public String getRunId() {
          return runId;
        }
      }
    }
  }

  class OrchestratorRequest {}

  class StageLog {
    private String stage;
    private long currentTimeMillis;

    public StageLog(String stage, long currentTimeMillis) {

      this.stage = stage;
      this.currentTimeMillis = currentTimeMillis;
    }

    public StageLog() {}

    public String getStage() {
      return stage;
    }

    public long getCurrentTimeMillis() {
      return currentTimeMillis;
    }

    @Override
    public String toString() {
      return "StageLog{"
          + "stage='"
          + stage
          + '\''
          + ", currentTimeMillis="
          + currentTimeMillis
          + '}';
    }
  }
}
