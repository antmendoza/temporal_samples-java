# Demo tasks interaction

This example shows a generic implementation for tasks interaction in Temporal.

The basic implementation consist on three parts. 
- One activity that send the request (create a task).
- Block the workflow execution `Workflow.await` awaiting a Signal.
- The workflow will eventually receive a signal that unblocks it.

If the client can not send a Signal to the workflow execution, steps 2 and 3 can be replaced by an activity 
that polls using one of [these three strategies](../polling). 

Additionally,the example allows the task progress tracking (PENDING, STARTED, COMPLETED) 





https://docs.oasis-open.org/bpel4people/ws-humantask-1.1-spec-cs-01.html

