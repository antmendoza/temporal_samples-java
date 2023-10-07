package io.antmendoza.samples.Murex;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StageB {

  static String BuildWorkflowId(String workflowId) {
    return "stageB" + workflowId;
  }

  @WorkflowMethod
  void run(StageBRequest stageBRequest);

  @SignalMethod
  void manualVerificationStageB(VerificationStageBRequest verificationStageBRequest);

  class StageBRequest {
    public final String id = "";
  }

  class StageBImpl implements StageB {
    private VerificationStageBRequest verificationStageBRequest;

    @Override
    public void run(StageBRequest stageBRequest) {

      Workflow.await(() -> verificationStageBRequest != null);
    }

    @Override
    public void manualVerificationStageB(VerificationStageBRequest verificationStageBRequest) {
      this.verificationStageBRequest = verificationStageBRequest;
    }
  }

  class VerificationStageBRequest {
    public final String id = "";
  }
}
