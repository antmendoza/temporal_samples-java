package io.antmendoza.samples.entityworkflowVsRequest;

import com.thedeanda.lorem.LoremIpsum;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

public class ProcessMessageImpl implements ProcessMessage {

  Logger log = Workflow.getLogger("ProcessMessageImpl");

  private final LoremIpsum lorem;

  public ProcessMessageImpl() {
    this.lorem = LoremIpsum.getInstance();
  }

  @Override
  public String process(String message) {
    log.info("Process message: " + message);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return lorem.getWords(2, 6);
  }
}
