package io.antmendoza.samples.Murex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StageB {

  static String buildWorkflowId(String workflowId) {
    return "stageB-" + workflowId;
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

      System.out.println("start  StageBRequest" + Workflow.getInfo().getRunId());

      Workflow.await(() -> verificationStageBRequest != null);

      if (verificationStageBRequest.isVerificationOk()) {
        return;
      }

      if (verificationStageBRequest.isRetryStage()) {
        System.out.println("continue as new   StageBRequest");

        Workflow.continueAsNew(new StageBRequest());
      }
    }

    @Override
    public void manualVerificationStageB(VerificationStageBRequest verificationStageBRequest) {
      this.verificationStageBRequest = verificationStageBRequest;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  class VerificationStageBRequest {
    public static final String RETRY_STAGE_A = "RETRY_STAGE_A";
    public static final String STATUS_OK = "STATUS_OK";
    public static final String STATUS_KO = "STATUS_KO";
    private String value;

    public VerificationStageBRequest() {}

    public VerificationStageBRequest(String value) {
      this.value = value;
    }

    @JsonIgnore
    public boolean isVerificationOk() {
      return this.value.equals(STATUS_OK);
    }

    @JsonIgnore
    public boolean isRetryStage() {
      return this.value.equals(STATUS_KO);
    }
  }
}
