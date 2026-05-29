pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-northeast-2'
        ECR_REPO = '357148912264.dkr.ecr.ap-northeast-2.amazonaws.com/manna-backend'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew bootJar'
            }
        }

        stage('Docker Build & Push to ECR') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY

                        aws ecr get-login-password --region $AWS_REGION \
                          | docker login --username AWS --password-stdin $ECR_REPO

                        docker build -t $ECR_REPO:latest .
                        docker push $ECR_REPO:latest
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker stop manna-backend || true
                    docker rm manna-backend || true
                    docker run -d --name manna-backend \
                      -p 8081:8080 \
                      --restart unless-stopped \
                      --env-file /home/ubuntu/.env \
                      -e SPRING_PROFILES_ACTIVE=production \
                      $ECR_REPO:latest
                '''
            }
        }
    }

    post {
        success {
            echo '✅ 배포 성공!'
        }
        failure {
            echo '❌ 배포 실패!'
        }
    }
}