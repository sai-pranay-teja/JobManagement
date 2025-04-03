// Global variables to capture overall metrics
def pipelineStartTime = 0
def leadTimeForChanges = 0
// rollbackTime is declared elsewhere; do not redeclare it here
rollbackTime = "N/A"

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

        // Memory usage log files in human readable format
        MEM_BEFORE_LOG       = "${WORKSPACE}/mem_before.log"
        MEM_AFTER_LOG        = "${WORKSPACE}/mem_after.log"

        // Test results log file
        TEST_RESULTS_LOG     = "${WORKSPACE}/test_results.log"

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

        stage('Run Unit Tests') {
            steps {
                sh """
                    mkdir -p ${WORKSPACE}/test_output
                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output \$(find ${WORKSPACE}/src/main/test -name "*.java")
                    java -cp "${WORKSPACE}/test_output:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path > ${TEST_RESULTS_LOG}
                """
                script {
                    def testResults = sh(script: "grep -E 'Tests run:' ${TEST_RESULTS_LOG}", returnStdout: true).trim()
                    echo "Test Results Summary:\n${testResults}"
                }
            }
        }

        stage('Measure Resource Usage Before Deployment') {
            steps {
                sh "echo 'Resource usage before deployment:' >> ${RESOURCE_LOG}"
                sh "vmstat 1 5 >> ${RESOURCE_LOG}"
                // Capture memory usage in human readable format
                sh "free -h > ${MEM_BEFORE_LOG}"
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
                    leadTimeForChanges = deployStartTime - commitTime
                    echo "Lead Time for Changes (time from last commit to deployment start): ${leadTimeForChanges} seconds"

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
                // Capture memory usage in human readable format after deployment
                sh "free -h > ${MEM_AFTER_LOG}"
            }
        }

        stage('Display Metrics') {
            steps {
                script {
                    // Record the pipeline end time
                    def pipelineEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def totalPipelineTime = pipelineEndTime - pipelineStartTime

                    // Read deployment time from file
                    def deploymentTime = readFile(DEPLOYMENT_TIME_FILE).trim()

                    // Read memory usage logs
                    def memBefore = readFile(MEM_BEFORE_LOG).trim()
                    def memAfter  = readFile(MEM_AFTER_LOG).trim()

                    // Handle rollback time (Avoid redeclaration)
                    rollbackTime = "N/A"
                    if (fileExists(ROLLBACK_LOG)) {
                        def rollbackContent = readFile(ROLLBACK_LOG).trim()
                        rollbackTime = rollbackContent.replaceAll("[^0-9]", "").isEmpty() ? "N/A" : rollbackContent.replaceAll("[^0-9]", "")
                    }

                    // Read Test Summary from test results log
                    def testSummary = "N/A"
                    if (fileExists(TEST_RESULTS_LOG)) {
                        def testResults = readFile(TEST_RESULTS_LOG).trim()
                        def summaryLines = testResults.readLines().findAll { it.contains("Tests run:") || it.contains("Failures:") }
                        testSummary = summaryLines ? summaryLines.join(" | ") : "N/A"
                    }

                    echo ""
                    echo "-------------------------------------------------"
                    echo "              CI/CD Metrics Summary              "
                    echo "-------------------------------------------------"
                    echo String.format("| %-35s | %-15s |", "Metric", "Value")
                    echo "-------------------------------------------------"
                    echo String.format("| %-35s | %-15s |", "Total Pipeline Time (sec)", totalPipelineTime)
                    
                    // Extract deployment time value from file (if multiple lines exist, use first numeric occurrence)
                    def deployTimeValue = deploymentTime.tokenize().find { it.isNumber() } ?: "N/A"
                    echo String.format("| %-35s | %-15s |", "Deployment Time (sec)", deployTimeValue)
                    echo String.format("| %-35s | %-15s |", "Lead Time for Changes (sec)", leadTimeForChanges)
                    echo String.format("| %-35s | %-15s |", "Rollback Time (sec)", rollbackTime)
                    echo String.format("| %-35s | %-15s |", "Test Summary", testSummary)
                    echo "-------------------------------------------------"
                    echo ""
                    echo "Memory Usage BEFORE Deployment (free -h):"
                    echo "-------------------------------------------------"
                    echo memBefore
                    echo "-------------------------------------------------"
                    echo "Memory Usage AFTER Deployment (free -h):"
                    echo "-------------------------------------------------"
                    echo memAfter
                    echo "-------------------------------------------------"
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
                def computedRollbackTime = rollbackEndTime - rollbackStartTime
                sh "echo \"Rollback took ${computedRollbackTime} seconds.\" >> ${ROLLBACK_LOG}"
                echo "Rollback completed in ${computedRollbackTime} seconds."
            }
        }
    }
    
}
