package io.temporal.samples.retryonsignalinterceptor2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.function.Supplier;

public class HumanTask {
  @JsonIgnore private Supplier<Object> supplier;
  private String token;

  public HumanTask() {}

  public <T> HumanTask(Supplier<T> supplier, String token) {
    this.supplier = (Supplier<Object>) supplier;
    this.token = token;
  }

  @JsonIgnore
  public void start() {

    // activity execution
    Object result = supplier.get();
    System.out.println(result);
  }

  public String getToken() {
    return token;
  }
}
