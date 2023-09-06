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

package io.antmendoza.samples.entityworkflowVsRequest._1;

import io.antmendoza.samples.entityworkflowVsRequest.ChatBot;
import io.antmendoza.samples.entityworkflowVsRequest.ServiceFactory;
import io.antmendoza.samples.entityworkflowVsRequest.worker.WorkerChatBotSslBuilder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class StarterAutometedExample {

  public static void main(String[] args) {

    WorkflowServiceStubs service = ServiceFactory.createService();

    // Start worker
    new WorkerChatBotSslBuilder().createWorkerChatBotSsl().start(service);

    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setNamespace(ServiceFactory.getNamespace()).build();

    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

    ChatBot.startAndCompleteWorkflow(client);

    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    System.exit(0);
  }
}
