package io.antmendoza.samples.entityworkflowVsRequest;

import io.antmendoza.samples.ScopeBuilder;
import io.antmendoza.samples.SslContextBuilderProvider;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

public class ServiceFactory {

  private static final boolean ssl = true;

  private static SslContextBuilderProvider sslContextBuilderProvider =
      new SslContextBuilderProvider();

  public static WorkflowServiceStubs createService() {

    if (ssl) {
      WorkflowServiceStubs service =
          WorkflowServiceStubs.newServiceStubs(
              WorkflowServiceStubsOptions.newBuilder()
                  // Add metrics scope to workflow service stub options
                  .setMetricsScope(ScopeBuilder.getScope())
                  .setSslContext(sslContextBuilderProvider.getSslContext())
                  .setTarget(sslContextBuilderProvider.getTargetEndpoint())
                  .build());
      return service;
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                // Add metrics scope to workflow service stub options
                .setMetricsScope(ScopeBuilder.getScope())
                .build());
    return service;
  }

  public static String getNamespace() {

    if (ssl) {
      return sslContextBuilderProvider.getNamespace();
    }

    return "default";
  }
}
