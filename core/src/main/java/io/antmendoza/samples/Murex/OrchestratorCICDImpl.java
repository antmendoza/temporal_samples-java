package io.antmendoza.samples.Murex;

import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class OrchestratorCICDImpl implements OrchestratorCICD {

  private final Logger log = Workflow.getLogger("OrchestratorCICDImpl");

  @Override
  public void run(OrchestratorRequest request) {

    String workflowId = Workflow.getInfo().getWorkflowId();
    log.info("Starting workflow " + workflowId);
    final StageA stageA =
        Workflow.newChildWorkflowStub(StageA.class, ChildWorkflowOptions.newBuilder().build());

    // Start and wait for child workflow to complete
    stageA.run(new StageA.StageARequest());

    final StageB stageB =
        Workflow.newChildWorkflowStub(StageB.class, ChildWorkflowOptions.newBuilder().build());

    // Start and wait for child workflow to complete
    stageB.run(new StageB.StageBRequest());
  }
}
