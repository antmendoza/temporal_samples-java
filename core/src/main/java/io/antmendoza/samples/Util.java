package io.antmendoza.samples;

public class Util {

  public static void sleep(int sleepMillisecond) {
    try {
      Thread.sleep(sleepMillisecond);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
