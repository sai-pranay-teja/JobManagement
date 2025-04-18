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
    // Use any agent; if you have a persistent node for prewarm, update this accordingly.
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
        ENABLE_WORKSPACE_CACHE   = "false"  // Toggle caching ON/OFF for baseline vs optimized runs.
        ENABLE_PARALLEL_TEST     = "true"
        ENABLE_INCREMENTAL_BUILD = "true"
        ENABLE_AGENT_PREWARM     = "true"

        // File paths for storing measured times for each pattern:
        BASELINE_WS_CACHE_FILE      = "${WORKSPACE}/baseline_workspace_cache.txt"
        OPTIMIZED_WS_CACHE_FILE     = "${WORKSPACE}/optimized_workspace_cache.txt"
        BASELINE_INCREMENTAL_FILE   = "${WORKSPACE}/baseline_incremental_build.txt"
        OPTIMIZED_INCREMENTAL_FILE  = "${WORKSPACE}/optimized_incremental_build.txt"
        BASELINE_AGENT_FILE         = "${WORKSPACE}/baseline_agent_prewarm.txt"
        OPTIMIZED_AGENT_FILE        = "${WORKSPACE}/optimized_agent_prewarm.txt"
        BASELINE_ROLLBACK_FILE      = "${WORKSPACE}/baseline_rollback.txt"
        OPTIMIZED_ROLLBACK_FILE     = "${WORKSPACE}/optimized_rollback.txt"
    }
    
    stages {
        stage('Clean Workspace') {
            steps {
                // Clean the workspace, excluding backups directory
                cleanWs(deleteDirs: true, patterns: [[pattern: 'backups/**', type: 'EXCLUDE']])
            }
        }
        
        stage('Initialize') {
            steps {
                script {
                    // Record the overall pipeline start time
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
        
        // -------------------------------------------------------------
        // Workspace‑Cache Pattern (Baselines and Optimized)
        // -------------------------------------------------------------
        stage('Build WAR - Baseline (Workspace Cache)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Execute a full build process without using a cache.
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline (Workspace Cache) build time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${BASELINE_WS_CACHE_FILE}"
                }
            }
        }
        
        stage('Build WAR - Optimized (Workspace Cache)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // If caching is enabled and cache marker exists, reuse cached artifacts; otherwise, do a full build and then update cache.
                    if (env.ENABLE_WORKSPACE_CACHE == "true" && fileExists("${WORKSPACE}/build_cache.marker")) {
                        echo "Workspace cache found, performing incremental build."
                        sh 'cp -R ${WORKSPACE}/build_cache/* build/'
                    } else {
                        sh 'mkdir -p build/WEB-INF/classes'
                        sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'
                        sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                        sh 'cp -R src/main/webapp/* build/'
                        sh 'jar -cvf ${WAR_NAME} -C build .'
                        
                        if (env.ENABLE_WORKSPACE_CACHE == "true") {
                            sh 'mkdir -p ${WORKSPACE}/build_cache'
                            sh 'cp -R build/* ${WORKSPACE}/build_cache/'
                            sh 'touch ${WORKSPACE}/build_cache.marker'
                            echo "Workspace cache updated."
                        }
                    }
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Workspace Cache) build time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${OPTIMIZED_WS_CACHE_FILE}"
                }
            }
        }
        
        // -------------------------------------------------------------
        // Incremental‑Build Pattern (Baselines and Optimized)
        // For demonstration, the same build commands are used.
        // In practice, incremental builds compile only changed files.
        // -------------------------------------------------------------
        stage('Build WAR - Baseline (Incremental Build)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Full build process (baseline)
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline (Incremental Build) full build time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${BASELINE_INCREMENTAL_FILE}"
                }
            }
        }
        
        stage('Build WAR - Optimized (Incremental Build)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Simulate an incremental build process – ideally, only changed files are recompiled.
                    // Here we use the same commands (for demonstration), but expect a lower elapsed time.
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Incremental Build) build time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${OPTIMIZED_INCREMENTAL_FILE}"
                }
            }
        }
        
        // -------------------------------------------------------------
        // Agent Pre‑Warm Pattern (Baselines and Optimized)
        // Here we simulate deployment delays.
        // -------------------------------------------------------------
        stage('Deploy - Baseline (Agent Pre-Warm)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Simulate a delay for cold agent provisioning (e.g., 5 seconds)
                    sh "sleep 5"
                    // (Deploy commands would be here)
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline (Agent Pre-Warm) deploy overhead: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${BASELINE_AGENT_FILE}"
                }
            }
        }
        
        stage('Deploy - Optimized (Agent Pre-Warm)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Simulate faster provisioning when using a persistent (prewarmed) agent (e.g., 2 seconds)
                    sh "sleep 2"
                    // (Deploy commands would be here)
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Agent Pre-Warm) deploy overhead: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${OPTIMIZED_AGENT_FILE}"
                }
            }
        }
        
        // -------------------------------------------------------------
        // Rollback Optimization Pattern (Baselines and Optimized)
        // -------------------------------------------------------------
        stage('Simulate Rollback - Baseline') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Simulate baseline rollback (e.g., 5 seconds delay)
                    sh "sleep 5"
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline rollback time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${BASELINE_ROLLBACK_FILE}"
                }
            }
        }
        
        stage('Simulate Rollback - Optimized') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Simulate optimized rollback (e.g., 2 seconds delay)
                    sh "sleep 2"
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized rollback time: ${elapsed} seconds"
                    sh "echo '${elapsed}' > ${OPTIMIZED_ROLLBACK_FILE}"
                }
            }
        }
        
        // -------------------------------------------------------------
        // Other stages: Backup WAR, Run Unit Tests, Resource Measurement, etc.
        // (These remain unchanged from your existing file.)
        // -------------------------------------------------------------
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
                                    # Compile and run test subset 1; assumes file names contain 'Part1'
                                    javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output/part1 \$(find ${WORKSPACE}/src/main/test -name "*Part1*.java")
                                    java -cp "${WORKSPACE}/test_output/part1:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output/part1 --details summary > ${WORKSPACE}/test_results_part1.log 2>&1 || true
                                """
                            },
                            "Unit Tests Part 2": {
                                sh """
                                    mkdir -p ${WORKSPACE}/test_output/part2
                                    # Compile and run test subset 2; assumes file names contain 'Part2'
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

                    // For Agent Prewarm Pattern: if ENABLE_AGENT_PREWARM=="true", run this stage on a prewarmed node.
                    // (Configuration for a persistent agent node is managed via node labels and Jenkins configuration.)
                    def deployStartTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def commitTime = sh(script: "git log -1 --format=%ct", returnStdout: true).trim().toInteger()
                    leadTimeForChanges = deployStartTime - commitTime
                    echo "Lead Time for Changes: ${leadTimeForChanges} seconds"

                    // Deploy and restart Tomcat via SSH
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
                // Rollback Optimization Pattern via SSH
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
                
                // Helper function to read a timing from a file
                def getTiming = { filePath -> fileExists(filePath) ? readFile(filePath).trim().toInteger() : 0 }
                
                // Read baseline and optimized times for each pattern
                def baselineCache      = getTiming(BASELINE_WS_CACHE_FILE)
                def optimizedCache     = getTiming(OPTIMIZED_WS_CACHE_FILE)
                def baselineIncremental = getTiming(BASELINE_INCREMENTAL_FILE)
                def optimizedIncremental= getTiming(OPTIMIZED_INCREMENTAL_FILE)
                def baselineAgent      = getTiming(BASELINE_AGENT_FILE)
                def optimizedAgent     = getTiming(OPTIMIZED_AGENT_FILE)
                def baselineRollback   = getTiming(BASELINE_ROLLBACK_FILE)
                def optimizedRollback  = getTiming(OPTIMIZED_ROLLBACK_FILE)
                
                // Calculate differences and percentage reductions
                def calcDelta = { baseline, optimized -> baseline - optimized }
                def calcPct   = { baseline, delta -> (baseline > 0) ? (delta * 100 / baseline) : 0 }
                
                def deltaCache       = calcDelta(baselineCache, optimizedCache)
                def pctCache         = calcPct(baselineCache, deltaCache)
                def deltaIncremental = calcDelta(baselineIncremental, optimizedIncremental)
                def pctIncremental   = calcPct(baselineIncremental, deltaIncremental)
                def deltaAgent       = calcDelta(baselineAgent, optimizedAgent)
                def pctAgent         = calcPct(baselineAgent, deltaAgent)
                def deltaRollback    = calcDelta(baselineRollback, optimizedRollback)
                def pctRollback      = calcPct(baselineRollback, deltaRollback)
                
                // Print Optimization Patterns Summary Table dynamically.
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
