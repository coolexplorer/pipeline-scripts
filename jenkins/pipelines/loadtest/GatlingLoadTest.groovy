#!/usr/bin/env groovy

def reportPath = "target/gatling/report"
def resultPath = "target/gatling"
def repositoryPath = "maven-gatling"

pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/agent/k8s/maven.yaml'
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

        stage('Initialize Gatling test') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        sh "mkdir -p ${reportPath}"
                    }
                }
            }
        }

        stage('Make the load profile') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
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

        stage('Run Gatling test') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        sh './load_profile.sh && mvn gatling:test -Dgatling.noReports=true'
                    }
                }
            }
        }

        stage('Generate Gatling report') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        script {
                            def simulationPath = sh script: "ls -l ${resultPath} | grep loadtestsimulation | awk '{print \$9}'", returnStdout: true

                            sh "cp ${resultPath}/${simulationPath.trim()}/simulation.log ${reportPath}/simulation.log"

                            sh 'mvn gatling:test -Dgatling.reportsOnly=report'
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