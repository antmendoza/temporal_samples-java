/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.antmendoza.samples.contextpropagator;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.Payload;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class HelloContextPropagator {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloActivityTaskQueue";

  // Define our workflow unique id
  static final String WORKFLOW_ID = "HelloActivityWorkflow";

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    {
      /*
       * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
       */
      WorkflowClient client =
          WorkflowClient.newInstance(
              service,
              WorkflowClientOptions.newBuilder()
                  .setContextPropagators(Arrays.asList(new WorkerContextPropagator()))
                  .build());

      // MDC.put("X-", "testing123");

      /*
       * Define the workflow factory. It is used to create workflow workers for a specific task queue.
       */
      WorkerFactory factory = WorkerFactory.newInstance(client);

      /*
       * Define the workflow worker. Workflow workers listen to a defined task queue and process
       * workflows and activities.
       */
      Worker worker = factory.newWorker(TASK_QUEUE);

      /*
       * Register our workflow implementation with the worker.
       * Workflow implementations must be known to the worker at runtime in
       * order to dispatch workflow tasks.
       */
      worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

      /*
       * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
       * the Activity Type is a shared instance.
       */
      worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

      /*
       * Start all the workers registered for a specific task queue.
       * The started workers then start polling for workflows and activities.
       */
      factory.start();
    }
    {
      WorkflowClient client =
          WorkflowClient.newInstance(
              service,
              WorkflowClientOptions.newBuilder()
                  // .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
                  .build());

      // Create the workflow client stub. It is used to start our workflow execution.
      GreetingWorkflow workflow =
          client.newWorkflowStub(
              GreetingWorkflow.class,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(WORKFLOW_ID)
                  .setContextPropagators(Collections.singletonList(new WF1ContextPropagator()))
                  .setTaskQueue(TASK_QUEUE)
                  .build());

      String greeting = workflow.getGreeting("World");

      System.out.println(greeting);
    }

    {
      WorkflowClient client =
          WorkflowClient.newInstance(
              service,
              WorkflowClientOptions.newBuilder()
                  // .setContextPropagators(Collections.singletonList(new TestContextPropagator()))
                  .build());

      // Create the workflow client stub. It is used to start our workflow execution.
      GreetingWorkflow workflow =
          client.newWorkflowStub(
              GreetingWorkflow.class,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(WORKFLOW_ID)
                  .setContextPropagators(Collections.singletonList(new WF2ContextPropagator()))
                  .setTaskQueue(TASK_QUEUE)
                  .build());

      String greeting = workflow.getGreeting("World");

      System.out.println(greeting);
    }

    System.exit(0);
  }

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {

    @ActivityMethod(name = "greet")
    String composeGreeting(String greeting, String name);
  }

  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities activities =
        Workflow.newActivityStub(
            GreetingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public String getGreeting(String name) {

      MDC.put("test", "123456updated");

      // This is a blocking call that returns only after the activity has completed.
      String hello = activities.composeGreeting("Hello", name);

      MDC.get("WorkflowType");
      return MDC.get("myKey");
    }
  }

  /** Simple activity implementation, that concatenates two strings. */
  static class GreetingActivitiesImpl implements GreetingActivities {
    private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

    @Override
    public String composeGreeting(String greeting, String name) {
      log.info("Composing greeting...");
      return greeting + " " + name + "!";
    }
  }

  public static class WF1ContextPropagator implements ContextPropagator {

    public WF1ContextPropagator() {
      MDC.put("myKey", getName());
    }

    public String getName() {
      return this.getClass().getName();
    }

    public Object getCurrentContext() {

      Map<String, String> context = new HashMap<>();
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      Map<String, String> copyOfContextMap = contextMap == null ? new HashMap<>() : contextMap;

      for (Map.Entry<String, String> entry : copyOfContextMap.entrySet()) {
        // if (entry.getKey().startsWith("X-")) {
        String key = entry.getKey();
        String value = entry.getValue();
        // System.out.println("getCurrentContext >>> " + key + " " + value);

        context.put(key, value);
        // }
      }
      return context;
    }

    public void setCurrentContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {

        String key = entry.getKey();
        String value = entry.getValue();
        // System.out.println("setCurrentContext >>> " + key + " " + value);

        MDC.put(key, value);
      }

      MDC.put("myKey", getName());
    }

    public Map<String, Payload> serializeContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      Map<String, Payload> serializedContext = new HashMap<>();
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {
        serializedContext.put(
            entry.getKey(), DataConverter.getDefaultInstance().toPayload(entry.getValue()).get());
      }
      return serializedContext;
    }

    public Object deserializeContext(Map<String, Payload> context) {
      Map<String, String> contextMap = new HashMap<>();
      for (Map.Entry<String, Payload> entry : context.entrySet()) {
        contextMap.put(
            entry.getKey(),
            DataConverter.getDefaultInstance()
                .fromPayload(entry.getValue(), String.class, String.class));
      }
      return contextMap;
    }
  }

  public static class WF2ContextPropagator implements ContextPropagator {

    public WF2ContextPropagator() {
      MDC.put("myKey", getName());
    }

    public String getName() {
      return this.getClass().getName();
    }

    public Object getCurrentContext() {

      Map<String, String> context = new HashMap<>();
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      Map<String, String> copyOfContextMap = contextMap == null ? new HashMap<>() : contextMap;

      for (Map.Entry<String, String> entry : copyOfContextMap.entrySet()) {
        // if (entry.getKey().startsWith("X-")) {
        String key = entry.getKey();
        String value = entry.getValue();
        // System.out.println("getCurrentContext >>> " + key + " " + value);

        context.put(key, value);
        // }
      }
      return context;
    }

    public void setCurrentContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {

        String key = entry.getKey();
        String value = entry.getValue();

        // System.out.println("setCurrentContext >>> " + key + " " + value);

        MDC.put(key, value);
      }
    }

    public Map<String, Payload> serializeContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      Map<String, Payload> serializedContext = new HashMap<>();
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {
        serializedContext.put(
            entry.getKey(), DataConverter.getDefaultInstance().toPayload(entry.getValue()).get());
      }
      return serializedContext;
    }

    public Object deserializeContext(Map<String, Payload> context) {
      Map<String, String> contextMap = new HashMap<>();
      for (Map.Entry<String, Payload> entry : context.entrySet()) {
        contextMap.put(
            entry.getKey(),
            DataConverter.getDefaultInstance()
                .fromPayload(entry.getValue(), String.class, String.class));
      }
      return contextMap;
    }
  }

  public static class WorkerContextPropagator implements ContextPropagator {

    public String getName() {
      return this.getClass().getName();
    }

    public Object getCurrentContext() {

      Map<String, String> context = new HashMap<>();
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      Map<String, String> copyOfContextMap = contextMap == null ? new HashMap<>() : contextMap;

      for (Map.Entry<String, String> entry : copyOfContextMap.entrySet()) {
        // if (entry.getKey().startsWith("X-")) {
        String key = entry.getKey();
        String value = entry.getValue();
        // System.out.println("getCurrentContext >>> " + key + " " + value);

        context.put(key, value);
        // }
      }
      return context;
    }

    public void setCurrentContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {

        String key = entry.getKey();
        String value = entry.getValue();

        // System.out.println("setCurrentContext >>> " + key + " " + value);

        MDC.put(key, value);
      }
    }

    public Map<String, Payload> serializeContext(Object context) {
      Map<String, String> contextMap = (Map<String, String>) context;
      Map<String, Payload> serializedContext = new HashMap<>();
      for (Map.Entry<String, String> entry : contextMap.entrySet()) {
        serializedContext.put(
            entry.getKey(), DataConverter.getDefaultInstance().toPayload(entry.getValue()).get());
      }
      return serializedContext;
    }

    public Object deserializeContext(Map<String, Payload> context) {
      Map<String, String> contextMap = new HashMap<>();
      for (Map.Entry<String, Payload> entry : context.entrySet()) {
        contextMap.put(
            entry.getKey(),
            DataConverter.getDefaultInstance()
                .fromPayload(entry.getValue(), String.class, String.class));
      }
      return contextMap;
    }
  }
}
