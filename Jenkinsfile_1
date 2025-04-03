// Global variable to capture the pipeline start time
def pipelineStartTime = 0

pipeline {
    agent any

    environment {
        // Tomcat and deployment variables
        TOMCAT_HOME          = "/opt/tomcat10"
        WAR_NAME             = "JobManagement.war"
        DEPLOY_DIR           = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE          = "${WORKSPACE}"  // WAR built in workspace

        // Log file variables
        RESOURCE_LOG         = "${WORKSPACE}/resource_usage.log"
        LOG_FILE             = "${WORKSPACE}/deployment.log"
        DEPLOYMENT_TIME_FILE = "${WORKSPACE}/deployment_time.log"
        ROLLBACK_LOG         = "${WORKSPACE}/rollback.log"

        // SSH variables for accessing localhost as root
        SSH_KEY              = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER             = "root"
        SSH_HOST             = "localhost"
        SSH_OPTS             = "-o StrictHostKeyChecking=no"
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    // Record the pipeline start time for automation overhead metrics
                    pipelineStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    echo "Pipeline start time recorded: ${pipelineStartTime}"
                }
            }
        }

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
                    // Record deployment start time
                    def deployStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()

                    // Calculate Lead Time for Changes:
                    // (difference between the last commit timestamp and deployment start time)
                    def commitTime = sh(script: "git log -1 --format=%ct", returnStdout: true).trim().toInteger()
                    def leadTime = deployStartTime - commitTime
                    echo "Lead Time for Changes (time from last commit to deployment start): ${leadTime} seconds"

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

                    // Record deployment end time and calculate deployment duration
                    def deployEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def deployDuration = deployEndTime - deployStartTime
                    sh "echo \"Deployment took ${deployDuration} seconds.\" >> ${DEPLOYMENT_TIME_FILE}"
                    echo "Deployment completed in ${deployDuration} seconds."
                }
            }
        }

        stage('Measure Resource Usage After Deployment') {
            steps {
                sh "echo 'Resource usage after deployment:' >> ${RESOURCE_LOG}"
                sh "vmstat 1 5 >> ${RESOURCE_LOG}"
            }
        }

        stage('Display Metrics') {
            steps {
                script {
                    // Record the pipeline end time
                    def pipelineEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def totalPipelineTime = pipelineEndTime - pipelineStartTime

                    echo "------- CI/CD Metrics Summary -------"
                    echo "Total Pipeline Time (Automation Overhead): ${totalPipelineTime} seconds"
                    echo ""
                    echo "Deployment Time:"
                    sh "cat ${DEPLOYMENT_TIME_FILE}"
                    echo ""
                    echo "Resource Utilization Log:"
                    sh "cat ${RESOURCE_LOG}"
                    echo "-------------------------------------"
                }
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
                def rollbackStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
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
                def rollbackEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                def rollbackDuration = rollbackEndTime - rollbackStartTime
                sh "echo \"Rollback took ${rollbackDuration} seconds.\" >> ${ROLLBACK_LOG}"
                echo "Rollback completed in ${rollbackDuration} seconds."
                // Optionally, display rollback log
                sh "echo 'Rollback Log:'; cat ${ROLLBACK_LOG}"
            }
        }
    }
}
