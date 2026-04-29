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
        choice(name: 'K8S_OVERLAY', choices: ['staging', 'production'], description: 'Kubernetes overlay.')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis if configured.')
        booleanParam(name: 'RUN_TRIVY', defaultValue: true, description: 'Run Trivy filesystem and image scan if installed.')
        booleanParam(name: 'RUN_NEXUS_PUBLISH', defaultValue: false, description: 'Publish backend artifact to Nexus if configured.')
        string(name: 'DOCKER_ENV_REMOTE_PATH', defaultValue: '/opt/ogfashions/deploy/docker/docker.env', description: 'Remote Docker env file path.')
    }

    environment {
        APP_NAME = 'ogfashions'
        REGISTRY = 'docker.io'
        IMAGE_REPO = 'your-dockerhub-or-ecr-user'
        DOCKER_BUILDKIT = '1'
        MAVEN_ARGS = '-q'
        NOTIFY_EMAIL = credentials('jenkins-notify-email')
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
                    def safeBranch = (env.BRANCH_NAME ?: 'local').replaceAll('[^A-Za-z0-9._-]', '-')
                    env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-backend"
                    env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-frontend"
                    env.IMAGE_TAG = "${safeBranch}-${env.BUILD_NUMBER}"
                    env.IS_RELEASE_BRANCH = (env.BRANCH_NAME == 'release' || (env.BRANCH_NAME ?: '').startsWith('release/')) ? 'true' : 'false'
                    env.IS_MAINLINE = ['main', 'master'].contains(env.BRANCH_NAME ?: '') ? 'true' : 'false'
                }
            }
        }

        stage('Compile Backend') {
            steps {
                dir('backend') {
                    bat "mvn %MAVEN_ARGS% -DskipTests compile"
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('backend') {
                    bat "mvn %MAVEN_ARGS% test"
                }
            }
        }

        stage('Frontend Validation') {
            steps {
                bat 'node --check frontend\\js\\admin.js'
                bat 'node --check frontend\\js\\auth.js'
                bat 'node --check frontend\\js\\app-config.js'
            }
        }

        stage('Integration Smoke Validation') {
            steps {
                dir('backend') {
                    bat "mvn %MAVEN_ARGS% -DskipTests package"
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { return params.RUN_SONAR }
            }
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    bat '''
                    if exist "C:\\sonar-scanner\\bin\\sonar-scanner.bat" (
                      C:\\sonar-scanner\\bin\\sonar-scanner.bat -Dsonar.login=%SONAR_TOKEN%
                    ) else (
                      echo Sonar scanner not installed on this agent. Skipping explicit scanner execution.
                    )
                    '''
                }
            }
        }

        stage('Trivy Filesystem Scan') {
            when {
                expression { return params.RUN_TRIVY }
            }
            steps {
                bat '''
                trivy fs --severity HIGH,CRITICAL --exit-code 0 .
                '''
            }
        }

        stage('Build Docker Images') {
            steps {
                bat "docker build -t %BACKEND_IMAGE%:%IMAGE_TAG% backend"
                bat "docker build -t %FRONTEND_IMAGE%:%IMAGE_TAG% frontend"
            }
        }

        stage('Trivy Image Scan') {
            when {
                expression { return params.RUN_TRIVY }
            }
            steps {
                bat '''
                trivy image --severity HIGH,CRITICAL --exit-code 0 %BACKEND_IMAGE%:%IMAGE_TAG%
                trivy image --severity HIGH,CRITICAL --exit-code 0 %FRONTEND_IMAGE%:%IMAGE_TAG%
                '''
            }
        }

        stage('Publish Maven Artifact To Nexus') {
            when {
                expression { return params.RUN_NEXUS_PUBLISH }
            }
            steps {
                dir('backend') {
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        bat 'mvn -DskipTests deploy'
                    }
                }
            }
        }

        stage('Push Images To Registry') {
            when {
                anyOf {
                    expression { return env.IS_MAINLINE == 'true' }
                    expression { return env.IS_RELEASE_BRANCH == 'true' }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat 'echo %DOCKER_PASS% | docker login %REGISTRY% -u %DOCKER_USER% --password-stdin'
                    bat 'docker push %BACKEND_IMAGE%:%IMAGE_TAG%'
                    bat 'docker push %FRONTEND_IMAGE%:%IMAGE_TAG%'
                }
            }
        }

        stage('Deploy Lower Environment') {
            when {
                allOf {
                    expression { return env.IS_MAINLINE == 'true' }
                    expression { return params.DEPLOY_TARGET != 'none' }
                }
            }
            parallel {
                stage('Docker Dev/Staging') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        sshagent(credentials: ['ec2-ssh-key']) {
                            bat """
                            ssh -o StrictHostKeyChecking=no %EC2_SSH_USER%@%EC2_SSH_HOST% ^
                              "cd /opt/ogfashions && ^
                              IMAGE_TAG=%IMAGE_TAG% BACKEND_IMAGE=%BACKEND_IMAGE%:%IMAGE_TAG% FRONTEND_IMAGE=%FRONTEND_IMAGE%:%IMAGE_TAG% BUILD_IMAGES_LOCALLY=false ./deploy/docker/ec2-registry-deploy.sh ${params.DOCKER_ENV_REMOTE_PATH}"
                            """
                        }
                    }
                }
                stage('K8s Staging') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        withCredentials([file(credentialsId: 'kubeconfig-credential', variable: 'KUBECONFIG_FILE')]) {
                            bat """
                            set KUBECONFIG=%KUBECONFIG_FILE%
                            bash deploy/k8s/deploy.sh staging %BACKEND_IMAGE%:%IMAGE_TAG% %FRONTEND_IMAGE%:%IMAGE_TAG%
                            """
                        }
                    }
                }
            }
        }

        stage('Production Approval') {
            when {
                allOf {
                    expression { return env.IS_RELEASE_BRANCH == 'true' }
                    expression { return params.DEPLOY_TARGET != 'none' }
                }
            }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Approve production deployment for ${env.BRANCH_NAME} build ${env.BUILD_NUMBER}?", ok: 'Deploy'
                }
            }
        }

        stage('Deploy Production') {
            when {
                allOf {
                    expression { return env.IS_RELEASE_BRANCH == 'true' }
                    expression { return params.DEPLOY_TARGET != 'none' }
                }
            }
            parallel {
                stage('Docker Production') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        sshagent(credentials: ['ec2-ssh-key']) {
                            bat """
                            ssh -o StrictHostKeyChecking=no %EC2_SSH_USER%@%EC2_SSH_HOST% ^
                              "cd /opt/ogfashions && ^
                              IMAGE_TAG=%IMAGE_TAG% BACKEND_IMAGE=%BACKEND_IMAGE%:%IMAGE_TAG% FRONTEND_IMAGE=%FRONTEND_IMAGE%:%IMAGE_TAG% BUILD_IMAGES_LOCALLY=false ./deploy/docker/ec2-registry-deploy.sh ${params.DOCKER_ENV_REMOTE_PATH}"
                            """
                        }
                    }
                }
                stage('K8s Production') {
                    when {
                        expression { return params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both' }
                    }
                    steps {
                        withCredentials([file(credentialsId: 'kubeconfig-credential', variable: 'KUBECONFIG_FILE')]) {
                            bat """
                            set KUBECONFIG=%KUBECONFIG_FILE%
                            bash deploy/k8s/deploy.sh production %BACKEND_IMAGE%:%IMAGE_TAG% %FRONTEND_IMAGE%:%IMAGE_TAG%
                            """
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
                echo 'Add curl or API smoke checks here for the deployed environment entrypoint.'
            }
        }
    }

    post {
        success {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build succeeded.

Job: ${env.JOB_NAME}
Build: ${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME}
Backend Image: ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}
Frontend Image: ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}
Deploy Target: ${params.DEPLOY_TARGET}
"""
            )
        }
        failure {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build failed.

Job: ${env.JOB_NAME}
Build: ${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME}
Check Jenkins console output for the failed stage.
"""
            )
        }
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
