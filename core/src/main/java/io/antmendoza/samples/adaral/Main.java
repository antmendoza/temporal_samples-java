package io.antmendoza.samples.adaral;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

  public static void main(String[] args) {

    // REVIEW!! this will cause a NDE since the value is going to be different during replay,
    // use Workflow.currentTimeMillis() instead that will return the same value for the first run
    // and during replay
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime start = new Task("2023-07-13 07:35:52").getDequeueTime();

    // Converting all DateTime to millis to compare it correctly given potential timezone difference
    long startTimeMillis = ZonedDateTime.of(start, ZoneOffset.UTC).toInstant().toEpochMilli();

    // REVIEW!!  Maybe you should consider using ISO-8601 when passing this date as an input
    // getDequeueTime,
    // I think the followint statement could return a different value depending on where the workers
    // are running
    long nowTimeMillis = ZonedDateTime.of(now, ZoneId.systemDefault()).toInstant().toEpochMilli();

    System.out.println("startTimeMillis : " + startTimeMillis);
    System.out.println("nowTimeMillis : " + nowTimeMillis);
  }

  private static class Task {
    private String s;

    public Task(String s) {
      this.s = s;
    }

    public LocalDateTime getDequeueTime() {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.parse(s, formatter);
    }
  }
}
