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

package io.temporal.samples.hello;

import io.temporal.api.common.v1.WorkflowType;
import io.temporal.api.schedule.v1.IntervalSpec;
import io.temporal.api.schedule.v1.Schedule;
import io.temporal.api.schedule.v1.ScheduleAction;
import io.temporal.api.schedule.v1.ScheduleSpec;
import io.temporal.api.taskqueue.v1.TaskQueue;
import io.temporal.api.workflow.v1.NewWorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.CreateScheduleRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class HelloSchedule {

  public static void main(String[] args) {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    ScheduleSpec.Builder interval =
        ScheduleSpec.newBuilder()
            .addInterval(
                IntervalSpec.newBuilder()
                    .setInterval(com.google.protobuf.Duration.newBuilder().setSeconds(5000).build())
                    .build());
    NewWorkflowExecutionInfo workflowExecutionInfo =
        NewWorkflowExecutionInfo.newBuilder()
            .setWorkflowType(WorkflowType.newBuilder().setName("MyWorkflow").build())
            .setTaskQueue(TaskQueue.newBuilder().setName("taskqueue").build())
            .build();
    client
        .getWorkflowServiceStubs()
        .blockingStub()
        .createSchedule(
            CreateScheduleRequest.newBuilder()
                .setScheduleId("scheduleI-1234d")
                .setRequestId("my-request-1234")
                .setNamespace("default")
                .setSchedule(
                    Schedule.newBuilder()
                        .setSpec(interval.build())
                        .setAction(
                            ScheduleAction.newBuilder()
                                .setStartWorkflow(workflowExecutionInfo)
                                .build())
                        .build())
                .build());
  }
}
