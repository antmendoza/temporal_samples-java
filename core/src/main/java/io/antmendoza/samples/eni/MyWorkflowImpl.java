package io.antmendoza.samples.eni;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MyWorkflowImpl implements MyWorkflow {
  private RetryOptions retryOptions =
      RetryOptions.newBuilder()
          .setBackoffCoefficient(1)
          .setInitialInterval(Duration.ofSeconds(5))
          .build();

  private final MyActivities activities =
      Workflow.newActivityStub(
          MyActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(20))
              .setRetryOptions(retryOptions)
              .build());

  @Override
  public void execute(boolean parallel) {

    if (parallel) {

      final List<Promise<Void>> promises = new ArrayList<>();

      /**
       * each worker, is able to execute a limited number of activities and workflow task
       * concurrently, now might not be a problem and for specific use-cases might be ok, but if you
       * have 200 running workflows and each workflow create 3 activities that are occupying one
       * slot, those are 900 slots. Again, maybe this is not a problem now, but I don't know in the
       * future if it will be.
       */
      promises.add(Async.procedure(activities::activity1));
      promises.add(Async.procedure(activities::activity2));
      promises.add(Async.procedure(activities::activity3));

      // What happen if one activity fails?
      // when do you consider an activity failed?
      //  - cancel activities
      Promise.allOf(promises);

      for (Promise result : promises) {
        try {
          result.get();
        } catch (ApplicationFailure e) {
          // cancel context?
          // Run compensation,
        }
      }
    }

    // Sequential
    activities.activity1();
    activities.activity2();
    activities.activity3();
  }
}
