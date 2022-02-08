#!/usr/bin/env groovy

def generateReleaseVersion(releaseOption, currentVersion, customReleaseVersion) {
    def currentVersionList = currentVersion.replace('-SNAPSHOT', '').split('\\.')
    def currentVersions = []

    for(String s : currentVersionList) currentVersions.add(Integer.valueOf(s));

    def releaseVersion = ""

    switch (releaseOption) {
        case "MAJOR":
            currentVersions[0] += 1
            currentVersions[1] = 0
            currentVersions[2] = 0
            break;
        case "MINOR":
            currentVersions[1] += 1
            currentVersions[2] = 0
            break;
    }

    if (releaseOption == "CUSTOM") {
        releaseVersion = customReleaseVersion
    } else {
        releaseVersion = currentVersions.join('.')
    }

    echo "release version : ${releaseVersion}"

    return releaseVersion
}

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
                        branches: [[name: "*/develop"]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${params.ProjectName}"]],
                        submoduleCfg: [],
                        userRemoteConfigs: [[credentialsId: 'GitHub-access-token', url: "https://github.com/coolexplorer/${params.ProjectName}.git"]]]
                )
            }
        }

        stage('Build auth') {
            steps {
                container('maven') {
                    dir("${params.ProjectName}") {                
                        sh "mvn clean verify -DexcludedGroups=\"embedded-redis-test\""
                    }
                }
            }
        }

        stage('Release version') {
            steps {
                container('maven') {
                    dir("${params.ProjectName}") {                
                        withCredentials([usernamePassword(credentialsId: 'GitHub-access-token', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            script {
                                def currentVersion = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true

                                def releaseVersion = generateReleaseVersion("${params.ReleaseOption}", currentVersion, "${params.CustomReleaseVersion}")

                                sh "git checkout develop"
                                sh "mvn jgitflow:release-start -Dmaven.test.skip=true -DreleaseVersion=${releaseVersion} -Dgit.user=$USERNAME -Dgit.password=$PASSWORD"
                                sh "mvn jgitflow:release-finish -Dmaven.test.skip=true -Dgit.user=$USERNAME -Dgit.password=$PASSWORD"
                            }
                        }
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