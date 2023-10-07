package io.antmendoza.samples.Murex;

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

  class OrchestratorCICDImpl implements OrchestratorCICD {

    private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

    private StageA stageA;

    @Override
    public void run(OrchestratorRequest request) {

      String workflowId = Workflow.getInfo().getWorkflowId();
      log.info("Starting workflow " + workflowId);
      stageA =
          Workflow.newChildWorkflowStub(StageA.class, ChildWorkflowOptions.newBuilder().build());

      // Start and wait for child workflow to complete
      stageA.run(new StageA.StageARequest());

      final StageB stageB =
          Workflow.newChildWorkflowStub(StageB.class, ChildWorkflowOptions.newBuilder().build());

      // Start and wait for child workflow to complete
      stageB.run(new StageB.StageBRequest());
    }

    @Override
    public void manualVerificationStageA(StageA.VerificationStageARequest request) {

      stageA.manualVerificationStageA(request);
    }
  }
}
