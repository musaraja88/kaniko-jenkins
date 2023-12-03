pipeline {

  agent {
    kubernetes {
      yamlFile 'kaniko-builder.yaml'
    }
  }

  environment {
        APP_NAME = "java-test"
        RELEASE = "1.0.0"

        DOCKER_CREDENTIALS = credentials('docker-cred')
        DOCKER_USER = "${DOCKER_CREDENTIALS_USR}"
        DOCKER_PASS = "${DOCKER_CREDENTIALS_PSW}"
        IMAGE_NAME = "${DOCKER_USER}" + "/" + "${APP_NAME}"
        IMAGE_TAG = "${RELEASE}-${BUILD_NUMBER}"
        SONARQUBE_URL = "https://sonarqube.192.168.1.220.nip.io/"
        SONARQUBE_TOKEN = credentials('sonar-token')

    }

  stages {

    stage("Cleanup Workspace") {
      steps {
        cleanWs()
      }
    }

    stage("Checkout from SCM"){
        steps {
            git branch: 'main', credentialsId: 'github', url: 'https://github.com/musaraja88/kaniko-jenkins'
        }

    }

    stage('SonarQube Analysis') {
        steps {
            script {
                def scannerHome = tool 'SonarQube Scanner'
                withSonarQubeEnv('SonarQube') {
                    sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${APP_NAME} -Dsonar.projectName=${APP_NAME} -Dsonar.sources=src"
                }
            }
        }
    }

    stage('Build & Push with Kaniko') {
      steps {
        container(name: 'kaniko', shell: '/busybox/sh') {
          sh 'pwd'
          sh '''#!/busybox/sh
            /kaniko/executor --dockerfile `pwd`/Dockerfile --context `pwd` --destination=${IMAGE_NAME}:${IMAGE_TAG} --destination=${IMAGE_NAME}:latest
          '''
        }
      }
    }
  }
}