package io.eni;

import io.temporal.activity.Activity;

public class MyActivitiesImpl implements MyActivities {
  @Override
  public void activity1() {

    RetryValue.get("activity1", Activity.getExecutionContext().getInfo().getAttempt());
  }

  @Override
  public void activity2() {
    RetryValue.get("activity2", Activity.getExecutionContext().getInfo().getAttempt());
  }

  @Override
  public void activity3() {
    RetryValue.get("activity3", Activity.getExecutionContext().getInfo().getAttempt());
  }
}
