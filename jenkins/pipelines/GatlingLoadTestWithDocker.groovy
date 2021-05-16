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

        stage('Docker version') {
            steps {
                container('docker') {
                    dir('maven-gatling') {
                        sh "docker version"
                    }
                }
            }
        }
    }
}