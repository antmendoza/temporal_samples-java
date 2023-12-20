package io.temporal.samples.humaninteraction;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class MyWorkflowImplTest {

  private final MyWorkflowImplTest.TestActivityImpl testActivity =
      new MyWorkflowImplTest.TestActivityImpl();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      createTestEnv(
              TestWorkflowRule.newBuilder()
                  .setWorkerFactoryOptions(
                      WorkerFactoryOptions.newBuilder()
                          // .setWorkerInterceptors(new RetryOnSignalWorkerInterceptor())
                          .validateAndBuildWithDefaults())
                  .setWorkflowTypes(MyWorkflowImpl.class)
                  .setActivityImplementations(testActivity))
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

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(1));

    final List<HumanTask> humanTasks_2 = client.getPendingTasks();
    assertEquals(2, humanTasks_2.size());
    completeTasks(client, humanTasks_2);

    final List<HumanTask> humanTasks_1 = client.getPendingTasks();
    assertEquals(1, humanTasks_1.size());
    completeTasks(client, humanTasks_1);

    WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    untyped.getResult(Void.class);
  }

  static class TestActivityImpl implements MyActivity {

    final AtomicInteger count = new AtomicInteger();

    @Override
    public String execute() {
      return "done";
    }
  }

  private static void completeTasks(HumanTaskClient client, List<HumanTask> humanTasks_1) {
    humanTasks_1.forEach(
        t -> {
          client.changeStatus(
              new HumanTaskService.TaskRequest(HumanTaskService.STATUS.PENDING, t.getToken()));
          client.changeStatus(
              new HumanTaskService.TaskRequest(HumanTaskService.STATUS.STARTED, t.getToken()));
          client.changeStatus(
              new HumanTaskService.TaskRequest(HumanTaskService.STATUS.COMPLETED, t.getToken()));
        });
  }

  private TestWorkflowRule.Builder createTestEnv(TestWorkflowRule.Builder builder) {

    if (Boolean.parseBoolean(System.getenv("TEST_LOCALHOST"))) {

      TestWorkflowRule.Builder result = builder;
      builder
          .setUseExternalService(true)
          .setTarget("127.0.0.1:7233") // default 127.0.0.1:7233
          .setNamespace("default");
      // default
      return result;
    }
    return builder;
  }
}
