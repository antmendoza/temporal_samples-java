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

package io.antmendoza.samples.activityAsyncCompletion;

import io.temporal.activity.*;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

/** Sample Temporal Workflow Definition that demonstrates asynchronous Activity Execution */
public class ClientCompleteActivity {

  /**
   * With our Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws ExecutionException, InterruptedException {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow
     * Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    String token =
        "CiQwMmZmOWJmYi1jNDdjLTQ5NTktYmY3ZC0xYjI4YTdkMDYzMWISJEhlbGxvQXN5bmNBY3Rpdml0eUNvbXBsZXRpb25Xb3JrZmxvdxokZDlkNzBjZTYtMTViMi00MzFlLWFhMjMtOTg4NDY0MGMxMjQxIAUoATIkOTMyNjU2YTEtNDA2ZS0zNTQ2LWJkZGItMjQ0YWU1NTBmYzQxQg9Db21wb3NlR3JlZXRpbmdKCQgEEO6BgAIYAQ==";

    client.newActivityCompletionClient().complete(Base64.getDecoder().decode(token), "my result");

    // Wait for workflow execution to complete and display its results.
    System.exit(0);
  }
}
