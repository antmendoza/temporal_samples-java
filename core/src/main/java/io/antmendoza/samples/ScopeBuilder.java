package io.antmendoza.samples;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.util.ImmutableMap;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.common.reporter.MicrometerClientStatsReporter;

public class ScopeBuilder {

  private static Scope scope;

  public static Scope getScope() {

    if (ScopeBuilder.scope == null) {

      // Set up prometheus registry and stats reported
      PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
      // Set up a new scope, report every 1 second
      scope =
          new RootScopeBuilder()
              // shows how to set custom tags
              .tags(
                  ImmutableMap.of(
                      "workerCustomTag1",
                      "workerCustomTag1Value",
                      "workerCustomTag2",
                      "workerCustomTag2Value"))
              .reporter(new MicrometerClientStatsReporter(registry))
              .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
      // Start the prometheus scrape endpoint
      HttpServer scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, 8078);
      // Stopping the worker will stop the http server that exposes the
      // scrape endpoint.
      Runtime.getRuntime().addShutdownHook(new Thread(() -> scrapeEndpoint.stop(1)));
    }
    return scope;
  }
}
