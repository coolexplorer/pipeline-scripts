pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/agent/k8s/maven.yaml'
        }
    }
    stages {
        stage('Clone the source') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']],
                        userRemoteConfigs: [[url: 'https://github.com/coolexplorer/maven-gatling.git']]])
            }
        }
        stage('Run maven version') {
            steps {
                container('maven') {
                    sh 'mvn -version'
                }
            }
        }
    }
}