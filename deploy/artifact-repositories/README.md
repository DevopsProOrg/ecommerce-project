# Nexus and Artifactory Guide

This project supports publishing the backend Maven artifact to either Nexus or Artifactory from Jenkins.

Docker images are still pushed to Docker Hub or your configured container registry. Nexus or Artifactory is used for Maven artifact governance.

## 1. Artifact Strategy

Use both artifact types:

- Docker images for deployments [example: `docker.io/yarrav7/ogfashions-backend:v1.2`]
- Maven artifacts for build traceability [example: `com.shop:ecommerce:1.2`]

Version rule:

- Jenkins `RELEASE_VERSION=v1.2`
- Docker image tag becomes `v1.2`
- Maven artifact version becomes `1.2`

## 2. Nexus Setup

Create a hosted Maven repository:

```text
Repository type: maven2 hosted
Name: maven-releases
Version policy: Release
Deployment policy: Allow redeploy only if your org allows it; otherwise disable redeploy
URL example: http://nexus.example.com:8081/repository/maven-releases/
```

Create Jenkins credential:

```text
Credential ID: nexus-creds
Type: Username with password
Username: nexus deployment username
Password: nexus deployment password or token
```

Jenkins parameters:

```text
ARTIFACT_REPOSITORY=nexus
NEXUS_MAVEN_REPO_URL=http://nexus.example.com:8081/repository/maven-releases/
RELEASE_VERSION=v1.2
```

## 3. Artifactory Setup

Create a local Maven repository:

```text
Repository type: Maven local
Repository key: libs-release-local
URL example: https://artifactory.example.com/artifactory/libs-release-local/
```

Create Jenkins credential:

```text
Credential ID: artifactory-creds
Type: Username with password
Username: Artifactory deployment username
Password: Artifactory access token or password
```

Jenkins parameters:

```text
ARTIFACT_REPOSITORY=artifactory
ARTIFACTORY_MAVEN_REPO_URL=https://artifactory.example.com/artifactory/libs-release-local/
RELEASE_VERSION=v1.2
```

## 4. Jenkins Publish Flow

Jenkins performs these steps:

1. validates backend and frontend
2. packages backend
3. builds Docker images
4. optionally publishes Maven artifact to Nexus or Artifactory
5. pushes Docker images
6. deploys selected environment
7. runs smoke test
8. sends email notification

## 5. Validation

After a successful Jenkins run, verify:

```text
Nexus or Artifactory contains com/shop/ecommerce/<version>
Docker registry contains ogfashions-backend:<version>
Docker registry contains ogfashions-frontend:<version>
Jenkins archived deployment-summary.txt
Jenkins archived release-notes.txt
```

## 6. Operational Rules

- Do not store Nexus or Artifactory passwords in Git.
- Use Jenkins credentials only.
- Use release repositories for `lp`.
- Use snapshot repositories only if you later add snapshot builds.
- Keep Maven artifact version aligned with Docker release version.
- Keep the same Docker image tag across `l1`, `l2`, `l3`, `l6`, and `lp`.
