# How this works

- Client send a request to the server to start a workflow
- Server persist the request, and deliver the task to the worker
- If there are no workers available, or the workers are overloaded the task will sit in the server 
until it can be delivered to the worker
- the worker get the task and execute the task, report back to the server and at the same time
store the workflow state in cache.
- sometimes, if your workers get restarted oryou don't have enough cache, the workflow state will be evicted from cache
- on the next workflow task delivered to the worker, the worker does not have the workflow state in cache, so
it has to poll the workflow history, and replay it to recover the workflow state and only then execute the task 


### Workflow per request
- advantages:
  - short running workflows:
    - easier to deal with, when you have to change your workflow code. 
  - do not handle event history limit and history size (50K)

- disadvantages:
  - have to keep the status of the entity in a separate database (send the request, update the db, and reply back)
  - 

### Signal + query
- signal: can change the workflow status, but can not return anything.
- query: can query workflow status, workflow variable, but can not mutate workflow state
  
- advantages:
  - Use other Temporal features like timers, cancellations etc.. 
  - Your workflow can run forever
  - Workflow become the source of truth

- disadvantages:
  - Signaling a workflow is an is asynchronous operation
  - Continue as new
  - Worker performance, can introduce delays, 
  - Specially with signals, depending on how you implement this. 
  - when you send a message to the 


There are two approaches, either you query the workflow after signaling it, if the workflow for some reason 
has not processed the signal, you will have to query it again. 


For the first consideration, the main thing is any pending, unprocessed Signals. 
You'll need to make sure to handle all pending Signals and Update before continuing as new.
Continue as new carry on with the signals that has not been processed 



### Workflow update
This is a new feature of Temporal, before there was two main ways to interact with a workflow, 
- signal: can change the workflow status, but can not return anything.
- query: can query workflow status, workflow variable, but can not mutate workflow state

Now we have something called workflow update that allows the client to send a synchronous signal to the 
workflow and wait for the result. You can mutate workflow state and the method returns something. 

This is convenient way probably for you to implement your use-case, or at least something you want to take a look. 

- advantages:
  - update workflow is syncronous operation
  - Use other Temporal features like timers, cancellations etc..
  - Your workflow can run forever
  - Workflow become the source of truth

- disadvantages:
  - Continue as new
  - Worker performance, can introduce delays, 





