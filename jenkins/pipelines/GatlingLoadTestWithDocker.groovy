#!/usr/bin/env groovy

pipeline {
    agent {
        kubernetes {
            // My cluster is running on the docker container via kind. So, for using docker on the agent, need to set up dind(docker-in-docker).
            yamlFile 'jenkins/agent/k8s/docker-dind.yaml'
        }
    }

    stages {
        stage('Clone the source') {
            steps {
                checkout([$class: 'GitSCM',
                        branches: [[name: "*/${params.Branch}"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'maven-gatling']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: 'pipeline_access_token', url: 'https://github.com/coolexplorer/maven-gatling.git']]]
                )
            }
        }

        stage('Make the load profile') {
            steps {
                container('docker') {
                    dir('maven-gatling') {
                        script {
                            def loadProfile = """
                            #!/bin/bash
                            export TARGET_SERVER=${params.TargetServer}
                            export USERS=${params.Users}
                            export DURATION=${params.Duration}
                            export RAMPUP_DURATION=${params.RampUpDuration}
                            export DURATION_UNIT=${params.DurationUnit}
                            export SCENARIO=${params.Scenario}
                            """.stripIndent()

                            echo "Environment variables : ${loadProfile}"

                            writeFile file: "./load_profile.sh", text: loadProfile
                        }
                    }
                }
            }
        }
    }
}