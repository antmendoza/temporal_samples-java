package io.antmendoza.samples;

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import java.io.FileInputStream;
import java.io.InputStream;

public class SslContextBuilderProvider {

  private final TemporalProperties properties;

  public SslContextBuilderProvider() {
    this.properties = new TemporalProperties();
  }

  public SslContext getSslContext() {

    try {
      InputStream clientCert = new FileInputStream(properties.temporal_cert_location);
      InputStream clientKey = new FileInputStream(properties.temporal_key_location);
      return SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getTargetEndpoint() {
    String targetEndpoint = properties.temporal_target_endpoint;
    return targetEndpoint;
  }

  public String getNamespace() {
    // Your registered namespace.
    // String namespace = System.getenv("TEMPORAL_NAMESPACE");
    String namespace = properties.temporal_namespace;

    return namespace;
  }
}
