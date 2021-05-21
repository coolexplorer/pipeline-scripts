#!/usr/bin/env groovy

def DOCKER_NAME = "loadgen"
def testResultPath = "target"
def repositoryPath = "maven-gatling"

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

        stage('Initialize environment') {
            steps {
                container('docker') {
                    script {
                        try {
                            // Remove the docker container 
                            sh "docker rm ${DOCKER_NAME}"
                        } catch (error) {
                            echo "${error.message}"
                            echo "${DOCKER_NAME} container is not exist."
                        }
                        
                    }
                }
            }
        }

        stage('Make the load profile') {
            steps {
                container('docker') {
                    dir("${repositoryPath}") {
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

                            writeFile(file: "./load_profile.env", text: loadProfile)
                        }
                    }
                }
            }
        }

        stage('Run Gatling test') {
            steps {
                container('docker') {
                    dir("${repositoryPath}") {
                        script {
                            sh "cat ./load_profile.env"

                            def docker_command = "docker run --rm --network host --ulimit nofile=20480:20480 --env-file ./load_profile.env --name=${DOCKER_NAME} -v \"${WORKSPACE}/${REPOSITORY_PATH}\":\"/${testResultPath}/target\" ${params.Image}"
                            def gatling_command = "bash -c \";mvn gatling:test;chmod 777 -R /gatling-test/target\""

                            sh "${docker_command} ${gatling_command}"

                            sh "ls ./gatling-test"
                        }
                    }
                }
            }
        }

        stage('Generate Report') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        script {
                            sh "mkdir -p target/gatling/report"
                            sh 'mvn gatling:test -Dgatling.noReports=true'
                        }
                    }
                }
            }
        }

        stage('Publish gatling report') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        publishHTML target: [
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: "target/gatling/report",
                            reportFiles: 'index.html',
                            reportName: 'Gatling_report'
                        ]
                    }
                }
            }
        }
    }
}