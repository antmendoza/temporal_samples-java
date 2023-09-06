package io.antmendoza.samples.hello;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Optional;

public class HelloWorkflowRetry {

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

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);

    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    worker.registerActivitiesImplementations(new GreetingActivityInWorkflowRetryImpl());

    factory.start();

    // Create the workflow client stub. It is used to start our workflow execution.
    GreetingWorkflowRetry workflow =
        client.newWorkflowStub(
            GreetingWorkflowRetry.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    /*
     * Execute our workflow and wait for it to complete. The call to our getGreeting method is
     * synchronous.
     *
     * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
     * without waiting synchronously for its result.
     */

    WorkflowClient.start(workflow::getGreeting);

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    workflow.signal();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    // Display workflow execution results
    // System.out.println(greeting);
    System.exit(0);
  }

  @WorkflowInterface
  public interface GreetingWorkflowRetry {

    @WorkflowMethod
    String getGreeting();

    @SignalMethod
    void signal();
  }

  @ActivityInterface
  public interface GreetingActivityInWorkflowRetry {

    // Define your activity method which can be called during workflow execution
    @ActivityMethod(name = "greet")
    String sendCommand(String greeting, String name);
  }

  public static class GreetingActivityInWorkflowRetryImpl
      implements HelloWorkflowRetry.GreetingActivityInWorkflowRetry {
    @Override
    public String sendCommand(String greeting, String name) {
      return null;
    }
  }

  // Define the workflow implementation which implements our getGreeting workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflowRetry {

    private final GreetingActivityInWorkflowRetry activities =
        Workflow.newActivityStub(
            GreetingActivityInWorkflowRetry.class,
            ActivityOptions.newBuilder()
                .setRetryOptions(RetryOptions.newBuilder().setDoNotRetry().build())
                .setStartToCloseTimeout(Duration.ofSeconds(2))
                .build());
    private Boolean signalReceived = false;

    @Override
    public String getGreeting() {

      RetryOptions retryOptions = RetryOptions.newBuilder().setMaximumAttempts(3).build();

      try {

        Workflow.retry(
            retryOptions,
            Optional.empty(),
            () -> {
              activities.sendCommand("-", "-");
              boolean signaled = Workflow.await(Duration.ofSeconds(3), () -> this.signalReceived);
              if (!signaled) {
                throw ApplicationFailure.newFailure("Signal not received after x", "");
              }
            });

      } catch (ApplicationFailure e) {
        // DO something
        // rethrow to fail the workflow execution
      }

      System.out.println("Is blocking what");

      Workflow.sleep(Duration.ofSeconds(10));

      return "hello";
    }

    @Override
    public void signal() {
      this.signalReceived = true;
    }
  }
}
