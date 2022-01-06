#!/usr/bin/env groovy

import groovy.json.JsonSlurper

def jobList = new JsonSlurper().parseText(new File("${WORKSPACE}/jenkins/configs/spring-micro/BuildImageAuth.json").text)

jobList.each { job -> 
    pipelineJob(job.name) {
        parameters {
            stringParam('Registry', job.registry, 'Registry address')
            stringParam('Branch', job.branch, 'Source Branch')
            stringParam('Tag', job.tag, 'Image tag - ex) 0.1.0')
            activeChoiceParam('Profile') {
                description('Select Profile.')
                filterable(false)
                choiceType('RADIO')
                groovyScript {
                    script('["dev", "stage", "prod"]')
                    fallbackScript('"fallback choice"')
                }
            }
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