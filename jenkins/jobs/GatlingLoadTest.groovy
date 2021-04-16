import groovy.json.JsonSlurper

def jobList = new JsonSlurper().parseText(new File("${WORKSPACE}/jenkins/configs/GatlingLoadTest.jobs.json").text)

jobList.each { job ->
    pipelineJob(job.name) {
        blockOn([job.name]) {
            blockLevel('NODE')
            scanQueueFor('DISABLED')
        }


        parameters {
            stringParam('TargetServer', job.target, 'Target Server')
        }

        logRotator {
            daysToKeep(14)
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