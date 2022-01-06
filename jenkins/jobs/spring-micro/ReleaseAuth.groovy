#!/usr/bin/env groovy

import groovy.json.JsonSlurper

def jobList = new JsonSlurper().parseText(new File("${WORKSPACE}/jenkins/configs/spring-micro/ReleaseAuthJobs.json").text)

jobList.each { job ->
    pipelineJob(job.name) {
        parameters {
            activeChoiceParam('ReleaseOption') {
                description('Select the release version option.')
                filterable(false)
                choiceType('RADIO')
                groovyScript {
                    script('["MAJOR", "MINOR", "FETCH", "CUSTOM"]')
                    fallbackScript('"fallback choice"')
                }
            }
            stringParam('CustomReleaseVersion', '', 'Customized Release Version - This option will be adapted when you choose "CUSTOM" option.')
        }

        logRotator {
            daysToKeep(14)
        }

        properties {
            pipelineTriggers {
                triggers {
                    bitBucketTrigger {
                        triggers {
                            bitBucketPPRPullRequestTriggerFilter {
                                actionFilter {
                                    bitBucketPPRPullRequestMergedActionFilter {
                                        allowedBranches(job.git.branch)
                                        isToApprove(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        definition {
            cpsScm {
                scriptPath(job.scriptPath)
                scm {
                    git {
                        branch(job.git.branch)
                        remote {
                            credentials(job.git.credential)
                            url(job.git.url)
                        }
                    }
                }
            }
        }
    }
}