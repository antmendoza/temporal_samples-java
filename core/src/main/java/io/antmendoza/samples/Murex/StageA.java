package io.antmendoza.samples.Murex;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StageA {

  static String buildWorkflowId(String workflowId) {
    return "stageA-" + workflowId;
  }

  @WorkflowMethod
  void run(StageARequest request);

  @SignalMethod
  void manualVerificationStageA(VerificationStageARequest verificationStageARequest);

  class StageARequest {
    public final String id = "";
  }

  class StageAImpl implements StageA {

    private VerificationStageARequest verificationStageARequest;

    @Override
    public void run(StageARequest request) {

      Workflow.await(() -> verificationStageARequest != null);
    }

    @Override
    public void manualVerificationStageA(VerificationStageARequest verificationStageARequest) {

      this.verificationStageARequest = verificationStageARequest;
    }
  }

  class VerificationStageARequest {
    public final String id = "";
  }
}
