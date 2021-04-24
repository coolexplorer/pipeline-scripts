pipeline {
    agent {
        kubernetes {
            yamlFile 'jenkins/agent/k8s/maven.yaml'
        }
    }
    stages {
        stage('Run maven version') {
            steps {
                container('maven') {
                    sh 'mvn -version'
                }
            }
        }
    }    
}