package io.temporal.samples.hello._6028;

class CGreeting extends AGreeting {

  private String key1InC;

  public CGreeting() {}

  public CGreeting(String key1InA, String key1InC) {
    super(key1InA);
    this.key1InC = key1InC;
  }

  public String getKey1InC() {
    return key1InC;
  }

  public void setKey1InC(String key1InC) {
    this.key1InC = key1InC;
  }
}
