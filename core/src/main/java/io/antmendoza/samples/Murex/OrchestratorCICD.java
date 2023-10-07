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

  @QueryMethod
  OrchestratorCICDImpl.StagesDescription stagesDescription();

  class OrchestratorCICDImpl implements OrchestratorCICD {

    private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

    private StageA stageA;
    private StageB stageB;
    private StagesDescription stagesDescription = new StagesDescription();

    @Override
    public void run(OrchestratorRequest request) {

      String workflowId = getWorkflowId();
      log.info("Starting workflow " + workflowId);

      {
        stageA =
            Workflow.newChildWorkflowStub(
                StageA.class,
                ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(StageA.BuildWorkflowId(workflowId))
                    .build());
        Promise<Void> resultStageA = Async.procedure(stageA::run, new StageA.StageARequest());
        Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageA);

        // Wait for child to start
        WorkflowExecution stageAExecution = childExecution.get();
        stagesDescription.setStageA(
            new StagesDescription.StageDescription(
                StageA.BuildWorkflowId(workflowId), stageAExecution.getRunId()));
        // Wait for the stageA to complete
        Promise.allOf(resultStageA).get();
      }

      {
        stageB =
            Workflow.newChildWorkflowStub(
                StageB.class,
                ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(StageB.BuildWorkflowId(workflowId))
                    .build());
        Promise<Void> resultStageB = Async.procedure(stageB::run, new StageB.StageBRequest());
        Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(stageB);

        // Wait for child to start
        WorkflowExecution stageBExecution = childExecution.get();
        stagesDescription.setStageB(
            new StagesDescription.StageDescription(
                StageB.BuildWorkflowId(workflowId), stageBExecution.getRunId()));

        // Wait for the stageA to complete
        Promise.allOf(resultStageB).get();
      }
    }

    private static String getWorkflowId() {
      return Workflow.getInfo().getWorkflowId();
    }

    @Override
    public void manualVerificationStageA(StageA.VerificationStageARequest request) {

      Workflow.newExternalWorkflowStub(StageA.class, StageA.BuildWorkflowId(getWorkflowId()))
          .manualVerificationStageA(request);
    }

    @Override
    public void manualVerificationStageB(
        StageB.VerificationStageBRequest verificationStageBRequest) {

      Workflow.newExternalWorkflowStub(StageB.class, StageB.BuildWorkflowId(getWorkflowId()))
          .manualVerificationStageB(verificationStageBRequest);
    }

    @Override
    public StagesDescription stagesDescription() {
      return this.stagesDescription;
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
}
