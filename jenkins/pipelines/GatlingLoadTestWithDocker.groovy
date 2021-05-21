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
                            TARGET_SERVER=${params.TargetServer}
                            USERS=${params.Users}
                            DURATION=${params.Duration}
                            RAMPUP_DURATION=${params.RampUpDuration}
                            DURATION_UNIT=${params.DurationUnit}
                            SCENARIO=${params.Scenario}
                            """.stripIndent()

                            echo "Environment variables : ${loadProfile}"

                            writeFile(file: "load_profile.env", text: loadProfile)
                        }
                    }
                }
            }
        }

        stage('Run Gatling test') {
            steps {
                container('docker') {
                    dir('maven-gatling') {
                        script {
                            echo "cat load_profile.env"
                            def docker_command = "docker run --ulimit nofile=20480:20480 --env-file ./load_profile.env --name=loadgen ${params.Image}"
                            def gatling_command = "bash -c \"mvn gatling:test\""
                            sh "${docker_command} ${gatling_command}"
                        }
                    }
                }
            }
        }
    }
}