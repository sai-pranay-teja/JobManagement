// Global variables to capture overall metrics and timing
def pipelineStartTime = 0
def leadTimeForChanges = 0
def rollbackTime = "N/A"

// Helper function to record stage timings.
def recordStageTiming(String stageName, int startTime, int endTime) {
    def duration = endTime - startTime
    echo "${stageName} took ${duration} seconds"
    // Append timing info to a log file for later analysis
    sh "echo '${stageName}: ${duration} sec' >> ${WORKSPACE}/stage_timings.log"
}

pipeline {
    agent any

    environment {
        // Deployment settings
        TOMCAT_HOME          = "/opt/tomcat10"
        WAR_NAME             = "JobManagement_JENKINS.war"
        DEPLOY_DIR           = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE          = "${WORKSPACE}"  // Where the WAR is built

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

        // Optimization toggles – set these appropriately for baseline vs. optimized runs.
        ENABLE_WORKSPACE_CACHE   = "false"
        ENABLE_INCREMENTAL_BUILD = "true"
        ENABLE_PARALLEL_TEST     = "true"

        // File paths for storing measured times for each pattern:
        BASELINE_WS_CACHE_FILE       = "${WORKSPACE}/baseline_workspace_cache.txt"
        OPTIMIZED_WS_CACHE_FILE      = "${WORKSPACE}/optimized_workspace_cache.txt"
        BASELINE_INCREMENTAL_FILE    = "${WORKSPACE}/baseline_incremental_build.txt"
        OPTIMIZED_INCREMENTAL_FILE   = "${WORKSPACE}/optimized_incremental_build.txt"
        BASELINE_COMPRESSION_FILE    = "${WORKSPACE}/baseline_artifact_compression.txt"
        OPTIMIZED_COMPRESSION_FILE   = "${WORKSPACE}/optimized_artifact_compression.txt"
        BASELINE_PARALLEL_TEST_FILE  = "${WORKSPACE}/baseline_parallel_test.txt"
        OPTIMIZED_PARALLEL_TEST_FILE = "${WORKSPACE}/optimized_parallel_test.txt"
        BASELINE_ROLLBACK_FILE       = "${WORKSPACE}/baseline_rollback.txt"
        OPTIMIZED_ROLLBACK_FILE      = "${WORKSPACE}/optimized_rollback.txt"
    }
    
    stages {

        stage('Clean Workspace') {
            steps {
                // Clean workspace (preserving backups directory if necessary)
                cleanWs(deleteDirs: true, patterns: [[pattern: 'backups/**', type: 'EXCLUDE']])
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
                script {
                    def stageStart = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
                    def stageEnd = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    recordStageTiming("Checkout", stageStart, stageEnd)
                }
            }
        }

        // --- Workspace-Cache Pattern ---
        stage('Build WAR - Baseline (Workspace Cache)') {
            steps {
                script {
                    def startTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    // Full build without using any cache:
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes \\$(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline (Workspace Cache) build time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${BASELINE_WS_CACHE_FILE}"
                }
            }
        }
        
        stage('Build WAR - Optimized (Workspace Cache)') {
            steps {
                script {
                    def startTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    // If caching is enabled and a marker exists, reuse cached artifacts.
                    if (env.ENABLE_WORKSPACE_CACHE == "true" && fileExists("${WORKSPACE}/build_cache.marker")) {
                        echo "Cache exists: using cached artifacts."
                        sh 'cp -R ${WORKSPACE}/build_cache/* build/'
                    } else {
                        // Full build then update cache if enabled.
                        sh 'mkdir -p build/WEB-INF/classes'
                        sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes \\$(find src -name "*.java") 2> ${WORKSPACE}/compile_error.log'
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
                    def endTime = sh(script: "date +%s", returnStdout: true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Workspace Cache) build time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${OPTIMIZED_WS_CACHE_FILE}"
                }
            }
        }
        
        // --- Incremental-Build Pattern ---
        stage('Build WAR - Baseline (Incremental Build)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Baseline: perform full build (simulate incremental build baseline)
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes \\$(find src -name "*.java")'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline (Incremental Build) full build time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${BASELINE_INCREMENTAL_FILE}"
                }
            }
        }
        
        stage('Build WAR - Optimized (Incremental Build)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Optimized: simulate incremental build (in practice, compile only changed files)
                    sh 'mkdir -p build/WEB-INF/classes'
                    sh 'javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes \\$(find src -name "*.java")'
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'
                    sh 'jar -cvf ${WAR_NAME} -C build .'
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Incremental Build) build time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${OPTIMIZED_INCREMENTAL_FILE}"
                }
            }
        }
        
        // --- Artifact Compression Pattern ---
        stage('Artifact Compression - Baseline') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Baseline: copy the WAR uncompressed
                    sh "cp ${WAR_STORAGE}/${WAR_NAME} ${WORKSPACE}/artifact_baseline.war"
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Baseline Artifact Transfer (Uncompressed) time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${BASELINE_COMPRESSION_FILE}"
                }
            }
        }
        
        stage('Artifact Compression - Optimized') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    // Optimized: compress and decompress the WAR
                    sh "gzip -c ${WAR_STORAGE}/${WAR_NAME} > ${WORKSPACE}/artifact_optimized.war.gz"
                    sh "gunzip -c ${WORKSPACE}/artifact_optimized.war.gz > ${WORKSPACE}/artifact_optimized.war"
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized Artifact Compression & Transfer time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${OPTIMIZED_COMPRESSION_FILE}"
                }
            }
        }
        
        // --- Parallel Test Pattern ---
        stage('Run Unit Tests - Baseline (Serial)') {
    steps {
        script {
            def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
            sh """
                mkdir -p ${WORKSPACE}/test_output_serial
                javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output_serial \$(find ${WORKSPACE}/src/main/test -name "*.java")
                java -cp "${WORKSPACE}/test_output_serial:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output_serial --details summary > ${WORKSPACE}/test_results_serial.log 2>&1 || true
            """
            def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
            def elapsed = endTime - startTime
            echo "Baseline (Serial) test execution time: ${elapsed} sec"
            sh "echo '${elapsed}' > ${BASELINE_PARALLEL_TEST_FILE}"
        }
    }
}
        
        stage('Run Unit Tests - Optimized (Parallel)') {
            steps {
                script {
                    def startTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    parallel (
    "Test Part 1": {
        sh """
            mkdir -p ${WORKSPACE}/test_output_parallel/part1
            javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output_parallel/part1 \$(find ${WORKSPACE}/src/main/test -name "*Part1*.java")
            java -cp "${WORKSPACE}/test_output_parallel/part1:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output_parallel/part1 --details summary > ${WORKSPACE}/test_results_parallel_part1.log 2>&1 || true
        """
    },
    "Test Part 2": {
        sh """
            mkdir -p ${WORKSPACE}/test_output_parallel/part2
            javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*:${WORKSPACE}/src" -d ${WORKSPACE}/test_output_parallel/part2 \$(find ${WORKSPACE}/src/main/test -name "*Part2*.java")
            java -cp "${WORKSPACE}/test_output_parallel/part2:${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path ${WORKSPACE}/test_output_parallel/part2 --details summary > ${WORKSPACE}/test_results_parallel_part2.log 2>&1 || true
        """
    }
)
                    sh "cat ${WORKSPACE}/test_results_parallel_part1.log ${WORKSPACE}/test_results_parallel_part2.log > ${TEST_RESULTS_LOG}"
                    def endTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def elapsed = endTime - startTime
                    echo "Optimized (Parallel) test execution time: ${elapsed} sec"
                    sh "echo '${elapsed}' > ${OPTIMIZED_PARALLEL_TEST_FILE}"
                }
            }
        }
        
        // --- Rollback Optimization Pattern ---
        stage('Backup WAR') {
            steps {
                script {
                    def stageStart = sh(script:"date +%s", returnStdout:true).trim().toInteger()
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
                    def stageEnd = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    recordStageTiming("Backup WAR", stageStart, stageEnd)
                }
            }
        }
        
        stage('Deploy and Restart Tomcat') {
            steps {
                script {
                    def stageStart = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def deployStartTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def commitTime = sh(script:"git log -1 --format=%ct", returnStdout:true).trim().toInteger()
                    leadTimeForChanges = deployStartTime - commitTime
                    echo "Lead Time for Changes: ${leadTimeForChanges} sec"
                    // Deploy and restart Tomcat via SSH
                    sh """
                        echo "Starting deployment at \\$(date)" >> ${LOG_FILE}
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_STORAGE}/${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF
                        tail -f ${TOMCAT_HOME}/logs/catalina.out | while read line; do
                           echo "\\${line}" | grep -q "Deployment of web application archive" && break;
                        done
                    """
                    def deployEndTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    def deployDuration = deployEndTime - deployStartTime
                    sh "echo \"Deployment took ${deployDuration} sec.\" >> ${DEPLOYMENT_TIME_FILE}"
                    echo "Deployment completed in ${deployDuration} sec"
                    def stageEnd = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    recordStageTiming("Deploy and Restart Tomcat", stageStart, stageEnd)
                }
            }
        }
        
        stage('Measure Resource Usage Before Deployment') {
            steps {
                script {
                    def stageStart = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \\$1/1024, substr(\\$0, index(\\$0,\\$2))}' > ${RESOURCE_BEFORE_LOG}"
                    sh "free -h > ${MEM_BEFORE_LOG}"
                    def stageEnd = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    recordStageTiming("Measure Resource Usage Before Deployment", stageStart, stageEnd)
                }
            }
        }
        
        stage('Measure Resource Usage After Deployment') {
            steps {
                script {
                    def stageStart = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                    sh "vmstat -s | awk '{printf \"%.2f MB - %s\\n\", \\$1/1024, substr(\\$0, index(\\$0,\\$2))}' > ${RESOURCE_AFTER_LOG}"
                    sh "free -h > ${MEM_AFTER_LOG}"
                    def stageEnd = sh(script:"date +%s", returnStdout:true).trim().toInteger()
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
                def rollbackStartTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                // Actual rollback via SSH – this should measure the real time taken to restore the backup
                if (fileExists("${BACKUP_DIR}/${WAR_NAME}_bak")) {
                    sh """
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "rm -rf ${BACKUP_DIR}/${WAR_NAME}"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "cp ${BACKUP_DIR}/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}"
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
pkill -f 'org.apache.catalina.startup.Bootstrap' || true
${TOMCAT_HOME}/bin/shutdown.sh || true
${TOMCAT_HOME}/bin/startup.sh
exit
EOF
                    """
                } else {
                    echo "Backup file ${WAR_NAME}_bak not found. Checking compile error..."
                    def compileError = readFile("${WORKSPACE}/compile_error.log").trim()
                    echo "Compile error captured:\n${compileError}"
                }
                def rollbackEndTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
                def computedRollbackTime = rollbackEndTime - rollbackStartTime
                sh "echo \"Rollback took ${computedRollbackTime} sec.\" >> ${ROLLBACK_LOG}"
                echo "Rollback completed in ${computedRollbackTime} sec."
            }
        }
        always {
            script {
                def pipelineEndTime = sh(script:"date +%s", returnStdout:true).trim().toInteger()
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
                def getTiming = { filePath -> fileExists(filePath) ? readFile(filePath).trim().toInteger() : 0 }
                
                // Read measured timings for each pattern
                def baselineCache = getTiming(BASELINE_WS_CACHE_FILE)
                def optimizedCache = getTiming(OPTIMIZED_WS_CACHE_FILE)
                def baselineIncremental = getTiming(BASELINE_INCREMENTAL_FILE)
                def optimizedIncremental = getTiming(OPTIMIZED_INCREMENTAL_FILE)
                def baselineCompression = getTiming(BASELINE_COMPRESSION_FILE)
                def optimizedCompression = getTiming(OPTIMIZED_COMPRESSION_FILE)
                def baselineTest = getTiming(BASELINE_PARALLEL_TEST_FILE)
                def optimizedTest = getTiming(OPTIMIZED_PARALLEL_TEST_FILE)
                def baselineRollback = getTiming(BASELINE_ROLLBACK_FILE)
                def optimizedRollback = getTiming(OPTIMIZED_ROLLBACK_FILE)
                
                // Functions to compute delta and percent reduction
                def calcDelta = { baseline, optimized -> baseline - optimized }
                def calcPct = { baseline, delta -> (baseline > 0) ? (delta * 100 / baseline) : 0 }
                
                def deltaCache = calcDelta(baselineCache, optimizedCache)
                def pctCache = calcPct(baselineCache, deltaCache)
                def deltaIncremental = calcDelta(baselineIncremental, optimizedIncremental)
                def pctIncremental = calcPct(baselineIncremental, deltaIncremental)
                def deltaCompression = calcDelta(baselineCompression, optimizedCompression)
                def pctCompression = calcPct(baselineCompression, deltaCompression)
                def deltaTest = calcDelta(baselineTest, optimizedTest)
                def pctTest = calcPct(baselineTest, deltaTest)
                def deltaRollback = calcDelta(baselineRollback, optimizedRollback)
                def pctRollback = calcPct(baselineRollback, deltaRollback)
                
                // Print the summary table for optimization patterns
                echo ""
                echo "-------------------------------------------------------------"
                echo "       Optimization Patterns Summary                         "
                echo "-------------------------------------------------------------"
                echo String.format("| %-30s | %-15s | %-15s | %-10s | %-10s |", "Pattern", "Baseline (sec)", "Optimized (sec)", "Δ (sec)", "% Reduction")
                echo "-------------------------------------------------------------"
                echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", "Workspace-Cache Pattern", baselineCache.intValue(), optimizedCache.intValue(), calcDelta(baselineCache, optimizedCache).intValue(), calcPct(baselineCache, deltaCache).intValue())
                echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", "Incremental-Build Pattern", baselineIncremental.intValue(), optimizedIncremental.intValue(), calcDelta(baselineIncremental, optimizedIncremental).intValue(), calcPct(baselineIncremental, deltaIncremental).intValue())
                echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", "Artifact Compression Pattern", baselineCompression.intValue(), optimizedCompression.intValue(), calcDelta(baselineCompression, optimizedCompression).intValue(), calcPct(baselineCompression, deltaCompression).intValue())
                echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", "Parallel Test Pattern", baselineTest.intValue(), optimizedTest.intValue(), calcDelta(baselineTest, optimizedTest).intValue(), calcPct(baselineTest, deltaTest).intValue())
                echo String.format("| %-30s | %-15d | %-15d | %-10d | %-10d%% |", "Rollback Optimization", baselineRollback.intValue(), optimizedRollback.intValue(), calcDelta(baselineRollback, optimizedRollback).intValue(), calcPct(baselineRollback, deltaRollback).intValue())
                echo "-------------------------------------------------------------"
                echo ""
            }
        }
    }
}
