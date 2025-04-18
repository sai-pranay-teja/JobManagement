// Global variables to capture overall metrics and timing
def pipelineStartTime = 0
def leadTimeForChanges = 0
// rollbackTime is declared elsewhere; do not redeclare it here
def rollbackTime = "N/A"

// Helper function to record stage timings.
def recordStageTiming(String stageName, int startTime, int endTime) {
    def duration = endTime - startTime
    echo "${stageName} took ${duration} seconds"
    // Append timing info to a log for later analysis
    sh "echo '${stageName}: ${duration} sec' >> ${WORKSPACE}/stage_timings.log"
}

pipeline {
    // Use any agent; for Agent Prewarm, you can update 'agent any' to a persistent label like: agent { label 'prewarm' }
    agent any

    environment {
        // Deployment settings
        TOMCAT_HOME          = "/opt/tomcat10"
        WAR_NAME             = "JobManagement_JENKINS.war"
        DEPLOY_DIR           = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE          = "${WORKSPACE}"  // WAR built in workspace

        // Log files for resources and metrics
        RESOURCE_BEFORE_LOG  = "${WORKSPACE}/resource_before_usage.log"
        RESOURCE_AFTER_LOG   = "${WORKSPACE}/resource_after_usage.log"
        LOG_FILE             = "${WORKSPACE}/deployment.log"
        DEPLOYMENT_TIME_FILE = "${WORKSPACE}/deployment_time.log"
        ROLLBACK_LOG         = "${WORKSPACE}/rollback.log"
        MEM_BEFORE_LOG       = "${WORKSPACE}/mem_before.log"
        MEM_AFTER_LOG        = "${WORKSPACE}/mem_after.log"
        TEST_RESULTS_LOG     = "${WORKSPACE}/test_results.log"

        // SSH settings
        SSH_KEY              = "/var/lib/jenkins/.ssh/id_rsa"
        SSH_USER             = "root"
        SSH_HOST             = "18.61.60.110"
        SSH_OPTS             = "-o StrictHostKeyChecking=no"
        BACKUP_DIR           = "/tmp/jenkins_bak"

        // Optimization toggles (set these to "true" or "false" as needed)
        ENABLE_WORKSPACE_CACHE = "true"
        ENABLE_PARALLEL_TEST   = "true"
        ENABLE_INCREMENTAL_BUILD = "true"
        ENABLE_AGENT_PREWARM   = "true"
    }


    stages {

        stage('Clean Workspace') {
            steps {
                // Clean the workspace, excluding the backups directory
                cleanWs(
                    deleteDirs: true,
                    patterns: [[pattern: 'backups/**', type: 'EXCLUDE']]
                )
            }
        }

        stage('Initialize') {
            steps {
                script {
                    // Record the start time of the entire pipeline
                    pipelineStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    echo "Pipeline start time recorded: ${pipelineStartTime}"
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Checkout", stageStart, stageEnd)
                }
            }
        }


        stage('Build WAR') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()

                    // --- Workspace-Cache / Incremental-Build Pattern ---
                    // Check if cache is enabled and a cache marker exists.
                    if (env.ENABLE_WORKSPACE_CACHE == "true" && fileExists("${WORKSPACE}/build_cache.marker")) {
                        echo "Workspace cache found, performing incremental build."
                        // Copy cached build artifacts instead of a full rebuild
                        sh 'cp -R ${WORKSPACE}/build_cache/* build/'
                    } else {
                        // Full build process
                        sh 'mkdir -p build/WEB-INF/classes'
                        sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'
                        sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                        sh 'cp -R src/main/webapp/* build/'
                        sh 'jar -cvf ${WAR_NAME} -C build .'

                        // Update cache if enabled
                        if (env.ENABLE_WORKSPACE_CACHE == "true") {
                            sh 'mkdir -p ${WORKSPACE}/build_cache'
                            sh 'cp -R build/* ${WORKSPACE}/build_cache/'
                            sh 'touch ${WORKSPACE}/build_cache.marker'
                            echo "Workspace cache updated."
                        }
                    }
                    // ------------------------------------------------------

                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Build WAR", stageStart, stageEnd)
                }
            }
        }

        stage('Backup WAR') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    if (fileExists("${WAR_STORAGE}/${WAR_NAME}")) {
                        def backupDir = "${BACKUP_DIR}"
                        def warFile = "${WAR_STORAGE}/${WAR_NAME}"
                        def backupFile = "${backupDir}/${WAR_NAME}_bak"
                        sh "mkdir -p ${backupDir}"
                        sh "cp ${warFile} ${backupFile}"
                        echo "Backup saved: ${backupFile}"
                    } else {
                        echo "ERROR: WAR file ${WAR_NAME} not found; backup not created."
                    }
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Backup WAR", stageStart, stageEnd)
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    if (env.ENABLE_PARALLEL_TEST == "true") {
                        // --- Parallel-Test Pattern ---
                        parallel (
                            "Unit Tests Part 1": {
                                sh """
                                    mkdir -p ${WORKSPACE}/test_output/part1
                                    # Compile and run test subset 1 (adapt file selection as needed)
                                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output/part1 \$(find ${WORKSPACE}/src/main/test -name "*Part1*.java")
                                    java -cp "${WORKSPACE}/test_output/part1:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output/part1 --details summary > ${WORKSPACE}/test_results_part1.log 2>&1 || true
                                """
                            },
                            "Unit Tests Part 2": {
                                sh """
                                    mkdir -p ${WORKSPACE}/test_output/part2
                                    # Compile and run test subset 2 (adapt file selection as needed)
                                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output/part2 \$(find ${WORKSPACE}/src/main/test -name "*Part2*.java")
                                    java -cp "${WORKSPACE}/test_output/part2:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output/part2 --details summary > ${WORKSPACE}/test_results_part2.log 2>&1 || true
                                """
                            }
                        )
                        // Merge logs from parallel runs
                        sh "cat ${WORKSPACE}/test_results_part1.log ${WORKSPACE}/test_results_part2.log > ${TEST_RESULTS_LOG}"
                    } else {
                        // --- Serial Test Execution (Baseline) ---
                        sh """
                            mkdir -p ${WORKSPACE}/test_output
                            javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output \$(find ${WORKSPACE}/src/main/test -name "*.java")
                            java -cp "${WORKSPACE}/test_output:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output --details summary > ${TEST_RESULTS_LOG} 2>&1 || true
                        """
                    }
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Run Unit Tests", stageStart, stageEnd)
                }
            }
        }

        stage('Measure Resource Usage Before Deployment') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_BEFORE_LOG}"
                    sh "free -h > ${MEM_BEFORE_LOG}"
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Measure Resource Usage Before Deployment", stageStart, stageEnd)
                }
            }
        }

        stage('Deploy and Restart Tomcat') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()

                    // For Agent Prewarm pattern: if ENABLE_AGENT_PREWARM=="true", run this stage on a prewarmed node.
                    // (Configuration for a persistent agent node is managed via node labels and Jenkins configuration.)
                    def deployStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def commitTime = sh(script: "git log -1 --format=%ct", returnStdout: true).trim().toInteger()
                    leadTimeForChanges = deployStartTime - commitTime
                    echo "Lead Time for Changes: ${leadTimeForChanges} seconds"

                    // Deploy and restart Tomcat
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
                    def deployEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def deployDuration = deployEndTime - deployStartTime
                    sh "echo \"Deployment took ${deployDuration} seconds.\" >> ${DEPLOYMENT_TIME_FILE}"
                    echo "Deployment completed in ${deployDuration} seconds."

                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Deploy and Restart Tomcat", stageStart, stageEnd)
                }
            }
        }

        stage('Measure Resource Usage After Deployment') {
            steps {
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \$1/1024, substr(\$0, index(\$0,\$2))}' > ${RESOURCE_AFTER_LOG}"
                    sh "free -h > ${MEM_AFTER_LOG}"
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Measure Resource Usage After Deployment", stageStart, stageEnd)
                }
            }
        }
    } // end stages

    post {
    success {
        echo 'Deployment successful!'
    }
    failure {
        echo 'Deployment failed! Performing rollback...'
        script {
            def rollbackStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
            // --- Rollback Optimization Pattern ---
            if (fileExists("${BACKUP_DIR}/${WAR_NAME}_bak")) {
                sh """
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${BACKUP_DIR}/${WAR_NAME}"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp ${BACKUP_DIR}/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}"
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
sleep 5
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF
                """
            } else {
                echo "Backup file ${WAR_NAME}_bak not found. Capturing compile error..."
                def compileError = readFile("${WORKSPACE}/compile_error.log").trim()
                echo "Compile error captured:\n${compileError}"
            }
            def rollbackEndTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
            def computedRollbackTime = rollbackEndTime - rollbackStartTime
            sh "echo \"Rollback took ${computedRollbackTime} seconds.\" >> ${ROLLBACK_LOG}"
            echo "Rollback completed in ${computedRollbackTime} seconds."
        }
    }
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
            
            // Helper function to get timing from a file (returns 0 if file not found)
            def getTiming = { filePath ->
                return fileExists(filePath) ? readFile(filePath).trim().toInteger() : 0
            }
            
            // Read baseline and optimized times for each pattern
            def baselineCache   = getTiming("${WORKSPACE}/baseline_workspace_cache.txt")
            def optimizedCache  = getTiming("${WORKSPACE}/optimized_workspace_cache.txt")
            def baselineIncremental   = getTiming("${WORKSPACE}/baseline_incremental_build.txt")
            def optimizedIncremental  = getTiming("${WORKSPACE}/optimized_incremental_build.txt")
            def baselineAgent   = getTiming("${WORKSPACE}/baseline_agent_prewarm.txt")
            def optimizedAgent  = getTiming("${WORKSPACE}/optimized_agent_prewarm.txt")
            def baselineRollback = getTiming("${WORKSPACE}/baseline_rollback.txt")
            def optimizedRollback= getTiming("${WORKSPACE}/optimized_rollback.txt")
            
            // Calculate differences and percentage reductions
            def calcDelta = { baseline, optimized ->
                return baseline - optimized
            }
            def calcPct = { baseline, delta ->
                return (baseline > 0) ? (delta * 100 / baseline) : 0
            }
            
            def deltaCache       = calcDelta(baselineCache, optimizedCache)
            def pctCache         = calcPct(baselineCache, deltaCache)
            def deltaIncremental = calcDelta(baselineIncremental, optimizedIncremental)
            def pctIncremental   = calcPct(baselineIncremental, deltaIncremental)
            def deltaAgent       = calcDelta(baselineAgent, optimizedAgent)
            def pctAgent         = calcPct(baselineAgent, deltaAgent)
            def deltaRollback    = calcDelta(baselineRollback, optimizedRollback)
            def pctRollback      = calcPct(baselineRollback, deltaRollback)
            
            // Print the Optimization Patterns summary table dynamically
            echo ""
            echo "-------------------------------------------------------------"
            echo "       Optimization Patterns Summary                         "
            echo "-------------------------------------------------------------"
            echo String.format("| %-30s | %-15s | %-15s | %-10s | %-10s |", 
                "Pattern", "Baseline (sec)", "Optimized (sec)", "Δ (sec)", "% Reduction")
            echo "-------------------------------------------------------------"
            echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", 
                "Workspace-Cache Pattern", baselineCache, optimizedCache, deltaCache, pctCache)
            echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", 
                "Incremental-Build Pattern", baselineIncremental, optimizedIncremental, deltaIncremental, pctIncremental)
            echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", 
                "Agent Pre-Warm Pattern", baselineAgent, optimizedAgent, deltaAgent, pctAgent)
            echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", 
                "Rollback Optimization", baselineRollback, optimizedRollback, deltaRollback, pctRollback)
            echo "-------------------------------------------------------------"
            echo ""
        }
    }
}


}
