pipeline {
    agent any

    environment {
        // Adjust these values based on your environment
        TOMCAT_HOME = "/opt/tomcat10"
        WAR_NAME = "JobManagement.war"
        DEPLOY_DIR = "${TOMCAT_HOME}/webapps"
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout code from GitHub
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
            }
        }
        
        stage('Compile') {
            steps {
                // Create build directories
                sh 'mkdir -p build/WEB-INF/classes'
                
                // Compile Java source files using JDK21 and include required libraries
                sh '''
                   javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")
                   '''
            }
        }
        
        stage('Copy Resources') {
            steps {
                // Copy non-Java resources (JSP, config.properties, etc.)
                sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                sh 'cp -R src/main/webapp/* build/'
            }
        }
        
        stage('Package WAR') {
            steps {
                // Package everything from build/ into a WAR file
                sh 'jar -cvf ${WAR_NAME} -C build .'
                archiveArtifacts artifacts: "${WAR_NAME}", fingerprint: true
            }
        }
        
        stage('Deploy') {
            steps {
                // Since Jenkins is running on the same server, simply move the WAR file to Tomcat's deployment directory and restart Tomcat
                sh """
                   mv ${WAR_NAME} ${DEPLOY_DIR}/ && \
                   cd ${TOMCAT_HOME}/bin && ./shutdown.sh && ./startup.sh
                   """
            }
        }
        
        stage('Smoke Test') {
            steps {
                // Simple test using curl to verify that the login page is available
                sh 'curl -o /dev/null -s -w "Response Time: %{time_total}s\\n" http://18.61.31.57:8080/JobManagement/login.jsp'
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
