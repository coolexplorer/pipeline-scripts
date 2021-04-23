# Job DSL build Job 

## Job creation

If you make your jobs using Job DSL, you can make them just one click using Job DSL build job. 

1. Create Freestyle job - `New Item > Freestyle project`
2. Add `Source code Management`. Connect this repository.
   * Add repository information  
    ![source_management](../../resource/images/build_job_dsl_source_management.png)
   * Add `Process Job DSLs` in `Build` Tab with the script path  
    ![job_dsl_build](../../resource/images/build_job_dsl.png)

## Run Job DSL build job

1. Build Job and see the success result.  
   ![job_dsl_build_success](../../resource/images/job_dsl_build_success.png)

2. Check the View and jobs you've added.  
   ![built_jobs_by_job_dsl](../../resource/images/built_jobs_by_job_dsl.png)