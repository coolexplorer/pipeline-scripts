#!/usr/bin/env groovy

def imageName = "spring-micro-auth"

pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/agent/k8s/maven.yaml'
        }
    }

    stages {
        stage('Clone the source') {
            steps {
                checkout scm
            }
        }

        stage('Build api server') {
            steps {
                container('maven') {
                    sh "mvn clean install"
                }
            }
        }

        stage('Docker login') {
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'docker-registry-secret', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login registry.yaybooh.com -u $USERNAME -p $PASSWORD"
                    }
                }
            }
        }

        stage('Build docker image') {
            steps {
                container('docker') {
                    sh "docker build --build-arg PROFILE=${params.Profile} --no-cache . -t ${params.Registry}/${imageName}:${params.Tag}"
                }
            }
        }

        stage('Push docker image') {
            steps {
                container('docker') {
                    sh "docker push ${params.Registry}/${imageName}:${params.Tag}"
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            // script {
            //     def message = "*${currentBuild.result}* - *${env.JOB_NAME}* (<${env.BUILD_URL}|Link>)"
            //     slackSend channel: "#yaybooh-project", message: "${message}", color: "good", tokenCredentialId: "slack-token", botUser: true
            // }
        }
        failure {
            // script {
            //     def message = "*${currentBuild.result}* - *${env.JOB_NAME}* (<${env.BUILD_URL}|Link>)"
            //     slackSend channel: "#yaybooh-project", message: "${message}", color: "danger", tokenCredentialId: "slack-token", botUser: true
            // }
        }
    }
}