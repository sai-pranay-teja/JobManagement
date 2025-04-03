pipeline {
    agent any

    environment {
        TOMCAT_HOME = "/opt/tomcat10"
        WAR_NAME = "JobManagement.war"
        DEPLOY_DIR = "${TOMCAT_HOME}/webapps"
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
            }
        }

        stage('Build WAR') {
            steps {
                sh 'mkdir -p build/WEB-INF/classes'
                sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")'
                sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                sh 'cp -R src/main/webapp/* build/'
                sh 'jar -cvf ${WAR_NAME} -C build .'
            }
        }

        stage('Deploy Locally') {
            steps {
                sh """
                    mv ${WAR_NAME} ${DEPLOY_DIR}/
                    ${TOMCAT_HOME}/bin/shutdown.sh || true
                    ${TOMCAT_HOME}/bin/startup.sh
                """
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
