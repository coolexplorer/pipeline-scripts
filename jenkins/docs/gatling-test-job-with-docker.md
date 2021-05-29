# Gatling test job with docker

This document illustrates how to run the Gatling script with docker container. If you'd like to run the script with lots of generators, running scripts with docker container is essential. This gives us many advantages running, gathering results, and generating reports. 

## Agent

Now, I'm running my own Jenkins on the kind (kubernetes in docker). And Jenkins agent is created as pods, this is also the docker container. In order to run the script under the container, we need to particular docker image called `docker-dind` which means `docker in docker`. Please refer to below yaml file for the agent.

```yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: jenkins
    type: agent
spec:
  containers:
...
  - name: docker
    image: docker:19.03.1
    command: ['sleep', '99d']
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: docker-daemon
    image: docker:19.03.1-dind
    env:
    - name: DOCKER_TLS_CERTDIR
      value: ""
    securityContext:
      privileged: true
    volumeMounts:
        - name: cache
          mountPath: /var/lib/docker
...
```

## Pipeline script

### Load profile
To run the our Gatling script under the container, we are going to create the file to pass the test profile into the container. 

Below scripts, I create the `load_profile.env` and will add the environment variable parameter on the run command. Refer to a below script. 

```groovy
def DOCKER_NAME = "loadgen"
def reportPath = "target/gatling/report"
def resultPath = "target/gatling"
def gatlingWorkPath = "/gatling-test"
def repoDirectory = "maven-gatling"

stage('Make the load profile') {
    steps {
        container('docker') {
            dir("${repoDirectory}") {
                script {
                    // This is the load profile environment variables which is added from Jeknins parameters.
                    def loadProfile = """
                    TARGET_SERVER=${params.TargetServer}
                    USERS=${params.Users}
                    DURATION=${params.Duration}
                    RAMPUP_DURATION=${params.RampUpDuration}
                    DURATION_UNIT=${params.DurationUnit}
                    SCENARIO=${params.Scenario}
                    """.stripIndent()

                    echo "Environment variables : ${loadProfile}"

                    writeFile(file: "./load_profile.env", text: loadProfile)
                }
            }
        }
    }
}
```

### Run stage

This stage is focusing on running one container for the test. You can utilize below command and expand it to run multi containers. 

```groovy
stage('Run Gatling test') {
    steps {
        container('docker') {
            dir("${repoDirectory}") {
                script {
                    sh "cat ./load_profile.env"

                    sh "docker pull ${params.Image}"

                    // Docker run command - network option, ulimit setting, env file, container name, volume mapping
                    def docker_command = "docker run --rm --network host --ulimit nofile=20480:20480 --env-file ./load_profile.env --name=${DOCKER_NAME} -v \"${WORKSPACE}/${repoDirectory}/target\":\"${gatlingWorkPath}/target\" ${params.Image}"
                    
                    // This command is for running gatling when the container is created. 
                    def gatling_command = "bash -c \"mvn gatling:test -Dgatling.noReports=true;chmod 777 -R ${gatlingWorkPath}/target/*\""

                    sh "${docker_command} ${gatling_command}"
                }
            }
        }
    }
}
```

### Report generation

I used the report generation command after the log file extraction from the directory mapped. 

```groovy
stage('Generate Gatling report') {
    steps {
        container('maven') {
            dir("${repoDirectory}") {
                script {
                    sh "mkdir -p ${reportPath}"

                    def simulationPath = sh script: "ls -l ${resultPath} | grep loadtestsimulation | awk '{print \$9}'", returnStdout: true
                    
                    sh "cp ${resultPath}/${simulationPath.trim()}/simulation.log ${reportPath}/simulation.log"
                    sh 'mvn gatling:test -Dgatling.reportsOnly=report'
                }
            }
        }
    }
}
```

