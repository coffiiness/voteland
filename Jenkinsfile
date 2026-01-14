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

                // dev 브랜치일 때 버전 태그(dev-숫자)를 같이 생성
                sh "docker build -t ${DOCKER_USERNAME}/voteland-backend:latest ."
                sh "docker tag ${DOCKER_USERNAME}/voteland-backend:latest ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER}"
                sh "docker push ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER}"

                dir('frontend') {
                    sh "docker build -t ${DOCKER_USERNAME}/voteland-frontend:latest ."
                    sh "docker tag ${DOCKER_USERNAME}/voteland-frontend:latest ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER}"
                    sh "docker push ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER}"
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }
    }

        stage('Update K8s Manifest') {
                    when {
                        branch 'dev'
                    }
                    steps {
                        script {
                            sh "git config user.email 'jenkins@voteland.com'"
                            sh "git config user.name 'Jenkins Bot'"

                            withCredentials([usernamePassword(credentialsId: 'github-token-id', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PWD')]) {
                                sh "git clone https://${GIT_USER}:${GIT_PWD}@github.com/coffiiness/voteland-k8s-repo.git k8s-repo"
                            }

                            dir('k8s-repo') {
                                sh "git checkout dev"

                                // (:dev 태그를 :dev-빌드번호 로 변경)
                                sh "sed -i 's|image: .*/voteland-backend:.*|image: ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER}|g' k8s/backend/deployment.yaml"
                                sh "sed -i 's|image: .*/voteland-frontend:.*|image: ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER}|g' k8s/frontend/deployment.yaml"

                                sh "git add ."
                                sh "git diff --staged --quiet || git commit -m 'Update image tag to dev-${env.BUILD_NUMBER}'"
                                sh "git push origin dev"
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
                                "title": "빌드 & 배포 업데이트 성공 ✅",
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