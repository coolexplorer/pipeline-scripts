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
        stage('Run maven version') {
            steps {
                container('maven') {
                    dir('maven-gatling') {
                        sh 'mvn gatling:test -Dgatling.noReports=true'
                    }
                }
            }
        }

        stage('Generate Gatling report') {
            steps {
                container('maven') {
                    dir('maven-gatling') {
                        sh 'mvn gatling:test -Dgatling.reportsOnly=report'
                        sh 'ls -al'
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
                            reportDir: 'coverage',
                            reportFiles: 'index.html',
                            reportName: 'RCov Report'
                        ]
                    }
                }
            }
        }
    }
}