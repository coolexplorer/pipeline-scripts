# Kubernetes dynamic agent via Pod template

If you operate your Jenkins in the Kubernetes, you can make agent dynamically via Pod. Pods are the smallest deployable units of computing that you can create and manage in Kubernetes. You can see the details on this [link](https://kubernetes.io/docs/concepts/workloads/pods/).

Pods in Kubernetes can run multicontainer simultaneously, which is a good option making our agent. For example, you just run the container added on `docker` or `maven` if you want to use them. 

## Prerequisites
1. Install plugins - Kubernetes Plugin
2. Jenkins pod in Kuberenetes is needed to have a permision to create pods. 
   * Needed to add a service account with RBAC (Role Based Access Control)
   * Refer to my repository, k8s-chart, there is the way to install Jenkins with a servcie account. Make sure to check below values in `values.yaml` file.
    ```yaml
    serviceAccount:
        create: true
    ```
    > If you get the official helm chart from Jenkins, above option is default. 

## Pipeline script with Dynamic agent

OK. You have the environment to make container based Agent in Kubernetes. Let's see how you can make Pipeline scripts with this agent.
