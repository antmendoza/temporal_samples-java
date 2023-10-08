package io.antmendoza.samples.Murex;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.slf4j.Logger;

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
    private final Logger log = Workflow.getLogger("StageAImpl");

    private VerificationStageARequest verificationStageARequest;

    @Override
    public void run(StageARequest request) {

      log.info("Starting with runId:" + Workflow.getInfo().getRunId());

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
