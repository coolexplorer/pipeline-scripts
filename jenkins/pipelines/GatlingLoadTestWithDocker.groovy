#!/usr/bin/env groovy

pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/agent/k8s/docker.yaml'
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