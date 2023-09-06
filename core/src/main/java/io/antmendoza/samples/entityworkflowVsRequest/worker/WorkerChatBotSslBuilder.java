package io.antmendoza.samples.entityworkflowVsRequest.worker;

public class WorkerChatBotSslBuilder {

  private int workflowCacheSize = 20;
  private int maxConcurrentActivityExecutionSize = 20;
  private int maxConcurrentLocalActivityExecutionSize = 20;
  private int maxConcurrentWorkflowTaskExecutionSize = 20;

  private int maxWorkflowThreadCount = 10;

  public WorkerChatBotSslBuilder setWorkflowCacheSize(int workflowCacheSize) {
    this.workflowCacheSize = workflowCacheSize;
    return this;
  }

  public WorkerChatBotSslBuilder setMaxConcurrentActivityExecutionSize(
      int maxConcurrentActivityExecutionSize) {
    this.maxConcurrentActivityExecutionSize = maxConcurrentActivityExecutionSize;
    return this;
  }

  public WorkerChatBotSslBuilder setMaxConcurrentLocalActivityExecutionSize(
      int maxConcurrentLocalActivityExecutionSize) {
    this.maxConcurrentLocalActivityExecutionSize = maxConcurrentLocalActivityExecutionSize;
    return this;
  }

  public WorkerChatBotSslBuilder setMaxConcurrentWorkflowTaskExecutionSize(
      int maxConcurrentWorkflowTaskExecutionSize) {
    this.maxConcurrentWorkflowTaskExecutionSize = maxConcurrentWorkflowTaskExecutionSize;
    return this;
  }

  public WorkerChatBotSslBuilder setMaxWorkflowThreadCount(int maxWorkflowThreadCount) {
    this.maxWorkflowThreadCount = maxWorkflowThreadCount;
    return this;
  }

  public WorkerChatBotSsl createWorkerChatBotSsl() {
    return new WorkerChatBotSsl(
        workflowCacheSize,
        maxConcurrentActivityExecutionSize,
        maxConcurrentLocalActivityExecutionSize,
        maxConcurrentWorkflowTaskExecutionSize,
        maxWorkflowThreadCount);
  }
}
