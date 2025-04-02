pipeline {
    agent any

    environment {
        TOMCAT_HOME = "/opt/tomcat10"
        WAR_NAME = "JobManagement.war"
        DEPLOY_DIR = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE = "/home/ubuntu"  // Store WAR before deploying
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
            }
        }
        
        stage('Compile') {
            steps {
                sh 'mkdir -p build/WEB-INF/classes'
                sh '''
                   javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")
                   '''
            }
        }

        stage('Copy Resources') {
            steps {
                sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                sh 'cp -R src/main/webapp/* build/'
            }
        }
        
        stage('Package WAR') {
            steps {
                sh 'jar -cvf ${WAR_NAME} -C build .'
                sh 'mv ${WAR_NAME} ${WAR_STORAGE}/'  // Save WAR in /home/ubuntu
                archiveArtifacts artifacts: "${WAR_STORAGE}/${WAR_NAME}", fingerprint: true
            }
        }
        
        stage('Deploy') {
            steps {
                sh """
                    sudo mv ${WAR_STORAGE}/${WAR_NAME} ${DEPLOY_DIR}/ 
                    sudo /opt/tomcat10/bin/shutdown.sh
                    sudo /opt/tomcat10/bin/startup.sh
                """
            }
        }
    }

    post {
        failure {
            echo 'Build or deployment failed.'
        }
        success {
            echo 'Build and deployment succeeded.'
        }
    }
}
