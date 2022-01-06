#!/usr/bin/env groovy

def imageName = "coolexplorer/spring-micro-auth"
def repositoryPath = "spring-micro-auth"

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
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'spring-micro-auth']],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: 'GitHub-access-token', url: 'https://github.com/coolexplorer/spring-micro-auth.git']]]
                )
            }
        }

        stage('Build api server') {
            steps {
                container('maven') {
                    dir("${repositoryPath}") {
                        sh "mvn clean install"
                    }
                }
            }
        }

        stage('Docker login') {
            steps {
                container('docker') {
                    dir("${repositoryPath}") {
                        withCredentials([usernamePassword(credentialsId: 'docker-registry-secret', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            sh "docker login ${params.Registry} -u $USERNAME -p $PASSWORD"
                        }
                    }
                }
            }
        }

        stage('Build docker image') {
            steps {
                container('docker') {
                    dir("${repositoryPath}") {
                        sh "docker build --build-arg PROFILE=${params.Profile} --no-cache . -t ${params.Registry}/${imageName}:${params.Tag}"
                    }
                }
            }
        }

        stage('Push docker image') {
            steps {
                container('docker') {
                    dir("${repositoryPath}") {
                        sh "docker push ${params.Registry}/${imageName}:${params.Tag}"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [[pattern: '.gitignore', type: 'INCLUDE']])
        }
        // success {
        //     script {
        //         def message = "*${currentBuild.result}* - *${env.JOB_NAME}* (<${env.BUILD_URL}|Link>)"
        //         slackSend channel: "#yaybooh-project", message: "${message}", color: "good", tokenCredentialId: "slack-token", botUser: true
        //     }
        // }
        // failure {
        //     script {
        //         def message = "*${currentBuild.result}* - *${env.JOB_NAME}* (<${env.BUILD_URL}|Link>)"
        //         slackSend channel: "#yaybooh-project", message: "${message}", color: "danger", tokenCredentialId: "slack-token", botUser: true
        //     }
        // }
    }
}