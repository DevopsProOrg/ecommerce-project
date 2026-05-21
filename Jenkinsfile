def runCmd(String unixCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh unixCommand
    } else {
        bat(windowsCommand ?: unixCommand)
    }
}

def sendBuildNotification(String status) {
    def recipients = (env.NOTIFY_EMAIL ?: '').trim()
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
        "Backend image: ${env.BACKEND_IMAGE ?: 'n/a'}:${env.IMAGE_TAG ?: 'n/a'}",
        "Frontend image: ${env.FRONTEND_IMAGE ?: 'n/a'}:${env.IMAGE_TAG ?: 'n/a'}",
        "Base URL: ${env.DEPLOY_BASE_URL ?: 'n/a'}",
        "Build URL: ${env.BUILD_URL ?: 'n/a'}"
    ]

    emailext(
        to: recipients,
        subject: "${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${env.BRANCH_NAME ?: 'n/a'}]",
        body: lines.join('\n')
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
        choice(name: 'DEPLOY_LEVEL', choices: ['none', 'l1', 'l2', 'l3', 'l6', 'lp'], description: 'Choose deployment environment level. Use none for validation-only builds.')
        choice(name: 'K8S_OVERLAY', choices: ['l1', 'l2', 'l3', 'l6', 'lp'], description: 'Choose Kubernetes overlay.')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis if configured.')
        booleanParam(name: 'RUN_TRIVY', defaultValue: true, description: 'Run Trivy filesystem and image scans if installed.')
        booleanParam(name: 'RUN_NEXUS_PUBLISH', defaultValue: false, description: 'Publish backend artifact to Nexus if configured.')
        string(name: 'REMOTE_APP_DIR', defaultValue: '/opt/ogfashions', description: 'Remote application checkout directory for Docker deployments.')
        string(name: 'DOCKER_ENV_REMOTE_PATH', defaultValue: '/opt/ogfashions/deploy/docker/docker.env', description: 'Remote Docker env file path.')
        string(name: 'DEPLOY_BASE_URL', defaultValue: 'https://ogstyles.store', description: 'Browser-testable base URL after deployment.')
    }

    environment {
        APP_NAME = 'ogfashions'
        REGISTRY = 'docker.io'
        IMAGE_REPO = 'yarrav7'
        DOCKER_BUILDKIT = '1'
        MAVEN_ARGS = '-q'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Initialize Metadata') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_REGISTRY_USER', passwordVariable: 'DOCKER_REGISTRY_PASS')]) {
                    script {
                        def safeBranch = (env.BRANCH_NAME ?: 'local').replaceAll('[^A-Za-z0-9._-]', '-')
                        def configuredRepo = env.IMAGE_REPO?.trim()
                        if (!configuredRepo || configuredRepo == 'your-dockerhub-or-ecr-user') {
                            configuredRepo = env.DOCKER_REGISTRY_USER
                        }

                        env.IMAGE_REPO = configuredRepo
                        env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-backend"
                        env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-frontend"
                        env.IMAGE_TAG = "${safeBranch}-${env.BUILD_NUMBER}"
                        env.REMOTE_REPO_URL = env.GIT_URL ?: 'https://github.com/DevopsProOrg/ecommerce-project.git'
                        env.DEPLOY_BASE_URL = params.DEPLOY_BASE_URL?.trim() ?: 'https://ogstyles.store'
                        env.DEPLOY_LEVEL = params.DEPLOY_LEVEL != 'none' ? params.DEPLOY_LEVEL : (params.DEPLOY_TARGET != 'none' ? params.K8S_OVERLAY : 'none')
                        env.IS_RELEASE_BRANCH = (env.BRANCH_NAME == 'release' || (env.BRANCH_NAME ?: '').startsWith('release/')) ? 'true' : 'false'
                        env.IS_MAINLINE = ['main', 'master'].contains(env.BRANCH_NAME ?: '') ? 'true' : 'false'
                        env.HAS_DEPLOY_REQUEST = (params.DEPLOY_TARGET != 'none' && params.DEPLOY_LEVEL != 'none') ? 'true' : 'false'
                        env.IS_PRODUCTION_LEVEL = (env.DEPLOY_LEVEL == 'lp') ? 'true' : 'false'
                        env.IS_PREPROD_LEVEL = (env.DEPLOY_LEVEL == 'l6') ? 'true' : 'false'
                        env.ALLOW_IMAGE_PUSH = (env.HAS_DEPLOY_REQUEST == 'true' || env.IS_MAINLINE == 'true' || env.IS_RELEASE_BRANCH == 'true') ? 'true' : 'false'
                    }
                }
            }
        }

        stage('Compile Backend') {
            steps {
                dir('backend') {
                    script {
                        runCmd("mvn ${env.MAVEN_ARGS} -DskipTests compile", "mvn %MAVEN_ARGS% -DskipTests compile")
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('backend') {
                    script {
                        runCmd("mvn ${env.MAVEN_ARGS} test", "mvn %MAVEN_ARGS% test")
                    }
                }
            }
        }

        stage('Frontend Validation') {
            steps {
                script {
                    runCmd('node --check frontend/js/admin.js', 'node --check frontend\\js\\admin.js')
                    runCmd('node --check frontend/js/auth.js', 'node --check frontend\\js\\auth.js')
                    runCmd('node --check frontend/js/app-config.js', 'node --check frontend\\js\\app-config.js')
                }
            }
        }

        stage('Package Backend') {
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
                expression { return params.RUN_SONAR }
            }
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    script {
                        runCmd(
                            '''
                            if command -v sonar-scanner >/dev/null 2>&1; then
                              sonar-scanner -Dsonar.login="$SONAR_TOKEN"
                            else
                              echo "Sonar scanner not installed on this agent. Skipping explicit scanner execution."
                            fi
                            '''.stripIndent().trim()
                        )
                    }
                }
            }
        }

        stage('Trivy Filesystem Scan') {
            when {
                expression { return params.RUN_TRIVY }
            }
            steps {
                script {
                    runCmd('trivy fs --severity HIGH,CRITICAL --exit-code 0 .')
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    runCmd("docker build -t ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} backend")
                    runCmd("docker build -t ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG} frontend")
                }
            }
        }

        stage('Trivy Image Scan') {
            when {
                expression { return params.RUN_TRIVY }
            }
            steps {
                script {
                    runCmd("trivy image --severity HIGH,CRITICAL --exit-code 0 ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}")
                    runCmd("trivy image --severity HIGH,CRITICAL --exit-code 0 ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}")
                }
            }
        }

        stage('Publish Maven Artifact To Nexus') {
            when {
                expression { return params.RUN_NEXUS_PUBLISH }
            }
            steps {
                dir('backend') {
                    script {
                        runCmd('mvn -DskipTests deploy')
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
                        runCmd("docker push ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}")
                        runCmd("docker push ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}")
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
                        sshagent(credentials: ['ec2-ssh-key']) {
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
                                IMAGE_TAG='${env.IMAGE_TAG}' BACKEND_IMAGE='${env.BACKEND_IMAGE}:${env.IMAGE_TAG}' FRONTEND_IMAGE='${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}' BUILD_IMAGES_LOCALLY=false ./deploy/docker/ec2-registry-deploy.sh '${remoteEnvPath}'
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
                                runCmd("KUBECONFIG='${env.KUBECONFIG_FILE}' bash deploy/k8s/scripts/deploy.sh ${env.DEPLOY_LEVEL} ${env.IMAGE_REPO} ${env.IMAGE_TAG} '${env.DEPLOY_BASE_URL}'")
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
                    input message: "Approve production deployment for ${env.BRANCH_NAME} build ${env.BUILD_NUMBER} to ${env.DEPLOY_LEVEL}?", ok: 'Deploy'
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
                        sshagent(credentials: ['ec2-ssh-key']) {
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
                                IMAGE_TAG='${env.IMAGE_TAG}' BACKEND_IMAGE='${env.BACKEND_IMAGE}:${env.IMAGE_TAG}' FRONTEND_IMAGE='${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}' BUILD_IMAGES_LOCALLY=false ./deploy/docker/ec2-registry-deploy.sh '${remoteEnvPath}'
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
                                runCmd("KUBECONFIG='${env.KUBECONFIG_FILE}' bash deploy/k8s/scripts/deploy.sh ${env.DEPLOY_LEVEL} ${env.IMAGE_REPO} ${env.IMAGE_TAG} '${env.DEPLOY_BASE_URL}'")
                            }
                        }
                    }
                }
            }
        }

        stage('Post Deploy Smoke Check') {
            when {
                expression { return params.DEPLOY_TARGET != 'none' }
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
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
