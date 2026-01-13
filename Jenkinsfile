pipeline {
    agent any

    environment {
        DOCKER_HUB = credentials('docker-hub-credentials')
        DOCKER_USERNAME = "${DOCKER_HUB_USR}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('Frontend Build') {
            tools {
                nodejs 'NodeJS-20'
            }
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('API Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh "echo ${DOCKER_HUB_PSW} | docker login -u ${DOCKER_HUB_USR} --password-stdin"

                sh "docker build -t ${DOCKER_USERNAME}/voteland-backend:latest ."
                sh "docker push ${DOCKER_USERNAME}/voteland-backend:latest"

                dir('frontend') {
                    sh "docker build -t ${DOCKER_USERNAME}/voteland-frontend:latest ."
                    sh "docker push ${DOCKER_USERNAME}/voteland-frontend:latest"
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }
    }

    post {
        success {
            node('') {
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK')]) {
                    sh '''
                        curl -X POST -H "Content-Type: application/json" -d '{
                            "embeds": [{
                                "title": "빌드 성공 ✅",
                                "color": 3066993,
                                "fields": [
                                    {"name": "Job", "value": "''' + env.JOB_NAME + '''", "inline": true},
                                    {"name": "Branch", "value": "''' + env.BRANCH_NAME + '''", "inline": true},
                                    {"name": "Build", "value": "#''' + env.BUILD_NUMBER + '''", "inline": true}
                                ]
                            }]
                        }' "$DISCORD_WEBHOOK"
                    '''
                }
            }
        }
        failure {
            node('') {
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK')]) {
                    sh '''
                        curl -X POST -H "Content-Type: application/json" -d '{
                            "embeds": [{
                                "title": "빌드 실패 ❌",
                                "color": 15158332,
                                "fields": [
                                    {"name": "Job", "value": "''' + env.JOB_NAME + '''", "inline": true},
                                    {"name": "Branch", "value": "''' + env.BRANCH_NAME + '''", "inline": true},
                                    {"name": "링크", "value": "[로그 확인](''' + env.BUILD_URL + ''')"}
                                ]
                            }]
                        }' "$DISCORD_WEBHOOK"
                    '''
                }
            }
        }
        always {
            node('') {
                cleanWs()
            }
        }
    }
}