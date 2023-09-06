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

package io.antmendoza.samples.entityworkflowVsRequest._3_per_request;

import io.antmendoza.samples.entityworkflowVsRequest.ServiceFactory;
import io.antmendoza.samples.entityworkflowVsRequest.worker.WorkerChatBotSslBuilder;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class StarterAutometedExample {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    WorkflowServiceStubs service = ServiceFactory.createService();

    // Start worker
    // int factor = 1; 1087 seconds = 17 minutes
    // int factor = 100; 80 seconds
    int factor = 1;
    new WorkerChatBotSslBuilder()
        .setWorkflowCacheSize(2 * factor) // 600
        .setMaxConcurrentWorkflowTaskExecutionSize(2 * factor) // 200
        .setMaxConcurrentActivityExecutionSize(2 * factor) // 200
        .setMaxConcurrentLocalActivityExecutionSize(1)
        .setMaxWorkflowThreadCount(6 * factor) // default 600
        .createWorkerChatBotSsl()
        .start(service);

    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setNamespace(ServiceFactory.getNamespace()).build();

    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    long begin = System.currentTimeMillis();

    // workflows in parallel
    int parallelism = 4000;
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    IntStream.rangeClosed(1, parallelism)
        .boxed()
        .parallel()
        .forEach(
            e -> {
              Runnable worker =
                  new Runnable() {
                    @Override
                    public void run() {
                      ChatBotRequest.startAndCompleteWorkflow(client);
                    }
                  };
              executor.execute(worker);
            });

    int executionsOpenWofkflowsCount;
    do {

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      executionsOpenWofkflowsCount =
          client
              .getWorkflowServiceStubs()
              .blockingStub()
              .listOpenWorkflowExecutions(
                  ListOpenWorkflowExecutionsRequest.newBuilder()
                      .setNamespace(ServiceFactory.getNamespace())
                      .build())
              .getExecutionsCount();

      System.out.println("Open workflows= " + executionsOpenWofkflowsCount);
    } while (executionsOpenWofkflowsCount > 0);

    // End time
    long end = System.currentTimeMillis();
    long time = end - begin;
    System.out.println();
    System.out.println("Time: " + time / 1000 + "  seconds");

    System.exit(0);
  }
}
