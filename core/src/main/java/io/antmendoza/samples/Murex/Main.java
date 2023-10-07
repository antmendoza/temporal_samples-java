package io.antmendoza.samples.Murex;

import java.util.concurrent.CompletableFuture;

public class Main {

  public static void main(String[] args) {

    CompletableFuture.runAsync(
        () -> {
          Starter.main(args);
        });

    CompletableFuture.runAsync(
        () -> {
          Worker.main(args);
        });
  }
}
