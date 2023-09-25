- One workflow starting many child workflows?
- What the activities are doing?
- 
- each worker, is able to execute a limited number or activities and workflow task concurrently,
now might not be a problem and for specific use-cases might be ok, but if you have 200 running workflows
and each workflow create 3 activities that are occupying one slot, those are 900 slots. Again, 
maybe this is not a problem now, but I don't know in the future if it will be.
  - everytime an activity is delivered to your worker. 
- keep resources busy