pipeline {
    agent {
        kubernetes {
            yamlFile 'agent/k8s/maven.yaml'
        }
    }
    stages {
        stage('Run maven version') {
            steps {
                container('maven') {
                    sh 'mvn --version'
                }
            }
        }
    }    
}