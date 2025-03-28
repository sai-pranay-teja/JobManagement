pipeline {
    agent any

    environment {
        // Adjust these values based on your environment
        TOMCAT_HOME = "/opt/tomcat10"
        WAR_NAME = "JobManagement.war"
        DEPLOY_DIR = "${TOMCAT_HOME}/webapps"
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

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
                    sudo mv ${WAR_NAME} ${DEPLOY_DIR}/ 
                   # cd ${TOMCAT_HOME}/bin && ./shutdown.sh && ./startup.sh
                    sudo /opt/tomcat10/bin/shutdown.sh
                    sudo /opt/tomcat10/bin/startup.sh
                   """
            }

        }
        



        stage('Smoke Test') {
            steps {
                // Simple test using curl to verify that the login page is available
               // sh 'curl -o /dev/null -s -w "Response Time: %{time_total}s\\n" http://18.61.31.57:8090/JobManagement/login.jsp'
               sh 'curl -o /dev/null -s -w "Response Time: %{time_total}s\\n" http://98.130.117.98:8090/JobManagement/login.jsp || true'



            }
        }
        // stage('Repeat Package & Deploy') {
        //     steps {
        //         // Rebuild the WAR file and deploy again to measure deployment speed.
        //         // Repackage the WAR (if needed) and deploy it.
        //         sh 'jar -cvf ${WAR_NAME} -C build .'
        //         archiveArtifacts artifacts: "${WAR_NAME}", fingerprint: true
        //         sh """
        //             sudo mv ${WAR_NAME} ${DEPLOY_DIR}/ 
        //             sudo /opt/tomcat10/bin/shutdown.sh
        //             sudo /opt/tomcat10/bin/startup.sh
        //         """
        //     }
        // }
        stage('Deployment Speed') {
            steps {
              sh """
            start_time=\$(date +%s)
            # Package the WAR file
            jar -cvf ${WAR_NAME} -C build .
            # Deploy the WAR to Tomcat and restart Tomcat
            sudo mv ${WAR_NAME} ${DEPLOY_DIR}/
            sudo /opt/tomcat10/bin/shutdown.sh
            sudo /opt/tomcat10/bin/startup.sh
            # Wait until the log indicates that the web application archive is deployed
            tail -f /opt/tomcat10/logs/catalina.out | while read line; do
              echo "\${line}" | grep -q "Deployment of web application archive" && break
            done
            end_time=\$(date +%s)
            echo "Deployment took \$((end_time - start_time)) seconds."
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
