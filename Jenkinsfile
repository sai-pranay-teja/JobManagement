// Global variables to capture overall metrics
def pipelineStartTime = 0
def leadTimeForChanges = 0
// rollbackTime is declared elsewhere; do not redeclare it here
def rollbackTime = "N/A"

pipeline {
    agent any

    environment {
        // Tomcat and deployment variables
        TOMCAT_HOME          = "/opt/tomcat10"
        WAR_NAME             = "JobManagement_JENKINS.war"
        DEPLOY_DIR           = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE          = "${WORKSPACE}"  // WAR built in workspace

        // Log file variables
        RESOURCE_BEFORE_LOG  = "${WORKSPACE}/resource_before_usage.log"
        RESOURCE_AFTER_LOG   = "${WORKSPACE}/resource_after_usage.log"
        LOG_FILE             = "${WORKSPACE}/deployment.log"
        DEPLOYMENT_TIME_FILE = "${WORKSPACE}/deployment_time.log"
        ROLLBACK_LOG         = "${WORKSPACE}/rollback.log"

        // Memory usage log files in human readable format
        MEM_BEFORE_LOG       = "${WORKSPACE}/mem_before.log"
        MEM_AFTER_LOG        = "${WORKSPACE}/mem_after.log"

        // Test results log file
        TEST_RESULTS_LOG     = "${WORKSPACE}/test_results.log"

        // SSH variables for accessing the server as root
        SSH_KEY              = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER             = "root"
        SSH_HOST             = "18.60.83.220"
        SSH_OPTS             = "-o StrictHostKeyChecking=no"
        BACKUP_DIR = "/home/ubuntu/jenkins_bak"
    }

    stages {

        // stage('Clean Workspace') {
        //     steps {
        //         // Clean the workspace to ensure a fresh start
        //         cleanWs()
        //     }
        // }
        stage('Clean Workspace') {
            steps {
        // Clean workspace but retain the backups directory
               cleanWs(
                deleteDirs: true,
                patterns: [[pattern: 'backups/**', type: 'EXCLUDE']]
        )
      }
    }

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
                // sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")'
                sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'

                sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                // sh 'cp -R src/main/webapp/* build/'
                sh 'cp -R src/main/webapp/. build/'  // Note the dot
                sh 'ls -l build/ > build_contents.log'

                sh 'mkdir -p build/logs'
                sh 'cp -R src/main/webapp/logs/* build/logs/ || true'
                sh 'jar -cvf ${WAR_NAME} -C build .'
            }
        }



        stage('Backup WAR') {
            steps {
                script {
                    // Create a backup of the WAR file in the workspace if it exists.
                    if (fileExists("${WAR_STORAGE}/${WAR_NAME}")) {
                        // sh "mkdir -p ${BACKUP_DIR}"
                        // sh "cp ${WAR_STORAGE}/${WAR_NAME} ${BACKUP_DIR}/${WAR_NAME}_bak"
                        // echo "Backup created: ${BACKUP_DIR}/${WAR_NAME}_bak"
                              def backupDir = "/tmp/jenkins_bak"
                              def warFile = "${WAR_STORAGE}/${WAR_NAME}"
                              def backupFile = "${backupDir}/${WAR_NAME}_bak"
                              sh "mkdir -p ${backupDir}"
                              sh "cp ${warFile} ${backupFile}"
                              echo "Backup saved: ${backupFile}"
                        // archiveArtifacts artifacts: "${WAR_STORAGE}/${WAR_NAME}_bak", fingerprint: true
                    } else {
                        echo "ERROR: WAR file ${WAR_NAME} not found; backup not created."
                    }
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh """
                    mkdir -p ${WORKSPACE}/test_output
                    # Compile tests from src/main/test to test_output
                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output \$(find ${WORKSPACE}/src/main/test -name "*.java")
                    # Run tests and redirect both stdout and stderr to the TEST_RESULTS_LOG.
                    java -cp "${WORKSPACE}/test_output:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output --details summary > ${TEST_RESULTS_LOG} 2>&1 || true
                """
                script {
                    // Read the entire test results log and display it
                    def testResults = readFile(TEST_RESULTS_LOG).trim()
                    echo "Test Results Summary:\n${testResults}"
                }
            }
        }

        stage('Measure Resource Usage Before Deployment') {
            steps {
                sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_BEFORE_LOG}"
                // Capture memory usage in human readable format
                sh "free -h > ${MEM_BEFORE_LOG}"
            }
        }

        stage('Deploy and Restart Tomcat') {
            steps {
                script {
                    // Record deployment start time
                    def deployStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()

                    // Calculate Lead Time for Changes: (difference between the last commit timestamp and deployment start time)
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
${TOMCAT_HOME}/bin/catalina.sh stop || true
${TOMCAT_HOME}/bin/catalina.sh start
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
                sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_AFTER_LOG}"
                // Capture memory usage in human readable format after deployment
                sh "free -h > ${MEM_AFTER_LOG}"
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
                def BackupFile="/tmp/jenkins_bak"
                // In rollback, check if the backup file exists. If yes, use it; otherwise, capture compile error.
                if (fileExists("${BackupFile}/${WAR_NAME}_bak")) {
                    sh """
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${BackupFile}/${WAR_NAME}"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp /tmp/jenkins_bak/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/catalina.sh stop || true
${TOMCAT_HOME}/bin/catalina.sh start
exit
EOF
                    """
                } else {
                    // If backup not found, run a compile to capture the error output
                    echo "Backup file ${WAR_NAME}_bak not found. Capturing compile error..."
                    // sh "javac src/main/java/model/Job.java 2> ${WORKSPACE}/compile_error.log || true"
                    def compileError = readFile("${WORKSPACE}/compile_error.log").trim()
                    echo "Compile error captured:\n${compileError}"
                }
                def rollbackEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                def computedRollbackTime = rollbackEndTime - rollbackStartTime
                sh "echo \"Rollback took ${computedRollbackTime} seconds.\" >> ${ROLLBACK_LOG}"
                echo "Rollback completed in ${computedRollbackTime} seconds."
            }
        }
        // Always display metrics even if the job fails
        always {
            script {
                def pipelineEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                def totalPipelineTime = pipelineEndTime - pipelineStartTime

                def deploymentTime = fileExists(DEPLOYMENT_TIME_FILE) ? readFile(DEPLOYMENT_TIME_FILE).trim() : "N/A"
                def memBefore = fileExists(MEM_BEFORE_LOG) ? readFile(MEM_BEFORE_LOG).trim() : "N/A"
                def memAfter = fileExists(MEM_AFTER_LOG) ? readFile(MEM_AFTER_LOG).trim() : "N/A"
                def resourceUsageBefore = fileExists(RESOURCE_BEFORE_LOG) ? readFile(RESOURCE_BEFORE_LOG).trim() : "N/A"
                def resourceUsageAfter = fileExists(RESOURCE_AFTER_LOG) ? readFile(RESOURCE_AFTER_LOG).trim() : "N/A"
                
                rollbackTime = "N/A"
                if (fileExists(ROLLBACK_LOG)) {
                    def rollbackContent = readFile(ROLLBACK_LOG).trim()
                    rollbackTime = rollbackContent.replaceAll("[^0-9]", "").isEmpty() ? "N/A" : rollbackContent.replaceAll("[^0-9]", "")
                }
                
                def testSummary = "N/A"
                if (fileExists(TEST_RESULTS_LOG)) {
                    def testResults = readFile(TEST_RESULTS_LOG).trim()
                    def summaryLines = testResults.readLines().findAll { it.toLowerCase().contains("tests") }
                    testSummary = summaryLines ? summaryLines.join(" | ") : "N/A"
                }
                
                echo ""
                echo "-------------------------------------------------"
                echo "              CI/CD Metrics Summary              "
                echo "-------------------------------------------------"
                echo String.format("| %-35s | %-15s |", "Metric", "Value")
                echo "-------------------------------------------------"
                echo String.format("| %-35s | %-15s |", "Total Pipeline Time (sec)", totalPipelineTime)
                def deployTimeValue = deploymentTime.tokenize().find { it.isNumber() } ?: "N/A"
                echo String.format("| %-35s | %-15s |", "Deployment Time (sec)", deployTimeValue)
                echo String.format("| %-35s | %-15s |", "Lead Time for Changes (sec)", leadTimeForChanges)
                // echo String.format("| %-35s | %-15s |", "Rollback Time (sec)", rollbackTime)
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
                echo ""
                echo "Resource Usage BEFORE Deployment (vmstat):"
                echo "-------------------------------------------------"
                echo resourceUsageBefore
                echo "-------------------------------------------------"
                echo "Resource Usage AFTER Deployment (vmstat):"
                echo "-------------------------------------------------"
                echo resourceUsageAfter
                echo "-------------------------------------------------"
            }
        }
    }
}