#!/usr/bin/env groovy

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
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${params.ProjectName}"]],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: 'GitHub-access-token', url: "https://github.com/coolexplorer/${params.ProjectName}.git"]]]
                )
            }
        }

        stage('Build') {
            steps {
                container('maven') {
                    dir("${params.ProjectName}") {
                        // TODO: Delete excludedGroups if use the agent of linux machine. 
                        //       Embedded servers of redis and kafka is not working under the pod agent.     
                        sh 'mvn clean verify -DexcludedGroups=embedded-redis-test,embedded-kafka-test'
                    }
                }
            }
        }

        stage('Docker login') {
            steps {
                container('docker') {
                    dir("${params.ProjectName}") {
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
                    dir("${params.ProjectName}") {
                        sh "docker build --build-arg PROFILE=${params.Profile} --no-cache . -t ${params.Registry}/${params.ImageName}:${params.Tag}"
                    }
                }
            }
        }

        stage('Push docker image') {
            steps {
                container('docker') {
                    dir("${params.ProjectName}") {
                        sh "docker push ${params.Registry}/${params.ImageName}:${params.Tag}"
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