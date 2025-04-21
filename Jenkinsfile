// Top‑level vars for timing/metrics
def pipelineStartTime    = 0L
def commitTimeMs         = 0L
def baselineTimeSec      = 0L
def jvmSetupStart        = 0L, jvmSetupEnd        = 0L
def buildCacheRestoreStart = 0L, buildCacheRestoreEnd = 0L
def buildTimeSec         = 0L
def buildCacheSaveStart  = 0L, buildCacheSaveEnd  = 0L
def testCacheRestoreStart  = 0L, testCacheRestoreEnd  = 0L
def testTimeSec          = 0L
def testCacheSaveStart   = 0L, testCacheSaveEnd   = 0L
def jvmStartupStart      = 0L, jvmStartupEnd      = 0L
def deployStartTime      = 0L, deployTimeSec      = 0L
def leadTimeSec          = 0L
def pipelineEndTime      = 0L, totalTimeSec       = 0L

pipeline {
  agent any
  environment {

    SSH_USER         = 'root'
    SSH_HOST         = '40.192.71.74'
    SSH_KEY          = '/var/lib/jenkins/.ssh/id_rsa'
    SSH_OPTS         = '-o StrictHostKeyChecking=no'
    REMOTE_BACKUP_DIR= '/tmp/jenkins_bak'
    WAR_NAME         = 'JobManagement_JENKINS.war'
    CACHE_BASE_DIR   = "${WORKSPACE}/jenkins_caches/${JOB_NAME}"
    BUILD_CACHE_DIR  = "${CACHE_BASE_DIR}/build_classes"
    TEST_CACHE_DIR   = "${CACHE_BASE_DIR}/test_outputs"
    CSV_FILE         = "stage_metrics.csv"
    TOMCAT_HOME      = "/opt/tomcat10"
  }
  options { timestamps() }

  stages {

stage('Initialize') {
    steps {
        script {
            pipelineStartTime = System.currentTimeMillis()
            
            // Get commit timestamp in seconds
            def ct = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim()
            
            // Convert to milliseconds safely
            commitTimeMs = ct ? (ct as Long) * 1000L : 0L
            echo "🔍 Commit timestamp: ${commitTimeMs} ms"
        }
    }
}


stage('Checkout') {
    steps {
        // Add timeout for reliability
        timeout(time: 2, unit: 'MINUTES') {
            git url: 'https://github.com/sai-pranay-teja/JobManagement.git', 
                 branch: 'main',
                 poll: false  // Disable SCM polling if not needed
        }
    }
}

stage('Measure Baseline Build+Test') {
  steps {
    script {
      echo "⏳ Baseline full build+test"
      def t0 = System.currentTimeMillis()  // Use def instead of long

      // Clean and prepare directories
      sh 'rm -rf build test_output'
      sh 'mkdir -p build/WEB-INF/classes test_output'

      // Full source compile
      sh 'find src/main/java/model -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'

      // Compile tests
      sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src/main/resources" -d test_output src/main/test/*.java'

      // Copy config.properties for test execution
      sh 'cp src/main/resources/config.properties test_output/'

      // Run all tests from test_output directory
      sh '''
        java -cp "test_output:src/main/webapp/WEB-INF/lib/*" \
          org.junit.platform.console.ConsoleLauncher \
          --scan-class-path test_output \
          --details summary || true
      '''

      // Time measurement - use explicit casting
      def elapsedMillis = System.currentTimeMillis() - t0
      baselineTimeSec = (long)(elapsedMillis / 1000)  

      // Export using explicit string conversion
      env.BASELINE_TIME_SEC = baselineTimeSec.toString()

      echo "⏱️ Baseline elapsed = ${elapsedMillis} ms"


      echo "⚖️  Baseline time = ${baselineTimeSec} sec"
    }
  }
}



stage('Decide Mode Dynamically') {
    steps {
        script {
            long threshold = 4L
            if (baselineTimeSec >= threshold) {
                writeFile file: 'pipeline_mode.txt', text: 'A'
                echo "✅ Using optimized Mode A"
            } else {
                writeFile file: 'pipeline_mode.txt', text: 'B'
                echo "✅ Using baseline Mode B"
            }
        }
    }
}



stage('Validate Mode') {
    steps {
        script {
            def mode = readFile('pipeline_mode.txt').trim()
            if (mode != 'A' && mode != 'B') {
                error "❌ Invalid MODE: ${mode}"
            }
            echo "🔧 Mode = ${mode}"
        }
    }
}

stage('Initialize & JVM Setup') {
    steps {
        script { 
            jvmSetupStart = System.currentTimeMillis() 
        }
        sh 'java -version || true'
        script {
            def mode = readFile('pipeline_mode.txt').trim()
            if (mode == 'A') {
                sh 'java -Xshare:auto -version > /dev/null 2>&1 || true'
            }
            jvmSetupEnd = System.currentTimeMillis()
        }
    }
}




stage('Build Cache Restore') {
        when { 
    expression { 
        // Add file existence check
        if (!fileExists('pipeline_mode.txt')) {
            error "Mode file missing! Ensure 'Decide Mode Dynamically' stage ran successfully."
        }
        
        // Read and validate mode
        def mode = readFile('pipeline_mode.txt').trim()
        if (!['A','B'].contains(mode)) {
            error "Invalid mode value: ${mode}"
        }
        
        return mode == 'A'
    } 
}  // Use env.MODE
    steps {
        script {
            buildCacheRestoreStart = System.currentTimeMillis()
            // Add quotes for path with spaces
            if (fileExists("${env.BUILD_CACHE_DIR}")) {
                echo "🔄 Restoring build cache from ${env.BUILD_CACHE_DIR}"
                sh """
                    mkdir -p build/WEB-INF/classes
                    cp -r "${env.BUILD_CACHE_DIR}/"* build/WEB-INF/classes/
                """
            } else {
                echo "⚠️ No build cache found"
            }
            buildCacheRestoreEnd = System.currentTimeMillis()
        }
    }
}



    stage('Build') {
    steps {
        script {
            def t0 = System.currentTimeMillis()
            sh 'mkdir -p build/WEB-INF/classes'
            def mode = readFile('pipeline_mode.txt').trim()

            if (mode == 'A') {  // Use env.MODE instead of mode
                def changed = sh(script: "find src/main/java/model -name '*.java' -newer build/WEB-INF/classes", returnStdout: true).trim()
                if (changed) {
                    echo "🔧 Incremental compile"
                    sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
                } else {
                    echo "🔧 Full compile (no changes)"
                    sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
                }
            } else {
                echo "🔧 Full compile (Mode B)"
                sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
            }
            sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
            sh 'cp -R src/main/webapp/* build/'
            sh "jar -cvf ${env.WAR_NAME} -C build ."
            buildTimeSec = (long)((System.currentTimeMillis() - t0)/1000)
            echo "✅ Build took ${buildTimeSec} sec"
        }
    }
}

stage('Build Cache Save') {
        when { 
    expression { 
        // Add file existence check
        if (!fileExists('pipeline_mode.txt')) {
            error "Mode file missing! Ensure 'Decide Mode Dynamically' stage ran successfully."
        }
        
        // Read and validate mode
        def mode = readFile('pipeline_mode.txt').trim()
        if (!['A','B'].contains(mode)) {
            error "Invalid mode value: ${mode}"
        }
        
        return mode == 'A'
    } 
}  // Use env.MODE
    steps {
        script {
            buildCacheSaveStart = System.currentTimeMillis()
            sh "mkdir -p '${env.BUILD_CACHE_DIR}'"  // Quote path
            sh "cp -r build/WEB-INF/classes/* '${env.BUILD_CACHE_DIR}/'"
            buildCacheSaveEnd = System.currentTimeMillis()
            echo "💾 Saved build cache to ${env.BUILD_CACHE_DIR}"
        }
    }
}

stage('Backup WAR') {
    steps {
        sh """
            ssh ${env.SSH_OPTS} -i '${env.SSH_KEY}' ${env.SSH_USER}@${env.SSH_HOST} \
                "mkdir -p '${env.REMOTE_BACKUP_DIR}'"
            scp ${env.SSH_OPTS} -i '${env.SSH_KEY}' ${env.WAR_NAME} \
                ${env.SSH_USER}@${env.SSH_HOST}:'${env.REMOTE_BACKUP_DIR}/${env.WAR_NAME}_bak'
        """
    }
}

stage('Test Cache Restore') {
        when { 
    expression { 
        // Add file existence check
        if (!fileExists('pipeline_mode.txt')) {
            error "Mode file missing! Ensure 'Decide Mode Dynamically' stage ran successfully."
        }
        
        // Read and validate mode
        def mode = readFile('pipeline_mode.txt').trim()
        if (!['A','B'].contains(mode)) {
            error "Invalid mode value: ${mode}"
        }
        
        return mode == 'A'
    } 
}
    steps {
        script {
            testCacheRestoreStart = System.currentTimeMillis()
            if (fileExists(env.TEST_CACHE_DIR)) {
                echo "🔄 Restoring test cache"
                sh "cp -r '${env.TEST_CACHE_DIR}/test_unit' ."
                sh "cp -r '${env.TEST_CACHE_DIR}/test_int' ."
            }
            testCacheRestoreEnd = System.currentTimeMillis()
        }
    }
}

stage('Run Tests') {
    steps {
        script {
            def t0 = System.currentTimeMillis()
            sh 'mkdir -p test_unit test_int && cp src/main/resources/config.properties test_unit/'
            sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_unit src/main/test/TestAppPart1.java'
            sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_int src/main/test/TestAppPart2.java'
            jvmStartupStart = System.currentTimeMillis()
            sh '''
                java -cp "test_unit:src/main/webapp/WEB-INF/lib/*" \
                    org.junit.platform.console.ConsoleLauncher \
                    --select-class TestAppPart1 \
                    --details summary > test_unit.log 2>&1 &
            '''
            sh '''
                java -cp "test_int:src/main/webapp/WEB-INF/lib/*" \
                    org.junit.platform.console.ConsoleLauncher \
                    --select-class TestAppPart2 \
                    --details summary > test_int.log 2>&1 &
            '''
            sh 'wait'
            jvmStartupEnd = System.currentTimeMillis()
            testTimeSec = (long)((System.currentTimeMillis() - t0)/1000)  
            echo "✅ Tests took ${testTimeSec} sec"
        }
    }
}

stage('Test Cache Save') {
        when { 
    expression { 
        // Add file existence check
        if (!fileExists('pipeline_mode.txt')) {
            error "Mode file missing! Ensure 'Decide Mode Dynamically' stage ran successfully."
        }
        
        // Read and validate mode
        def mode = readFile('pipeline_mode.txt').trim()
        if (!['A','B'].contains(mode)) {
            error "Invalid mode value: ${mode}"
        }
        
        return mode == 'A'
    } 
}
    steps {
        script {
            testCacheSaveStart = System.currentTimeMillis()
            sh "mkdir -p '${env.TEST_CACHE_DIR}'"
            sh "cp -r test_unit '${env.TEST_CACHE_DIR}/'"
            sh "cp -r test_int  '${env.TEST_CACHE_DIR}/'"
            testCacheSaveEnd = System.currentTimeMillis()
            echo "💾 Saved test cache to ${env.TEST_CACHE_DIR}"
        }
    }
}

stage('Deploy') {
    steps {
        script {
            deployStartTime = System.currentTimeMillis()
            def warBase = env.WAR_NAME.replaceAll(/\.war$/, "")

            sh """
                ssh ${env.SSH_OPTS} -i '${env.SSH_KEY}' ${env.SSH_USER}@${env.SSH_HOST} \
                    "sudo rm -rf /opt/tomcat10/webapps/${warBase}* || true
                     sudo ${TOMCAT_HOME}/bin/catalina.sh stop || true
                     sudo ${TOMCAT_HOME}/bin/catalina.sh start"
                scp ${env.SSH_OPTS} -i '${env.SSH_KEY}' \
                    ${env.WAR_NAME} \
                    ${env.SSH_USER}@${env.SSH_HOST}:/opt/tomcat10/webapps/
            """
            deployTimeSec = (long)((System.currentTimeMillis() - deployStartTime)/1000)
            leadTimeSec = (long)((deployStartTime - commitTimeMs)/1000)
            echo "🚀 Deployed in ${deployTimeSec} sec, lead time = ${leadTimeSec} sec"
        }
    }
}
  }

  post {
    always {
        script {
            // Calculate total pipeline time
            pipelineEndTime = System.currentTimeMillis()
            totalTimeSec = (long)((pipelineEndTime - pipelineStartTime) / 1000)

            // Calculate overhead durations
            def jvmSetupTime = (long)((jvmSetupEnd - jvmSetupStart) / 1000)
            def buildCacheRestoreTime = (long)((buildCacheRestoreEnd - buildCacheRestoreStart) / 1000)
            def buildCacheSaveTime = (long)((buildCacheSaveEnd - buildCacheSaveStart) / 1000)
            def testCacheRestoreTime = (long)((testCacheRestoreEnd - testCacheRestoreStart) / 1000)
            def testCacheSaveTime = (long)((testCacheSaveEnd - testCacheSaveStart) / 1000)
            def jvmStartupTime = (long)((jvmStartupEnd - jvmStartupStart) / 1000)

            // Calculate net times
            def netBuild = buildTimeSec - buildCacheRestoreTime - buildCacheSaveTime
            def netTest = testTimeSec - jvmStartupTime
            def netTotal = totalTimeSec - jvmSetupTime - buildCacheRestoreTime - buildCacheSaveTime - 
                         testCacheRestoreTime - testCacheSaveTime - jvmStartupTime

            // Print metrics
            def mode = readFile('pipeline_mode.txt').trim()
            echo "=== PIPELINE METRICS (Mode ${mode}) ==="
            echo "Build Time                   : ${buildTimeSec} sec"
            echo "Test Time                    : ${testTimeSec} sec"
            echo "Deploy Time                  : ${deployTimeSec} sec"
            echo "Lead Time for Change         : ${leadTimeSec} sec"
            echo "Total Pipeline Time          : ${totalTimeSec} sec"
            echo "Overhead (JVM Setup)         : ${jvmSetupTime} sec"
            echo "Overhead (Cache Restore‑Bld) : ${buildCacheRestoreTime} sec"
            echo "Overhead (Cache Save‑Bld)    : ${buildCacheSaveTime} sec"
            echo "Overhead (Cache Restore‑Tst) : ${testCacheRestoreTime} sec"
            echo "Overhead (Cache Save‑Tst)    : ${testCacheSaveTime} sec"
            echo "Overhead (JVM Startup)       : ${jvmStartupTime} sec"
            echo "➡️ Pure Build Time           : ${netBuild} sec"
            echo "➡️ Pure Test Time            : ${netTest} sec"
            echo "➡️ Pure Total Time           : ${netTotal} sec"

            // Generate CSV
            def csvHeader = "MODE,BUILD,TEST,DEPLOY,LEAD,TOTAL,JVM_SETUP,BC_RESTORE,BC_SAVE,TC_RESTORE,TC_SAVE,JVM_STARTUP,NET_BUILD,NET_TEST,NET_TOTAL\n"
            def csvLine = String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                readFile('pipeline_mode.txt').trim(),
                buildTimeSec,
                testTimeSec,
                deployTimeSec,
                leadTimeSec,
                totalTimeSec,
                jvmSetupTime,
                buildCacheRestoreTime,
                buildCacheSaveTime,
                testCacheRestoreTime,
                testCacheSaveTime,
                jvmStartupTime,
                netBuild,
                netTest,
                netTotal
            )
            
            writeFile file: env.CSV_FILE, text: csvHeader + csvLine + "\n"
            archiveArtifacts artifacts: env.CSV_FILE, onlyIfSuccessful: false


                        if (fileExists('pipeline_mode.txt')) {
                sh 'rm pipeline_mode.txt'
            }
        }
    }
}



}
