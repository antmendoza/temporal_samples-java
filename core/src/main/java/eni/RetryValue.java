package eni;

public class RetryValue {

  public static MyValue get(String activityName, int attempt) {
    return new MyValue();
  }

  public static class MyValue {}
}
