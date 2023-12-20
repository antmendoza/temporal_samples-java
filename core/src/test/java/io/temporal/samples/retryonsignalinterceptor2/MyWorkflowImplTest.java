package io.temporal.samples.retryonsignalinterceptor2;

import static org.junit.jupiter.api.Assertions.*;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class MyWorkflowImplTest {

  static class TestActivityImpl implements MyActivity {

    final AtomicInteger count = new AtomicInteger();

    @Override
    public void execute() {}
  }

  private final MyWorkflowImplTest.TestActivityImpl testActivity =
      new MyWorkflowImplTest.TestActivityImpl();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setUseExternalService(true)
          .setTarget("127.0.0.1:7233") // default 127.0.0.1:7233
          .setNamespace("default") // default
          .setWorkerFactoryOptions(
              WorkerFactoryOptions.newBuilder()
                  // .setWorkerInterceptors(new RetryOnSignalWorkerInterceptor())
                  .validateAndBuildWithDefaults())
          .setWorkflowTypes(MyWorkflowImpl.class)
          .setActivityImplementations(testActivity)
          .build();

  @Test
  public void testRetryThenFail() {
    testActivity.count.set(0);
    TestWorkflowEnvironment testEnvironment = testWorkflowRule.getTestEnvironment();
    MyWorkflow workflow = testWorkflowRule.newWorkflowStub(MyWorkflow.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // Get stub to the dynamically registered interface
    HumanTaskClient client =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(HumanTaskClient.class, execution.getWorkflowId());

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(2));

    client.status(new HumanTaskService.UpdateTask(HumanTaskService.STATUS.PENDING));
    client.status(new HumanTaskService.UpdateTask(HumanTaskService.STATUS.STARTED));
    client.status(new HumanTaskService.UpdateTask(HumanTaskService.STATUS.COMPLETED));

    WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    untyped.getResult(Void.class);
  }
}
