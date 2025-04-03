pipeline {
    agent any

    environment {
        TOMCAT_HOME         = "/opt/tomcat10"
        WAR_NAME            = "JobManagement.war"
        DEPLOY_DIR          = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE         = "${WORKSPACE}"
        
        METRICS_DIR         = "${WORKSPACE}/metrics"
        DEPLOYMENT_TIME_LOG = "${METRICS_DIR}/deployment_times.log"
        ROLLBACK_TIME_LOG   = "${METRICS_DIR}/rollback_times.log"
        SUCCESS_RATE_LOG    = "${METRICS_DIR}/success_rate.log"
        RESOURCE_LOG        = "${METRICS_DIR}/resource_usage.log"
        LEAD_TIME_LOG       = "${METRICS_DIR}/lead_times.log"
        
        SSH_KEY             = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER            = "root"
        SSH_HOST            = "localhost"
        SSH_OPTS            = "-o StrictHostKeyChecking=no"
    }

    stages {
        stage('Initialize Metrics') {
            steps {
                sh """
                    mkdir -p "${METRICS_DIR}"
                    touch "${DEPLOYMENT_TIME_LOG}" "${ROLLBACK_TIME_LOG}" 
                    touch "${SUCCESS_RATE_LOG}" "${LEAD_TIME_LOG}"
                """
            }
        }

        stage('Checkout & Commit Time') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
                sh "git log -1 --format=%ct > ${METRICS_DIR}/commit_time.txt"
            }
        }

        stage('Build WAR') {
            steps {
                sh 'mkdir -p build/WEB-INF/classes'
                sh '''
                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" \\
                          -d build/WEB-INF/classes \\
                          $(find src -name "*.java")
                    jar -cvf ${WAR_NAME} -C build .
                '''
            }
        }

        stage('Create Backup') {
            steps {
                sh "cp ${WAR_NAME} ${WAR_NAME}.backup"
            }
        }

        stage('Deploy with Metrics') {
            steps {
                script {
                    def commitTime = sh(script: "cat ${METRICS_DIR}/commit_time.txt", returnStdout: true).trim().toInteger()
                    def deployStart = System.currentTimeMillis()

                    try {
                        sh """
                            # Stop Tomcat if running
                            if ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "ps -p \$(cat ${TOMCAT_HOME}/temp/tomcat.pid)"; then
                                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "${TOMCAT_HOME}/bin/shutdown.sh"
                                sleep 5
                            fi

                            # Deploy
                            scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                            
                            # Start Tomcat
                            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "${TOMCAT_HOME}/bin/startup.sh"
                            
                            # Verify deployment
                            timeout(time: 2, unit: 'MINUTES') {
                                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "grep 'Deployment of web application archive' ${TOMCAT_HOME}/logs/catalina.out"
                            }
                        """
                        currentBuild.result = 'SUCCESS'
                    } catch(Exception e) {
                        currentBuild.result = 'FAILURE'
                    }

                    def deployEnd = System.currentTimeMillis()
                    def deployDuration = (deployEnd - deployStart)/1000
                    sh "echo ${deployDuration} >> ${DEPLOYMENT_TIME_LOG}"
                    
                    def leadTime = (deployEnd/1000) - commitTime
                    sh "echo ${leadTime} >> ${LEAD_TIME_LOG}"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${METRICS_DIR}/**, ${WAR_NAME}.backup", allowEmptyArchive: true
            
            script {
                // Success Rate
                def totalBuilds = sh(script: "wc -l < ${SUCCESS_RATE_LOG}", returnStdout: true).trim().toInteger() + 1
                def successCount = currentBuild.result == 'SUCCESS' ? 1 : 0
                sh "echo ${successCount} >> ${SUCCESS_RATE_LOG}"
                
                // Calculations using bc for precision
                def successRate = sh(script: """
                    echo "scale=2; (\$(awk '{sum+=\$1} END {print sum}' ${SUCCESS_RATE_LOG}) / ${totalBuilds}) * 100" | bc
                """, returnStdout: true).trim() + "%"

                def mttr = sh(script: """
                    awk '{sum+=\$1} END {printf "%.1f", sum/NR}' ${ROLLBACK_TIME_LOG} 2>/dev/null || echo 0
                """, returnStdout: true).trim()

                def changeFailureRate = sh(script: """
                    echo "scale=2; ((${totalBuilds} - ${successCount}) / ${totalBuilds}) * 100" | bc
                """, returnStdout: true).trim() + "%"

                echo """
                ========== CI/CD METRICS REPORT ==========
                Deployment Time:      ${deployDuration}s
                Average Deployment:   ${sh(script: "awk '{sum+=\$1} END {printf \"%.1f\", sum/NR}' ${DEPLOYMENT_TIME_LOG}", returnStdout: true).trim()}s
                Success Rate:         ${successRate}
                Change Failure Rate:  ${changeFailureRate}
                MTTR:                 ${mttr}s
                Lead Time (Avg):      ${sh(script: "awk '{sum+=\$1} END {printf \"%.1f\", sum/NR}' ${LEAD_TIME_LOG}", returnStdout: true).trim()}s
                """
            }
        }

        failure {
            script {
                def rollbackStart = System.currentTimeMillis()
                sh """
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                        rm -f ${DEPLOY_DIR}/${WAR_NAME}
                        cp ${WAR_STORAGE}/${WAR_NAME}.backup ${DEPLOY_DIR}/
                        ${TOMCAT_HOME}/bin/shutdown.sh || true
                        sleep 5
                        ${TOMCAT_HOME}/bin/startup.sh
EOF
                """
                def rollbackDuration = (System.currentTimeMillis() - rollbackStart)/1000
                sh "echo ${rollbackDuration} >> ${ROLLBACK_TIME_LOG}"
            }
        }
    }
}