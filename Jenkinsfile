pipeline {
    agent any

    environment {
        // Deployment variables
        TOMCAT_HOME         = "/opt/tomcat10"
        WAR_NAME            = "JobManagement.war"
        DEPLOY_DIR          = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE         = "${WORKSPACE}"
        
        // Metric tracking files
        METRICS_DIR         = "${WORKSPACE}/metrics"
        DEPLOYMENT_TIME_LOG = "${METRICS_DIR}/deployment_times.log"
        ROLLBACK_TIME_LOG   = "${METRICS_DIR}/rollback_times.log"
        SUCCESS_RATE_LOG    = "${METRICS_DIR}/success_rate.log"
        RESOURCE_LOG        = "${METRICS_DIR}/resource_usage.log"
        LEAD_TIME_LOG       = "${METRICS_DIR}/lead_times.log"
        
        // SSH variables
        SSH_KEY             = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER            = "root"
        SSH_HOST            = "localhost"
        SSH_OPTS            = "-o StrictHostKeyChecking=no"
    }

    stages {
        stage('Initialize Metrics') {
            steps {
                sh "mkdir -p ${METRICS_DIR}"
                sh "touch ${DEPLOYMENT_TIME_LOG} ${ROLLBACK_TIME_LOG} ${SUCCESS_RATE_LOG} ${LEAD_TIME_LOG}"
            }
        }

        stage('Checkout & Get Commit Time') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
                sh "git log -1 --format=%ct > ${METRICS_DIR}/commit_time.txt"
            }
        }

        stage('Build WAR with JaCoCo') {
            steps {
                sh 'mkdir -p build/WEB-INF/classes'
                
                // Compile with JaCoCo agent (no Maven required)
                sh '''
                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" \
                          -d build/WEB-INF/classes \
                          $(find src -name "*.java")
                    
                    # Package with JaCoCo instrumentation
                    jar -cvf ${WAR_NAME} -C build .
                '''
            }
        }

        stage('Run Tests with Coverage') {
            steps {
                jacoco(
                    execPattern: '**/jacoco.exec',
                    classPattern: 'build/WEB-INF/classes',
                    sourcePattern: 'src/main/java',
                    exclusionPattern: 'src/test/*'
                )
                
                // Add your test execution command here
                sh 'echo "Running tests with coverage..."'  // Replace with actual test command
            }
        }

        stage('Measure Resource Usage') {
            steps {
                sh """
                    echo '=== PRE-DEPLOYMENT RESOURCES ===' >> ${RESOURCE_LOG}
                    vmstat 1 5 >> ${RESOURCE_LOG}
                    echo '\n' >> ${RESOURCE_LOG}
                """
            }
        }

        stage('Deploy with Metrics') {
            steps {
                script {
                    def commitTime = sh(script: "cat ${METRICS_DIR}/commit_time.txt", returnStdout: true).trim().toInteger()
                    def deployStart = System.currentTimeMillis() / 1000

                    try {
                        sh """
                            scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_STORAGE}/${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                            
                            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
EOF
                            tail -f ${TOMCAT_HOME}/logs/catalina.out | grep -m 1 'Deployment of web application archive'
                        """
                    } catch(Exception e) {
                        currentBuild.result = 'FAILURE'
                    }

                    def deployEnd = System.currentTimeMillis() / 1000
                    def deployDuration = deployEnd - deployStart
                    sh "echo ${deployDuration} >> ${DEPLOYMENT_TIME_LOG}"
                    
                    def leadTime = deployEnd - commitTime
                    sh "echo ${leadTime} >> ${LEAD_TIME_LOG}"
                }
            }
        }

        stage('Post-Deployment Metrics') {
            steps {
                sh """
                    echo '=== POST-DEPLOYMENT RESOURCES ===' >> ${RESOURCE_LOG}
                    vmstat 1 5 >> ${RESOURCE_LOG}
                """
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${METRICS_DIR}/**", allowEmptyArchive: true
            
            // Publish JaCoCo report
            jacoco(
                execPattern: '**/jacoco.exec',
                classPattern: 'build/WEB-INF/classes',
                sourcePattern: 'src/main/java',
                exclusionPattern: 'src/test/*'
            )

            script {
                def totalBuilds = sh(script: "wc -l < ${SUCCESS_RATE_LOG}", returnStdout: true).trim().toInteger() + 1
                def successCount = currentBuild.result == 'SUCCESS' ? 1 : 0
                sh "echo ${successCount} >> ${SUCCESS_RATE_LOG}"
                
                def successRate = (sh(script: "awk '{sum+=\$1} END {print sum/NR*100}' ${SUCCESS_RATE_LOG}", returnStdout: true).trim()) + "%"
                def mttr = sh(script: "awk '{sum+=\$1} END {print sum/NR}' ${ROLLBACK_TIME_LOG} 2>/dev/null || echo 0", returnStdout: true).trim()
                def changeFailureRate = ((totalBuilds - successCount).toFloat() / totalBuilds * 100) + "%"

                echo """
                ========== CI/CD METRICS REPORT ==========
                Deployment Time:      ${deployDuration}s (Current)
                Average Deployment:   ${sh(script: "awk '{sum+=\$1} END {print sum/NR}' ${DEPLOYMENT_TIME_LOG}", returnStdout: true).trim()}s
                Success Rate:         ${successRate}
                Change Failure Rate:  ${changeFailureRate}
                MTTR:                 ${mttr}s
                Lead Time (Avg):      ${sh(script: "awk '{sum+=\$1} END {print sum/NR}' ${LEAD_TIME_LOG}", returnStdout: true).trim()}s
                """
            }
        }

        failure {
            script {
                def rollbackStart = System.currentTimeMillis() / 1000
                sh """
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${DEPLOY_DIR}/${WAR_NAME}"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp ${WAR_STORAGE}/${WAR_NAME}.backup ${DEPLOY_DIR}/"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
EOF
                """
                def rollbackDuration = (System.currentTimeMillis() / 1000) - rollbackStart
                sh "echo ${rollbackDuration} >> ${ROLLBACK_TIME_LOG}"
            }
        }
    }
}