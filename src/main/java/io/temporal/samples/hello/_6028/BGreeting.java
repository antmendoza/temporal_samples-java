package io.temporal.samples.hello._6028;

public class BGreeting extends AGreeting {

  private String key1InB;
  private CGreeting cGreeting;

  public BGreeting() {}

  public BGreeting(String key1InA, String key1InB, String key1InC) {
    super(key1InA);
    this.key1InB = key1InB;
    this.cGreeting = new CGreeting(key1InA, key1InC);
  }

  public String getKey1InB() {
    return key1InB;
  }

  public void setKey1InB(String key1InB) {
    this.key1InB = key1InB;
  }

  public CGreeting getcGreeting() {
    return cGreeting;
  }

  public void setcGreeting(CGreeting cGreeting) {
    this.cGreeting = cGreeting;
  }
}
