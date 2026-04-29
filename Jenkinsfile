pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['none', 'docker', 'k8s', 'both'], description: 'Choose where to deploy after image push.')
        choice(name: 'K8S_OVERLAY', choices: ['staging', 'production'], description: 'Kubernetes overlay to deploy.')
        string(name: 'DOCKER_ENV_REMOTE_PATH', defaultValue: '/opt/ogfashions/deploy/docker/docker.env', description: 'Remote Docker env file path on EC2 host.')
    }

    environment {
        APP_NAME = 'ogfashions'
        REGISTRY = 'docker.io'
        IMAGE_REPO = 'your-dockerhub-or-ecr-user'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Initialize Build Metadata') {
            steps {
                script {
                    def safeBranch = (env.BRANCH_NAME ?: 'local').replaceAll('[^A-Za-z0-9._-]', '-')
                    env.BACKEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-backend"
                    env.FRONTEND_IMAGE = "${env.REGISTRY}/${env.IMAGE_REPO}/ogfashions-frontend"
                    env.IMAGE_TAG = "${safeBranch}-${env.BUILD_NUMBER}"
                }
            }
        }

        stage('Validate Backend') {
            steps {
                dir('backend') {
                    bat 'mvn -q -DskipTests compile'
                }
            }
        }

        stage('Validate Frontend Scripts') {
            steps {
                bat 'node --check frontend\\js\\admin.js'
                bat 'node --check frontend\\js\\auth.js'
                bat 'node --check frontend\\js\\app-config.js'
            }
        }

        stage('Build Images') {
            steps {
                bat "docker build -t %BACKEND_IMAGE%:%IMAGE_TAG% backend"
                bat "docker build -t %FRONTEND_IMAGE%:%IMAGE_TAG% frontend"
            }
        }

        stage('Push Images') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'release'
                    expression { return env.BRANCH_NAME?.startsWith('release/') }
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

        stage('Deploy Docker') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'master'
                        branch 'release'
                        expression { return env.BRANCH_NAME?.startsWith('release/') }
                    }
                    expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                }
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

        stage('Deploy Kubernetes') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'master'
                        branch 'release'
                        expression { return env.BRANCH_NAME?.startsWith('release/') }
                    }
                    expression { return params.DEPLOY_TARGET == 'k8s' || params.DEPLOY_TARGET == 'both' }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-credential', variable: 'KUBECONFIG_FILE')]) {
                    bat """
                    set KUBECONFIG=%KUBECONFIG_FILE%
                    bash deploy/k8s/deploy.sh ${params.K8S_OVERLAY} %BACKEND_IMAGE%:%IMAGE_TAG% %FRONTEND_IMAGE%:%IMAGE_TAG%
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
