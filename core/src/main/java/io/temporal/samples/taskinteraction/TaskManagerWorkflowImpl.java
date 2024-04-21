package io.temporal.samples.taskinteraction;

import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TaskManagerWorkflowImpl implements TaskManagerWorkflow {

  private List<Task> pendingTask;

  private List<String> taskToComplete;

  @Override
  public void execute(List<Task> inputPendingTask, List<String> inputTaskToComplete) {

    System.out.println("starting...  " + pendingTask);

    initPendingTasks(inputPendingTask);
    initTaskToComplete(inputTaskToComplete);

    while (true) {

      final List<Task> currentTask = new ArrayList<>(pendingTask);

      // Wait until one task is added / removed
      Workflow.await(
          () ->
              currentTask.size() != pendingTask.size()
                  // or there are pending task to complete
                  || !taskToComplete.isEmpty());

      if (!taskToComplete.isEmpty()) {
        final String taskToken = taskToComplete.remove(0);
        final String externalWorkflowId = new StringTokenizer(taskToken, "-").nextToken();
        Workflow.newExternalWorkflowStub(TaskClient.class, externalWorkflowId)
            .completeByTaskToken(taskToken);

        final Task task =
            pendingTask.stream().filter((t) -> t.getToken().equals(taskToken)).findFirst().get();
        pendingTask.remove(task);
      }

      if (Workflow.getInfo().isContinueAsNewSuggested()) {
        Workflow.continueAsNew(pendingTask);
      }
    }
  }

  private void initTaskToComplete(final List<String> tasks) {
    if (taskToComplete == null) {
      taskToComplete = new ArrayList<>();
    }
    taskToComplete.addAll(tasks);
  }

  private void initPendingTasks(final List<Task> tasks) {

    if (pendingTask == null) {
      pendingTask = new ArrayList<>();
    }
    pendingTask.addAll(tasks);
  }

  @Override
  public void addTask(Task task) {

    System.out.println("addTask...  " + task);

    initPendingTasks(new ArrayList<>());
    pendingTask.add(task);
  }

  @Override
  public void completeTaskByTaskToken(String taskToken) {
    taskToComplete.add(taskToken);
  }

  @Override
  public List<Task> getPendingTask() {
    return pendingTask;
  }
}
