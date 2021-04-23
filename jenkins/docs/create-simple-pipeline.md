## Simple pipeline job creation based on the Job DSL

It's a time-consuming task if you create the job on the UI. One or two jobs are affordable to create manually, but it would take a long time if you need to create 10 or 20 jobs same time. 

To resolve this issue, I'm going to use Job DSL with a Job dsl build job to make whole jobs from the script. 

## Job folder tree

This is my Job DSL folder tree for my pipeline job and each folder have own script related to the purpose. 
```
.
├── configs
├── jobs
├── pipelines
└── views
```

`configs` : Job configuration json file created by Job DSL. Job name, SCM info, Parameter default value, etc.
`jobs` : Job DSL groovy file for creating pipeline job configuration. Refer to [Job DSL Plugin API viewer.](https://jenkinsci.github.io/job-dsl-plugin/)
`pipelines` : Declarative pipeline groovy script. 
`view` : Job DLS groovi file for creating the listed view. Need to install `Listed View plugin` for this. 

## Sample script

|      Scripts        |
|---------------------|
| [Job Configuration](../configs/GatlingLoadTest.jobs.json) |
| [Pipeline Job](../jobs/GatlingLoadTest.groovy) |
| [Declarative Pipeline Script](../pipelines/GatlingLoadTest.groovy) |
| [Listed view](../views/GatlingView.groovy) |
 
