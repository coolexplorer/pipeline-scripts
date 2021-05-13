#!/usr/bin/env groovy

def reportPath = "target/gatling/report"
def resultPath = "target/gatling"

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
                    dir('maven-gatling') {
                        sh "mkdir -p ${reportPath}"
                    }
                }
            }
        }

        stage('Run Gatling test') {
            steps {
                container('maven') {
                    dir('maven-gatling') {
                        sh 'mvn gatling:test'
                    }
                }
            }
        }

        stage('Generate Gatling report') {
            steps {
                container('maven') {
                    dir('maven-gatling') {
                        script {
                            def simulationPath = sh script: "ls -l ${resultPath} | grep loadtestsimulation | awk \"{print \$9}\"", returnStdout: true
                            echo "${simulationPath}"
                            sh "cp ${resultPath}/${simulationPath}/*.log ${reportPath}"

                            sh 'mvn gatling:test -Dgatling.reportsOnly=report'
                        }
                    }
                }
            }
        }

        stage('Publish gatling report') {
            steps {
                container('maven') {
                    dir('maven-gatling') {
                        publishHTML target: [
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: "maven-gatling/${reportPath}",
                            reportFiles: 'index.html',
                            reportName: 'Gatlinge report'
                        ]
                    }
                }
            }
        }
    }
}