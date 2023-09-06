package io.antmendoza.samples.entityworkflowVsRequest;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ProcessMessage {

  public String process(String message);
}
