package io.antmendoza.samples.hello;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class HelloTestSleepAndContinueAsNewTest {

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloTestSleepAndContinueAsNew.GreetingWorkflowImpl.class)
          .setDoNotStart(true)
          // .setUseTimeskipping(false)
          .build();

  @Test
  public void testActivityImpl() {

    testWorkflowRule
        .getWorker()
        .registerActivitiesImplementations(
            new HelloTestSleepAndContinueAsNew.GreetingActivitiesImpl());
    testWorkflowRule.getTestEnvironment().start();

    // Get a workflow stub using the same task queue the worker uses.
    HelloTestSleepAndContinueAsNew.GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                HelloTestSleepAndContinueAsNew.GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                    .setWorkflowId("test")
                    .setTaskQueue(testWorkflowRule.getTaskQueue())
                    .build());

    // Execute a workflow waiting for it to complete.
    WorkflowExecution execution = WorkflowClient.start(workflow::getGreeting, "World");
    String namespace = testWorkflowRule.getTestEnvironment().getNamespace();

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(30));

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(1));

    assertEquals(
        WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW,
        describeWorkflowExecution(execution, namespace).getWorkflowExecutionInfo().getStatus());

    // String greeting =
    //     testWorkflowRule
    //         .getWorkflowClient()
    //         .newUntypedWorkflowStub(execution, Optional.empty())
    //         .getResult(String.class);

    // assertEquals("Hello World!", greeting);

    testWorkflowRule.getTestEnvironment().shutdown();
  }

  private DescribeWorkflowExecutionResponse describeWorkflowExecution(
      WorkflowExecution execution, String namespace) {
    DescribeWorkflowExecutionRequest.Builder builder =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(namespace)
            .setExecution(execution);
    DescribeWorkflowExecutionResponse result =
        testWorkflowRule
            .getTestEnvironment()
            .getWorkflowServiceStubs()
            .blockingStub()
            .describeWorkflowExecution(builder.build());
    return result;
  }
}
