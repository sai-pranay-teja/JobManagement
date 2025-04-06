// Global variables to capture overall metrics
def pipelineStartTime = 0
def leadTimeForChanges = 0
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
        // File to record rollback deployment time
        ROLLBACK_DEPLOYMENT_TIME_FILE = "${WORKSPACE}/rollback_deployment_time.log"

        // Memory usage log files in human readable format
        MEM_BEFORE_LOG       = "${WORKSPACE}/mem_before.log"
        MEM_AFTER_LOG        = "${WORKSPACE}/mem_after.log"

        // Test results log file
        TEST_RESULTS_LOG     = "${WORKSPACE}/test_results.log"

        // SSH variables for accessing the server as root
        SSH_KEY              = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER             = "root"
        SSH_HOST             = "40.192.68.176"
        SSH_OPTS             = "-o StrictHostKeyChecking=no"
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Initialize') {
            steps {
                script {
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

        stage('Backup WAR') {
            steps {
                script {
                    if (fileExists("${WAR_STORAGE}/${WAR_NAME}")) {
                        sh "cp ${WAR_STORAGE}/${WAR_NAME} ${WAR_STORAGE}/${WAR_NAME}_BACKUP"
                        echo "Backup created: ${WAR_STORAGE}/${WAR_NAME}_BACKUP"
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
                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output \$(find ${WORKSPACE}/src/main/test -name "*.java")
                    java -cp "${WORKSPACE}/test_output:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output --details summary > ${TEST_RESULTS_LOG} 2>&1 || true
                """
                script {
                    def testResults = readFile(TEST_RESULTS_LOG).trim()
                    echo "Test Results Summary:\n${testResults}"
                }
            }
        }

        stage('Measure Resource Usage Before Deployment') {
            steps {
                sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_BEFORE_LOG}"
                sh "free -h > ${MEM_BEFORE_LOG}"
            }
        }

        stage('Deploy and Restart Tomcat') {
            steps {
                script {
                    // Store deployment start time in env variable so it can be used later even if job fails
                    env.DEPLOY_START_TIME = sh(script: "date +%s", returnStdout: true).trim()
                    def commitTime = sh(script: "git log -1 --format=%ct", returnStdout: true).trim().toInteger()
                    def deployStartTime = env.DEPLOY_START_TIME.toInteger()
                    leadTimeForChanges = deployStartTime - commitTime
                    echo "Lead Time for Changes (sec): ${leadTimeForChanges}"
                    
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

                        tail -f ${TOMCAT_HOME}/logs/catalina.out | while read line; do
                          echo "\${line}" | grep -q "Deployment of web application archive" && break;
                        done
                    """

                    // Record deployment end time and save it in env
                    env.DEPLOY_END_TIME = sh(script: "date +%s", returnStdout: true).trim()
                    def deployDuration = env.DEPLOY_END_TIME.toInteger() - deployStartTime
                    sh "echo \"Deployment took ${deployDuration} seconds.\" >> ${DEPLOYMENT_TIME_FILE}"
                    echo "Deployment completed in ${deployDuration} seconds."
                }
            }
        }

        stage('Measure Resource Usage After Deployment') {
            steps {
                sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_AFTER_LOG}"
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
                if (fileExists("${WAR_STORAGE}/${WAR_NAME}_BACKUP")) {
                    sh """
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${DEPLOY_DIR}/${WAR_NAME}"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp ${WAR_STORAGE}/${WAR_NAME}_BACKUP ${DEPLOY_DIR}/"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF
                    """
                } else {
                    echo "Backup file ${WAR_NAME}_BACKUP not found. Capturing compile error..."
                    sh "javac src/main/java/model/Job.java 2> ${WORKSPACE}/compile_error.log || true"
                    def compileError = readFile("${WORKSPACE}/compile_error.log").trim()
                    echo "Compile error captured:\n${compileError}"
                }
                def rollbackEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                def computedRollbackTime = rollbackEndTime - rollbackStartTime
                // Write rollback deployment time to file (do not echo directly)
                sh "echo \"${computedRollbackTime}\" > ${ROLLBACK_DEPLOYMENT_TIME_FILE}"
            }
        }
        always {
            script {
                def pipelineEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                def totalPipelineTime = pipelineEndTime - pipelineStartTime

                // Calculate deployment time from stored env variables if available
                def deploymentTime = "N/A"
                if (env.DEPLOY_START_TIME && env.DEPLOY_END_TIME) {
                    deploymentTime = env.DEPLOY_END_TIME.toInteger() - env.DEPLOY_START_TIME.toInteger()
                } else if (fileExists(DEPLOYMENT_TIME_FILE)) {
                    deploymentTime = readFile(DEPLOYMENT_TIME_FILE).trim()
                }

                def memBefore = fileExists(MEM_BEFORE_LOG) ? readFile(MEM_BEFORE_LOG).trim() : "N/A"
                def memAfter = fileExists(MEM_AFTER_LOG) ? readFile(MEM_AFTER_LOG).trim() : "N/A"
                def resourceUsageBefore = fileExists(RESOURCE_BEFORE_LOG) ? readFile(RESOURCE_BEFORE_LOG).trim() : "N/A"
                def resourceUsageAfter = fileExists(RESOURCE_AFTER_LOG) ? readFile(RESOURCE_AFTER_LOG).trim() : "N/A"

                def rollbackDeployTime = "N/A"
                if (fileExists(ROLLBACK_DEPLOYMENT_TIME_FILE)) {
                    rollbackDeployTime = readFile(ROLLBACK_DEPLOYMENT_TIME_FILE).trim()
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
                echo String.format("| %-35s | %-15s |", "Deployment Time (sec)", deploymentTime)
                echo String.format("| %-35s | %-15s |", "Lead Time for Changes (sec)", leadTimeForChanges)
                echo String.format("| %-35s | %-15s |", "Rollback Deployment Time (sec)", rollbackDeployTime)
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
