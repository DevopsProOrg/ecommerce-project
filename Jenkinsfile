def runCmd(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat(windowsCommand ?: unixCommand)
    }
}

def captureCmd(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        return sh(script: unixCommand, returnStdout: true).trim()
    }
    return bat(script: windowsCommand ?: unixCommand, returnStdout: true).trim()
}

def sendBuildNotification(String status) {
    def recipients = (params.NOTIFY_EMAIL ?: env.NOTIFY_EMAIL ?: '').trim()
    if (!recipients) {
        echo "NOTIFY_EMAIL is not configured. Skipping ${status} notification email."
        return
    }

    def lines = [
        "Build status: ${status}",
        "Job: ${env.JOB_NAME}",
        "Build: #${env.BUILD_NUMBER}",
        "Branch: ${env.BRANCH_NAME ?: 'n/a'}",
        "Deploy target: ${params.DEPLOY_TARGET}",
        "Deploy level: ${env.DEPLOY_LEVEL ?: 'n/a'}",
        "K8s overlay: ${env.DEPLOY_LEVEL ?: params.K8S_OVERLAY}",
        "Effective image tag: ${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
        "Existing image reuse: ${env.IS_ROLLBACK ?: 'false'}",
        "Existing image tag: ${params.ROLLBACK_IMAGE_TAG ?: 'n/a'}",
        "Image tag: ${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
        "Approved by: ${env.APPROVED_BY ?: 'n/a'}",
        "Change ticket: ${params.CHANGE_TICKET ?: 'n/a'}",
        "Backend image: ${env.BACKEND_IMAGE ?: 'n/a'}:${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
        "Frontend image: ${env.FRONTEND_IMAGE ?: 'n/a'}:${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
        "Base URL: ${env.DEPLOY_BASE_URL ?: 'n/a'}",
        "Build URL: ${env.BUILD_URL ?: 'n/a'}"
    ]

    emailext(
        to: recipients,
        subject: "${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${env.BRANCH_NAME ?: 'n/a'}]",
        body: (lines + ['', 'Release notes:', env.RELEASE_NOTES ?: 'No release notes available.']).join('\n')
    )
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '25'))
        timeout(time: 90, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['none', 'docker', 'k8s', 'both'], description: 'Choose deployment target.')
        choice(name: 'DEPLOY_LEVEL', choices: ['none', 'l1', 'l2', 'l3', 'l6', 'lp'], description: 'Choose one deployment environment for this run. Image tags are generated automatically.')
        choice(name: 'K8S_OVERLAY', choices: ['l1', 'l2', 'l3', 'l6', 'lp'], description: 'Choose Kubernetes overlay.')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis if configured.')
        booleanParam(name: 'RUN_TRIVY', defaultValue: true, description: 'Run Trivy filesystem and image scans if installed.')
        choice(name: 'ARTIFACT_REPOSITORY', choices: ['none', 'nexus', 'artifactory'], description: 'Publish backend Maven artifact to Nexus or Artifactory.')
        booleanParam(name: 'RUN_NEXUS_PUBLISH', defaultValue: false, description: 'Legacy switch. Prefer ARTIFACT_REPOSITORY=nexus.')
        string(name: 'ROLLBACK_IMAGE_TAG', defaultValue: '', description: 'Optional existing build tag to promote or roll back. When set, Jenkins skips building and redeploys that registry image.')
        string(name: 'CHANGE_TICKET', defaultValue: '', description: 'Optional change ticket, release ID, or CAB reference for auditability.')
        string(name: 'RELEASE_NOTES_MAX_COMMITS', defaultValue: '15', description: 'How many recent commits to include when no previous successful build is available.')
        string(name: 'EC2_SSH_HOST', defaultValue: '44.211.170.188', description: 'Target Docker host public IP or DNS name.')
        string(name: 'EC2_SSH_USER', defaultValue: 'ubuntu', description: 'Target Docker host SSH user. Example: ubuntu or ec2-user.')
        string(name: 'EC2_SSH_CREDENTIALS_ID', defaultValue: 'ec2-ssh-key', description: 'Jenkins SSH private key credential ID for Docker host deployments.')
        string(name: 'REMOTE_APP_DIR', defaultValue: '/opt/ogfashions', description: 'Remote application checkout directory for Docker deployments.')
        string(name: 'DOCKER_ENV_REMOTE_PATH', defaultValue: '/opt/ogfashions/deploy/docker/docker.env', description: 'Remote Docker env file path.')
        string(name: 'DEPLOY_BASE_URL', defaultValue: 'https://ogstyles.store', description: 'Browser-testable base URL after deployment.')
        string(name: 'NOTIFY_EMAIL', defaultValue: 'contact.ogfashions@gmail.com', description: 'Build and deployment notification recipients.')
        string(name: 'CONTAINER_REGISTRY', defaultValue: 'docker.io', description: 'Container registry host. Examples: docker.io, nexus.company.com:5000, company.jfrog.io/docker-local.')
        string(name: 'CONTAINER_IMAGE_REPO', defaultValue: 'yarrav7', description: 'Container image namespace or repository owner. Example: yarrav7.')
        string(name: 'NEXUS_MAVEN_REPO_URL', defaultValue: 'http://your-nexus-host:8081/repository/maven-releases/', description: 'Nexus hosted Maven repository URL.')
        string(name: 'ARTIFACTORY_MAVEN_REPO_URL', defaultValue: 'https://your-artifactory-host/artifactory/libs-release-local/', description: 'Artifactory Maven repository URL.')
    }

    environment {
        APP_NAME = 'ogfashions'
        REGISTRY = 'docker.io'
        IMAGE_REPO = 'yarrav7'
        DOCKER_BUILDKIT = '1'
        MAVEN_ARGS = '-q'

        EC2_SSH_HOST = '44.211.170.188'
        EC2_SSH_USER = 'ubuntu'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Initialize Metadata') {
            steps {
                script {
                    env.REGISTRY = params.CONTAINER_REGISTRY?.trim() ?: env.REGISTRY
                    def configuredRepo = params.CONTAINER_IMAGE_REPO?.trim() ?: env.IMAGE_REPO?.trim()
                    if (!configuredRepo || configuredRepo == 'your-dockerhub-or-ecr-user') {
                        error('Set CONTAINER_IMAGE_REPO to the registry namespace before building images.')
                    }

                    env.IMAGE_REPO = configuredRepo
                    env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-backend"
                    env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-frontend"
                    def commitSuffix = (env.GIT_COMMIT ?: 'local').take(8)
                    env.IMAGE_TAG = "build-${env.BUILD_NUMBER}-${commitSuffix}"
                    env.EFFECTIVE_IMAGE_TAG = params.ROLLBACK_IMAGE_TAG?.trim() ? params.ROLLBACK_IMAGE_TAG.trim() : env.IMAGE_TAG
                    env.ARTIFACT_VERSION = env.EFFECTIVE_IMAGE_TAG
                    env.IS_ROLLBACK = params.ROLLBACK_IMAGE_TAG?.trim() ? 'true' : 'false'
                    env.REMOTE_REPO_URL = env.GIT_URL ?: 'https://github.com/DevopsProOrg/ecommerce-project.git'
                    env.EC2_SSH_HOST = params.EC2_SSH_HOST?.trim() ?: env.EC2_SSH_HOST
                    env.EC2_SSH_USER = params.EC2_SSH_USER?.trim() ?: env.EC2_SSH_USER
                    env.EC2_SSH_CREDENTIALS_ID = params.EC2_SSH_CREDENTIALS_ID?.trim() ?: 'ec2-ssh-key'
                    env.DEPLOY_BASE_URL = params.DEPLOY_BASE_URL?.trim() ?: 'https://ogstyles.store'
                    env.DEPLOY_LEVEL = params.DEPLOY_LEVEL != 'none' ? params.DEPLOY_LEVEL : (params.DEPLOY_TARGET != 'none' ? params.K8S_OVERLAY : 'none')
                    env.IS_RELEASE_BRANCH = (env.BRANCH_NAME == 'release' || (env.BRANCH_NAME ?: '').startsWith('release/')) ? 'true' : 'false'
                    env.IS_MAINLINE = ['main', 'master'].contains(env.BRANCH_NAME ?: '') ? 'true' : 'false'
                    env.HAS_DEPLOY_REQUEST = (params.DEPLOY_TARGET != 'none' && params.DEPLOY_LEVEL != 'none') ? 'true' : 'false'
                    env.IS_PRODUCTION_LEVEL = (env.DEPLOY_LEVEL == 'lp') ? 'true' : 'false'
                    env.IS_PREPROD_LEVEL = (env.DEPLOY_LEVEL == 'l6') ? 'true' : 'false'
                    env.ALLOW_IMAGE_PUSH = (env.IS_ROLLBACK == 'true') ? 'false' : ((env.HAS_DEPLOY_REQUEST == 'true' || env.IS_MAINLINE == 'true' || env.IS_RELEASE_BRANCH == 'true') ? 'true' : 'false')
                    currentBuild.description = "${params.DEPLOY_TARGET}/${env.DEPLOY_LEVEL} tag=${env.EFFECTIVE_IMAGE_TAG}${env.IS_ROLLBACK == 'true' ? ' existing image' : ''}"
                }
            }
        }

        stage('Prepare Release Notes') {
            steps {
                script {
                    def maxCommits = params.RELEASE_NOTES_MAX_COMMITS?.trim() ?: '15'
                    def previousCommit = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT?.trim()
                    def currentCommit = env.GIT_COMMIT?.trim() ?: 'HEAD'
                    def notes = previousCommit ?
                        captureCmd(
                            "git log --pretty=format:'- %h %s' ${previousCommit}..${currentCommit}",
                            "@echo off\r\ngit log --pretty=format:\"- %%h %%s\" ${previousCommit}..${currentCommit}"
                        ) :
                        captureCmd(
                            "git log -n ${maxCommits} --pretty=format:'- %h %s' ${currentCommit}",
                            "@echo off\r\ngit log -n ${maxCommits} --pretty=format:\"- %%h %%s\" ${currentCommit}"
                        )

                    env.RELEASE_NOTES = notes?.trim() ? notes.trim() : 'No release notes available.'
                    writeFile file: 'release-notes.txt', text: env.RELEASE_NOTES + '\n'
                }
            }
        }

        stage('Validate Deployment Parameters') {
            when {
                expression { return params.DEPLOY_TARGET != 'none' }
            }
            steps {
                script {
                    if (params.DEPLOY_LEVEL == 'none') {
                        error('DEPLOY_LEVEL must be set when DEPLOY_TARGET is not none.')
                    }
                    if ((params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both') && params.K8S_OVERLAY != params.DEPLOY_LEVEL) {
                        error("K8S_OVERLAY (${params.K8S_OVERLAY}) must match DEPLOY_LEVEL (${params.DEPLOY_LEVEL}) for Kubernetes deployments.")
                    }
                    if (env.IS_PRODUCTION_LEVEL == 'true' && !params.CHANGE_TICKET?.trim()) {
                        error('CHANGE_TICKET is required for lp production deployments.')
                    }
                }
            }
        }

        stage('Compile Backend') {
            when {
                expression { return env.IS_ROLLBACK != 'true' }
            }
            steps {
                dir('backend') {
                    script {
                        runCmd("mvn ${env.MAVEN_ARGS} -DskipTests compile", "mvn %MAVEN_ARGS% -DskipTests compile")
                    }
                }
            }
        }

        stage('Unit Tests') {
            when {
                expression { return env.IS_ROLLBACK != 'true' }
            }
            steps {
                dir('backend') {
                    script {
                        runCmd("mvn ${env.MAVEN_ARGS} test", "mvn %MAVEN_ARGS% test")
                    }
                }
            }
        }

        stage('Frontend Validation') {
            when {
                expression { return env.IS_ROLLBACK != 'true' }
            }
            steps {
                script {
                    runCmd('node --check frontend/js/admin.js', 'node --check frontend\\js\\admin.js')
                    runCmd('node --check frontend/js/auth.js', 'node --check frontend\\js\\auth.js')
                    runCmd('node --check frontend/js/app-config.js', 'node --check frontend\\js\\app-config.js')
                    runCmd('node --check frontend/js/app.js', 'node --check frontend\\js\\app.js')
                    runCmd('node --check frontend/js/products.js', 'node --check frontend\\js\\products.js')
                    runCmd('node --check frontend/js/cart.js', 'node --check frontend\\js\\cart.js')
                    runCmd('node --check frontend/js/ui.js', 'node --check frontend\\js\\ui.js')
                    runCmd('node --check frontend/js/wishlist.js', 'node --check frontend\\js\\wishlist.js')
                }
            }
        }

        stage('Package Backend') {
            when {
                expression { return env.IS_ROLLBACK != 'true' }
            }
            steps {
                dir('backend') {
                    script {
                        runCmd("mvn ${env.MAVEN_ARGS} -DskipTests package", "mvn %MAVEN_ARGS% -DskipTests package")
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { return params.RUN_SONAR && env.IS_ROLLBACK != 'true' }
            }
            steps {
                script {
                    def scannerStatus = isUnix() ?
                        sh(script: 'command -v sonar-scanner >/dev/null 2>&1', returnStatus: true) :
                        bat(script: 'where sonar-scanner >nul 2>nul', returnStatus: true)
                    if (scannerStatus == 0) {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            runCmd(
                                'sonar-scanner -Dsonar.token="$SONAR_TOKEN"',
                                'sonar-scanner -Dsonar.token=%SONAR_TOKEN%'
                        )
                        }
                    } else {
                        echo 'Sonar scanner is not installed on this agent; skipping SonarQube analysis.'
                    }
                }
            }
        }

        stage('Trivy Filesystem Scan') {
            when {
                expression { return params.RUN_TRIVY && env.IS_ROLLBACK != 'true' }
            }
            steps {
                script {
                    runCmd('trivy fs --severity HIGH,CRITICAL --exit-code 1 .')
                }
            }
        }

        stage('Build Docker Images') {
            when {
                expression { return env.IS_ROLLBACK != 'true' }
            }
            steps {
                script {
                    runCmd("docker build -t ${env.BACKEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG} backend")
                    runCmd("docker build -t ${env.FRONTEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG} frontend")
                }
            }
        }

        stage('Trivy Image Scan') {
            when {
                expression { return params.RUN_TRIVY && env.IS_ROLLBACK != 'true' }
            }
            steps {
                script {
                    runCmd("trivy image --severity HIGH,CRITICAL --exit-code 1 ${env.BACKEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}")
                    runCmd("trivy image --severity HIGH,CRITICAL --exit-code 1 ${env.FRONTEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}")
                }
            }
        }

        stage('Publish Maven Artifact To Nexus') {
            when {
                expression { return (params.RUN_NEXUS_PUBLISH || params.ARTIFACT_REPOSITORY == 'nexus') && env.IS_ROLLBACK != 'true' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                    dir('backend') {
                        script {
                            def repoUrl = params.NEXUS_MAVEN_REPO_URL?.trim() ?: env.NEXUS_MAVEN_REPO_URL
                            if (!repoUrl || repoUrl.contains('your-nexus-host')) {
                                error('Set NEXUS_MAVEN_REPO_URL before publishing to Nexus.')
                            }
                            writeFile file: 'ci-maven-settings.xml', text: """
                            <settings>
                              <servers>
                                <server>
                                  <id>nexus</id>
                                  <username>${env.NEXUS_USER}</username>
                                  <password>${env.NEXUS_PASS}</password>
                                </server>
                              </servers>
                            </settings>
                            """.stripIndent().trim()
                            runCmd("mvn -s ci-maven-settings.xml -Drevision=${env.ARTIFACT_VERSION} -DskipTests deploy -DaltDeploymentRepository=nexus::default::${repoUrl}")
                        }
                    }
                }
            }
        }

        stage('Publish Maven Artifact To Artifactory') {
            when {
                expression { return params.ARTIFACT_REPOSITORY == 'artifactory' && env.IS_ROLLBACK != 'true' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactory-creds', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASS')]) {
                    dir('backend') {
                        script {
                            def repoUrl = params.ARTIFACTORY_MAVEN_REPO_URL?.trim() ?: env.ARTIFACTORY_MAVEN_REPO_URL
                            if (!repoUrl || repoUrl.contains('your-artifactory-host')) {
                                error('Set ARTIFACTORY_MAVEN_REPO_URL before publishing to Artifactory.')
                            }
                            writeFile file: 'ci-maven-settings.xml', text: """
                            <settings>
                              <servers>
                                <server>
                                  <id>artifactory</id>
                                  <username>${env.ARTIFACTORY_USER}</username>
                                  <password>${env.ARTIFACTORY_PASS}</password>
                                </server>
                              </servers>
                            </settings>
                            """.stripIndent().trim()
                            runCmd("mvn -s ci-maven-settings.xml -Drevision=${env.ARTIFACT_VERSION} -DskipTests deploy -DaltDeploymentRepository=artifactory::default::${repoUrl}")
                        }
                    }
                }
            }
        }

        stage('Push Images To Registry') {
            when {
                expression { return env.ALLOW_IMAGE_PUSH == 'true' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        runCmd('echo "$DOCKER_PASS" | docker login "$REGISTRY" -u "$DOCKER_USER" --password-stdin')
                        runCmd("docker push ${env.BACKEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}")
                        runCmd("docker push ${env.FRONTEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}")
                    }
                }
            }
        }

        stage('Deploy Selected Non-Prod Environment') {
            when {
                expression { return env.HAS_DEPLOY_REQUEST == 'true' && env.IS_PRODUCTION_LEVEL != 'true' }
            }
            parallel {
                stage('Docker Non-Prod') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        sshagent(credentials: [env.EC2_SSH_CREDENTIALS_ID]) {
                            script {
                                def remoteDir = params.REMOTE_APP_DIR?.trim() ?: '/opt/ogfashions'
                                def remoteParent = remoteDir.contains('/') ? remoteDir.substring(0, remoteDir.lastIndexOf('/')) : '.'
                                def remoteEnvPath = params.DOCKER_ENV_REMOTE_PATH?.trim() ?: "${remoteDir}/deploy/docker/docker.env"
                                def remoteCommand = """
                                set -e
                                mkdir -p '${remoteParent}'
                                if [ ! -d '${remoteDir}/.git' ]; then
                                  if [ -d '${remoteDir}' ] && find '${remoteDir}' -mindepth 1 -maxdepth 1 -print -quit | grep -q .; then
                                    echo 'Remote app dir exists but is not a git checkout: ${remoteDir}'
                                    exit 1
                                  fi
                                  rmdir '${remoteDir}' 2>/dev/null || true
                                  git clone '${env.REMOTE_REPO_URL}' '${remoteDir}'
                                fi
                                cd '${remoteDir}'
                                git fetch origin '${env.BRANCH_NAME}' --prune
                                git checkout -f '${env.GIT_COMMIT ?: 'FETCH_HEAD'}' || git checkout -f FETCH_HEAD
                                if [ ! -f '${remoteEnvPath}' ]; then
                                  echo 'Missing remote env file: ${remoteEnvPath}'
                                  exit 1
                                fi
                                chmod +x deploy/docker/*.sh deploy/scripts/*.sh
                                IMAGE_TAG='${env.EFFECTIVE_IMAGE_TAG}' BACKEND_IMAGE='${env.BACKEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}' FRONTEND_IMAGE='${env.FRONTEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}' BUILD_IMAGES_LOCALLY=false bash ./deploy/docker/ec2-registry-deploy.sh '${remoteEnvPath}'
                                """.stripIndent().trim()
                                runCmd("ssh -o StrictHostKeyChecking=no ${env.EC2_SSH_USER}@${env.EC2_SSH_HOST} \"${remoteCommand.replace('"', '\\\"')}\"")
                            }
                        }
                    }
                }
                stage('K8s Non-Prod') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        withCredentials([file(credentialsId: 'kubeconfig-credential', variable: 'KUBECONFIG_FILE')]) {
                            script {
                                runCmd("KUBECONFIG='${env.KUBECONFIG_FILE}' CONTAINER_REGISTRY='${env.REGISTRY}' bash deploy/k8s/scripts/deploy.sh ${env.DEPLOY_LEVEL} ${env.IMAGE_REPO} ${env.EFFECTIVE_IMAGE_TAG} '${env.DEPLOY_BASE_URL}'")
                            }
                        }
                    }
                }
            }
        }

        stage('Production Approval') {
            when {
                expression { return env.HAS_DEPLOY_REQUEST == 'true' && env.IS_PRODUCTION_LEVEL == 'true' }
            }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    script {
                        env.APPROVED_BY = input(
                            message: "Approve production deployment for ${env.BRANCH_NAME} build ${env.BUILD_NUMBER} to ${env.DEPLOY_LEVEL}?",
                            ok: 'Deploy',
                            submitterParameter: 'APPROVED_BY'
                        )
                    }
                }
            }
        }

        stage('Deploy Production') {
            when {
                expression { return env.HAS_DEPLOY_REQUEST == 'true' && env.IS_PRODUCTION_LEVEL == 'true' }
            }
            parallel {
                stage('Docker Production') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        sshagent(credentials: [env.EC2_SSH_CREDENTIALS_ID]) {
                            script {
                                def remoteDir = params.REMOTE_APP_DIR?.trim() ?: '/opt/ogfashions'
                                def remoteParent = remoteDir.contains('/') ? remoteDir.substring(0, remoteDir.lastIndexOf('/')) : '.'
                                def remoteEnvPath = params.DOCKER_ENV_REMOTE_PATH?.trim() ?: "${remoteDir}/deploy/docker/docker.env"
                                def remoteCommand = """
                                set -e
                                mkdir -p '${remoteParent}'
                                if [ ! -d '${remoteDir}/.git' ]; then
                                  if [ -d '${remoteDir}' ] && find '${remoteDir}' -mindepth 1 -maxdepth 1 -print -quit | grep -q .; then
                                    echo 'Remote app dir exists but is not a git checkout: ${remoteDir}'
                                    exit 1
                                  fi
                                  rmdir '${remoteDir}' 2>/dev/null || true
                                  git clone '${env.REMOTE_REPO_URL}' '${remoteDir}'
                                fi
                                cd '${remoteDir}'
                                git fetch origin '${env.BRANCH_NAME}' --prune
                                git checkout -f '${env.GIT_COMMIT ?: 'FETCH_HEAD'}' || git checkout -f FETCH_HEAD
                                if [ ! -f '${remoteEnvPath}' ]; then
                                  echo 'Missing remote env file: ${remoteEnvPath}'
                                  exit 1
                                fi
                                chmod +x deploy/docker/*.sh deploy/scripts/*.sh
                                IMAGE_TAG='${env.EFFECTIVE_IMAGE_TAG}' BACKEND_IMAGE='${env.BACKEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}' FRONTEND_IMAGE='${env.FRONTEND_IMAGE}:${env.EFFECTIVE_IMAGE_TAG}' BUILD_IMAGES_LOCALLY=false bash ./deploy/docker/ec2-registry-deploy.sh '${remoteEnvPath}'
                                """.stripIndent().trim()
                                runCmd("ssh -o StrictHostKeyChecking=no ${env.EC2_SSH_USER}@${env.EC2_SSH_HOST} \"${remoteCommand.replace('"', '\\\"')}\"")
                            }
                        }
                    }
                }
                stage('K8s Production') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        withCredentials([file(credentialsId: 'kubeconfig-credential', variable: 'KUBECONFIG_FILE')]) {
                            script {
                                runCmd("KUBECONFIG='${env.KUBECONFIG_FILE}' CONTAINER_REGISTRY='${env.REGISTRY}' bash deploy/k8s/scripts/deploy.sh ${env.DEPLOY_LEVEL} ${env.IMAGE_REPO} ${env.EFFECTIVE_IMAGE_TAG} '${env.DEPLOY_BASE_URL}'")
                            }
                        }
                    }
                }
            }
        }

        stage('Post Deploy Smoke Check') {
            when {
                expression { return env.HAS_DEPLOY_REQUEST == 'true' }
            }
            steps {
                script {
                    echo "Frontend URL: ${env.DEPLOY_BASE_URL}/"
                    echo "Frontend health URL: ${env.DEPLOY_BASE_URL}/healthz"
                    echo "Backend health URL: ${env.DEPLOY_BASE_URL}/api/actuator/health"
                    runCmd("bash deploy/scripts/http-smoke.sh '${env.DEPLOY_BASE_URL}'")
                }
            }
        }

        stage('Record Deployment Summary') {
            steps {
                script {
                    def summaryLines = [
                        "Job: ${env.JOB_NAME}",
                        "Build: #${env.BUILD_NUMBER}",
                        "Branch: ${env.BRANCH_NAME ?: 'n/a'}",
                        "Deploy target: ${params.DEPLOY_TARGET}",
                        "Deploy level: ${env.DEPLOY_LEVEL ?: 'n/a'}",
                        "K8s overlay: ${params.K8S_OVERLAY}",
                        "Existing image reuse: ${env.IS_ROLLBACK ?: 'false'}",
                        "Image tag: ${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
                        "Effective image tag: ${env.EFFECTIVE_IMAGE_TAG ?: 'n/a'}",
                        "Change ticket: ${params.CHANGE_TICKET ?: 'n/a'}",
                        "Approved by: ${env.APPROVED_BY ?: 'n/a'}",
                        "Base URL: ${env.DEPLOY_BASE_URL ?: 'n/a'}",
                        "Build URL: ${env.BUILD_URL ?: 'n/a'}"
                    ]

                    writeFile file: 'deployment-summary.txt', text: summaryLines.join('\n') + '\n'
                    currentBuild.description = "${params.DEPLOY_TARGET}/${env.DEPLOY_LEVEL} tag=${env.EFFECTIVE_IMAGE_TAG}${env.IS_ROLLBACK == 'true' ? ' rollback' : ''}${env.APPROVED_BY ? " by ${env.APPROVED_BY}" : ''}"
                }
            }
        }
    }

    post {
        success {
            script {
                sendBuildNotification('SUCCESS')
            }
        }
        failure {
            script {
                sendBuildNotification('FAILURE')
            }
        }
        unstable {
            script {
                sendBuildNotification('UNSTABLE')
            }
        }
        aborted {
            script {
                sendBuildNotification('ABORTED')
            }
        }
        always {
            archiveArtifacts artifacts: 'deployment-summary.txt,release-notes.txt', allowEmptyArchive: true, onlyIfSuccessful: false
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
