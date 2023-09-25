package io.eni;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MyActivities {

  @ActivityMethod
  void activity1();

  @ActivityMethod
  void activity2();

  @ActivityMethod
  void activity3();
}
