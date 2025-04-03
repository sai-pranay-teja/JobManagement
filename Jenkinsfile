pipeline {
    agent any

    environment {
        // Tomcat and deployment variables
        TOMCAT_HOME         = "/opt/tomcat10"
        WAR_NAME            = "JobManagement.war"
        DEPLOY_DIR          = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE         = "${WORKSPACE}"  // WAR built in workspace

        // Log file variables
        RESOURCE_LOG        = "${WORKSPACE}/resource_usage.log"
        LOG_FILE            = "${WORKSPACE}/deployment.log"
        DEPLOYMENT_TIME_FILE= "${WORKSPACE}/deployment_time.log"
        ROLLBACK_LOG        = "${WORKSPACE}/rollback.log"

        // SSH variables for accessing localhost as root
        SSH_KEY             = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER            = "root"
        SSH_HOST            = "localhost"
        SSH_OPTS            = "-o StrictHostKeyChecking=no"
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

        stage('Measure Resource Usage Before Deployment') {
            steps {
                sh "echo 'Resource usage before deployment:' >> ${RESOURCE_LOG}"
                sh "vmstat 1 5 >> ${RESOURCE_LOG}"
            }
        }

        stage('Deploy and Restart Tomcat') {
            steps {
                script {
                    def start_time = sh(script: "date +%s", returnStdout: true).trim()
                    
                    // Transfer the WAR file and restart Tomcat via SSH
                    sh """
                        echo "Starting deployment at \$(date)" >> ${LOG_FILE}
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_STORAGE}/${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF

                        # Wait until a deployment message is logged
                        tail -f ${TOMCAT_HOME}/logs/catalina.out | while read line; do
                          echo "\${line}" | grep -q "Deployment of web application archive" && break;
                        done
                    """

                    def end_time = sh(script: "date +%s", returnStdout: true).trim()
                    def deploy_time = end_time.toInteger() - start_time.toInteger()
                    sh "echo \"Deployment took ${deploy_time} seconds.\" >> ${DEPLOYMENT_TIME_FILE}"
                    echo "Deployment completed in ${deploy_time} seconds."
                }
            }
        }

        stage('Measure Resource Usage After Deployment') {
            steps {
                sh "echo 'Resource usage after deployment:' >> ${RESOURCE_LOG}"
                sh "vmstat 1 5 >> ${RESOURCE_LOG}"
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed! Performing rollback...'
            script {
                def start_time = sh(script: "date +%s", returnStdout: true).trim()
                sh """
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${DEPLOY_DIR}/${WAR_NAME}"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp ${WAR_STORAGE}/${WAR_NAME}.backup ${DEPLOY_DIR}/"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF
                """
                def end_time = sh(script: "date +%s", returnStdout: true).trim()
                def rollback_time = end_time.toInteger() - start_time.toInteger()
                sh "echo \"Rollback took ${rollback_time} seconds.\" >> ${ROLLBACK_LOG}"
                echo "Rollback completed in ${rollback_time} seconds."
            }
        }
    }
}
