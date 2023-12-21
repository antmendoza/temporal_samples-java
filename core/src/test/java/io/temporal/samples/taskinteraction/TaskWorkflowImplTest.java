package io.temporal.samples.taskteraction;

import static org.junit.Assert.assertEquals;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.worker.WorkerFactoryOptions;
import java.time.Duration;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class TaskWorkflowImplTest {

  private final TaskWorkflowImplTest.TestActivityImpl testActivity =
      new TaskWorkflowImplTest.TestActivityImpl();

  @Rule
  public TestWorkflowRule testWorkflowRule =
      createTestEnv(
              TestWorkflowRule.newBuilder()
                  .setWorkerFactoryOptions(
                      WorkerFactoryOptions.newBuilder()
                          // .setWorkerInterceptors(new RetryOnSignalWorkerInterceptor())
                          .validateAndBuildWithDefaults())
                  .setWorkflowTypes(TaskWorkflowImpl.class)
                  .setActivityImplementations(testActivity))
          .build();

  @Test
  public void testRetryThenFail() {

    TaskWorkflow workflow = testWorkflowRule.newWorkflowStub(TaskWorkflow.class);
    WorkflowExecution execution = WorkflowClient.start(workflow::execute);

    // Get stub to the dynamically registered interface
    TaskClient client =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(TaskClient.class, execution.getWorkflowId());

    testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(1));

    final List<Task> tasks_2 = client.getPendingTasks();
    assertEquals(2, tasks_2.size());
    completeTasks(client, tasks_2);

    final List<Task> tasks_1 = client.getPendingTasks();
    assertEquals(1, tasks_1.size());
    completeTasks(client, tasks_1);

    WorkflowStub untyped =
        testWorkflowRule.getWorkflowClient().newUntypedWorkflowStub(execution.getWorkflowId());
    untyped.getResult(Void.class);
  }

  static class TestActivityImpl implements TaskActivity {

    @Override
    public String createTask(String task) {
      return "done";
    }
  }

  private static void completeTasks(TaskClient client, List<Task> tasks_1) {
    tasks_1.forEach(
        t -> {
          client.changeStatus(
              new TaskService.TaskRequest(TaskService.STATUS.PENDING, "Submitting ", t.getToken()));
          client.changeStatus(
              new TaskService.TaskRequest(TaskService.STATUS.STARTED, "Submitting ", t.getToken()));
          client.changeStatus(
              new TaskService.TaskRequest(
                  TaskService.STATUS.COMPLETED, "Submitting ", t.getToken()));
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
