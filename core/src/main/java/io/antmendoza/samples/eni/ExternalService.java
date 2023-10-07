package io.antmendoza.samples.eni;

public class ExternalService {
  private static MyValue myValue = new MyValue();

  public static MyValue getMyValue() {
    return myValue;
  }

  public static void doWork(String activityName, int attempt) {

    if (activityName.equals("activity1")) {
      sleepMs(12000);
    }

    if (activityName.equals("activity2")) {
      sleepMs(400);
    }
  }

  public static void sleepMs(int l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static class MyValue {
    private boolean isActivity2Completed = false;
    private boolean isActivity1Completed = false;

    public boolean isActivity2Completed() {
      return isActivity2Completed;
    }

    public void setActivity2Completed(boolean activity2Completed) {
      isActivity2Completed = activity2Completed;
    }

    public boolean isActivity1Completed() {
      return isActivity1Completed;
    }

    public void setActivity1Completed(boolean activity1Completed) {
      isActivity1Completed = activity1Completed;
    }
  }
}
