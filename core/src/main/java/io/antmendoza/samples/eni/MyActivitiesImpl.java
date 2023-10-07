package io.antmendoza.samples.eni;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;

public class MyActivitiesImpl implements MyActivities {
  @Override
  public void activity1() {

    ExternalService.doWork("activity1", Activity.getExecutionContext().getInfo().getAttempt());

    ExternalService.getMyValue().setActivity1Completed(true);
  }

  @Override
  public void activity2() {

    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    ExternalService.doWork("activity2", attempt);

    if (!ExternalService.getMyValue().isActivity1Completed()) {
      throw ApplicationFailure.newFailure(
          "Activity 1 not completed, attempt=" + attempt, "my error");
    }

    ExternalService.getMyValue().setActivity2Completed(true);
  }

  @Override
  public void activity3() {

    while (true) {

      try {

        System.out.println("Geting value in activity 3");

        ExternalService.doWork("activity3", Activity.getExecutionContext().getInfo().getAttempt());

        if (ExternalService.getMyValue().isActivity2Completed()) {
          return;
        }

        Activity.getExecutionContext().heartbeat(null);
        ExternalService.sleepMs(1000);

      } catch (Exception e) {
        throw e;
      }
    }
  }
}
