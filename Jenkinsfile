pipeline {
    agent any

    environment {
        DISCORD_WEBHOOK = credentials('discord-webhook-url')
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
                    junit 'build/test-results/test/*.xml'
                }
            }
        }
    }

    post {
        success {
            script {
                def message = """{"embeds": [{"title": "빌드 성공", "color": 3066993, "fields": [{"name": "Job", "value": "${env.JOB_NAME}", "inline": true}, {"name": "Branch", "value": "${env.BRANCH_NAME}", "inline": true}, {"name": "Build", "value": "#${env.BUILD_NUMBER}", "inline": true}]}]}"""
                sh "curl -X POST -H 'Content-Type: application/json' -d '${message}' ${DISCORD_WEBHOOK}"
            }
        }
        failure {
            script {
                def message = """{"embeds": [{"title": "빌드 실패", "color": 15158332, "fields": [{"name": "Job", "value": "${env.JOB_NAME}", "inline": true}, {"name": "Branch", "value": "${env.BRANCH_NAME}", "inline": true}, {"name": "링크", "value": "[로그 확인](${env.BUILD_URL})"}]}]}"""
                sh "curl -X POST -H 'Content-Type: application/json' -d '${message}' ${DISCORD_WEBHOOK}"
            }
        }
        always {
            cleanWs()
        }
    }
}