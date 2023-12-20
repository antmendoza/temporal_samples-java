package io.temporal.samples.retryonsignalinterceptor2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class HumanTaskService {

  private final MyActivity activity =
      Workflow.newActivityStub(
          MyActivity.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              // disable server side retries. In most production applications the retries should be
              // done for a while before requiring an external operator signal.
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
              .build());
  private Boolean activityCompleted = false;

  public HumanTaskService() {

    Workflow.registerListener(
        new HumanTaskClient() {

          @Override
          public void status(UpdateTask task) {
            activityCompleted = task.isCompleted();
          }

          @Override
          public String getPendingActivitiesStatus() {
            return "";
          }
        });
  }

  public String execute() {

    activity.execute();
    Workflow.await(() -> this.activityCompleted);

    return "result from activityCompleted";
  }

  public enum STATUS {
    PENDING,

    STARTED,
    COMPLETED
  }

  public static class UpdateTask {

    private STATUS status;

    public UpdateTask() {}

    public UpdateTask(STATUS status) {
      this.status = status;
    }

    @JsonIgnore
    public boolean isCompleted() {
      return this.status == STATUS.COMPLETED;
    }
  }
}
