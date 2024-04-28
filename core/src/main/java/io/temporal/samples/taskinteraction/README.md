# Demo tasks interaction

This example demonstrate a generic implementation for "User Tasks" interaction in Temporal.

TODO 

Temporal does not have such concept of "human task", as BPM systems, but it can be easily implemented with 
the pattern: 
- The main workflow have an activity (or local activity) that send the request to an external service. 
The external for this example is another workflow ([WorkflowTaskManagerImpl.java](WorkflowTaskManagerImpl.java)), 
that takes care of the task life-cicle.
- The main workflow wait with `Workflow.await` to receive a Signal.The external service signal back the main 
workflow to unblock it.

The three steps mentioned above are encapsulated in the class [TaskClient.java](./TaskClient.java)

## Run the sample

- Schedule the main workflow execution

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.StartWorkflow
```

- Open other terminal and Start the Worker

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.worker.Worker
```

The worker will start the workflow execution and schedule the two activities: 


```
06:08:22.927 {WorkflowWithTasks0.25382038076376945 } [workflow[WorkflowWithTasks0.25382038076376945]-1] INFO  i.t.s.taskinteraction.TaskService - Before creating task : Task{token='WorkflowWithTasks0.25382038076376945-1713845302806-1', title=TaskTitle{value='TODO 1'}} 
06:08:22.958 {WorkflowWithTasks0.25382038076376945 } [workflow[WorkflowWithTasks0.25382038076376945]-2] INFO  i.t.s.taskinteraction.TaskService - Before creating task : Task{token='WorkflowWithTasks0.25382038076376945-1713845302806-2', title=TaskTitle{value='TODO 2'}} 
06:08:23.039 {WorkflowWithTasks0.25382038076376945 } [workflow[WorkflowWithTasks0.25382038076376945]-1] INFO  i.t.s.taskinteraction.TaskService - Task created: Task{token='WorkflowWithTasks0.25382038076376945-1713845302806-1', title=TaskTitle{value='TODO 1'}} 
06:08:23.039 {WorkflowWithTasks0.25382038076376945 } [workflow[WorkflowWithTasks0.25382038076376945]-2] INFO  i.t.s.taskinteraction.TaskService - Task created: Task{token='WorkflowWithTasks0.25382038076376945-1713845302806-2', title=TaskTitle{value='TODO 2'}} 

```

- Complete task in the "External system". This class will query and complete one of the 
pending task in the external system, in this case a workflow, that will at the same time, 
signal back the main workflow (the one that created the task and is waiting)

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.CompleteTask
```

