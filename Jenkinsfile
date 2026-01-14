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
                script {
                    sh "echo ${DOCKER_HUB_PSW} | docker login -u ${DOCKER_HUB_USR} --password-stdin"

                    // dev 브랜치일 때만 dev-숫자 태그로 빌드 및 푸시
                    if (env.BRANCH_NAME == 'dev') {
                        sh "docker build -t ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER}"

                        dir('frontend') {
                            sh "docker build -t ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER} ."
                            sh "docker push ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER}"
                        }
                    }
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Update K8s Manifest') {
            when {
                branch 'dev'
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'coffiiness', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PWD')]) {
                        sh 'rm -rf k8s-repo || true'
                        sh "git clone https://${GIT_USER}:${GIT_PWD}@github.com/coffiiness/voteland-k8s-repo.git k8s-repo"
                    }

                    dir('k8s-repo') {
                        sh "git config user.email 'jenkins@voteland.com'"
                        sh "git config user.name 'Jenkins Bot'"

                        sh "git checkout dev"

                        sh "sed -i 's|image: .*/voteland-backend:.*|image: ${DOCKER_USERNAME}/voteland-backend:dev-${env.BUILD_NUMBER}|g' k8s/backend/deployment.yaml"

                        sh "sed -i 's|image: .*/voteland-frontend:.*|image: ${DOCKER_USERNAME}/voteland-frontend:dev-${env.BUILD_NUMBER}|g' k8s/frontend/deployment.yaml"

                        // 변경사항 커밋 & 푸시
                        sh '''
                            if [ -n "$(git status --porcelain)" ]; then
                                git add .
                                git commit -m "Update image tag to dev-${BUILD_NUMBER}"
                                git push origin dev
                            else
                                echo "No changes to commit"
                            fi
                        '''
                    }
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
                                    {"name": "Image Tag", "value": "dev-''' + env.BUILD_NUMBER + '''", "inline": true}
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
                                    {"name": "Link", "value": "[로그 확인](''' + env.BUILD_URL + ''')"}
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