# Demo tasks interaction

This example demonstrate a generic implementation for "human tasks" interaction in Temporal.

Temporal does not have such concept of "human task", as BPM systems, but it can be easyly implemented with 
the pattern: 
- One activity (or local activity) that send the request to an external service. The external 
service is where the task life-cicle is kept. For this example we are not using any external service but
- Block the workflow execution `Workflow.await` awaiting a Signal.
- The workflow will eventually receive a signal that unblocks it.


Additionally, the example allows to track task state (PENDING, STARTED, COMPLETED...).

> If the client can not send a Signal to the workflow execution, steps 2 and 3 can be replaced by an activity
that polls using one of [these three strategies](../polling).

## Run the sample


- Schedule the workflow execution

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.StartWorkflow
```

- Open other terminal and Start the Worker

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.worker.Worker
```

The worker will start the workflow execution and schedule the two activities: 

- Update task

Update one of the open task to the next state (PENDING -> STARTED -> COMPLETED)
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.CompleteNextTask
```

The workflow has three task, each task has three different states and is created in PENDING state. 
You will have to run this class six times to move each task from PENDING to STARTED and COMPLETED. 
Once the last task is completed the workflow completes.
