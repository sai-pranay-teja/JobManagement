/*
 * Pattern‑Driven CI/CD Jenkinsfile (Java‑Servlet Job Portal)
 * Enhanced with detailed overhead & “pure” time metrics
 */

def pipelineStartTime = 0L
def jvmSetupStart = 0L, jvmSetupEnd = 0L
def buildCacheRestoreStart = 0L, buildCacheRestoreEnd = 0L
def buildTime = 0L
def buildCacheSaveStart = 0L, buildCacheSaveEnd = 0L
def testCacheRestoreStart = 0L, testCacheRestoreEnd = 0L
def testTime = 0L
def testCacheSaveStart = 0L, testCacheSaveEnd = 0L
def jvmStartupStart = 0L, jvmStartupEnd = 0L
def deployTime = 0L
def leadTimeForChanges = 0L
def pipelineEndTime = 0L, totalTime = 0L

// CI/CD Optimization Mode: 'A' = optimized (with caching), 'B' = baseline
def mode = 'A'


pipeline {
  agent any

  environment {
    TOMCAT_HOME = '/opt/tomcat10'
    WAR_NAME    = 'JobManagement_JENKINS.war'
    DEPLOY_DIR  = "${TOMCAT_HOME}/webapps"
    SSH_KEY     = '/var/lib/jenkins/.ssh/id_rsa'
    SSH_USER    = 'root'
    SSH_HOST    = '40.192.66.15'
    SSH_OPTS    = '-o StrictHostKeyChecking=no'
    
  }
  def CSV_FILE = "${env.WORKSPACE}/stage_metrics.csv"
  options { timestamps() }

  stages {

    // Define file paths for logs and war outputs
    stage('Setup Paths') {
      steps {
        script {
          env.WAR_STORAGE          = "${env.WORKSPACE}"
          env.RESOURCE_BEFORE_LOG  = "${env.WORKSPACE}/resource_before_usage.log"
          env.RESOURCE_AFTER_LOG   = "${env.WORKSPACE}/resource_after_usage.log"
          env.LOG_FILE             = "${env.WORKSPACE}/deployment.log"
          env.DEPLOYMENT_TIME_FILE = "${env.WORKSPACE}/deployment_time.log"
          env.ROLLBACK_LOG         = "${env.WORKSPACE}/rollback.log"
          env.MEM_BEFORE_LOG       = "${env.WORKSPACE}/mem_before.log"
          env.MEM_AFTER_LOG        = "${env.WORKSPACE}/mem_after.log"
          env.TEST_RESULTS_LOG     = "${env.WORKSPACE}/test_results.log"
        }
      }
    }

    // Measure JVM setup time
    stage('Initialize & JVM Setup') {
      steps {
        script {
          pipelineStartTime = System.currentTimeMillis()
          jvmSetupStart    = System.currentTimeMillis()
        }
        sh 'java -version || true'
        script {
          jvmSetupEnd = System.currentTimeMillis()
        }
      }
    }

    // Ensure correct mode is used
    stage('Validate MODE') {
      steps {
        script {
          if (!(mode == 'A' || mode == 'B')) {
            error "Invalid mode '${mode}'. Must be 'A' or 'B'."
          }
        }
      }
    }

    // Git checkout the application
    stage('Checkout') {
      steps {
        git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
      }
    }

    // Restore previously stashed build artifacts
    stage('Build Cache Restore') {
      when { expression { mode == 'A' } }
      steps {
        script {
          buildCacheRestoreStart = System.currentTimeMillis()
          try { unstash 'buildClasses' } catch(e) { /* ignore first run */ }
          buildCacheRestoreEnd = System.currentTimeMillis()
        }
      }
    }

    // Compile source files and package WAR
    stage('Build') {
      steps {
        script {
          def start = System.currentTimeMillis()
          sh 'mkdir -p build/WEB-INF/classes'

          if (mode == 'A') {
            def changed = sh(
              script: "find src/main/java/model -name '*.java' -newer build/WEB-INF/classes",
              returnStdout: true
            ).trim()
            if (changed) {
              echo "Incremental compile of changed files"
              sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
            } else {
              echo "No changes => full compile"
              sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
            }
          } else {
            echo "[Mode B] Full compile"
            sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
          }

          sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
          sh 'cp -R src/main/webapp/* build/'
          sh "jar -cvf ${WAR_NAME} -C build ."
          buildTime = (System.currentTimeMillis() - start) / 1000
        }
      }
    }

    // Save compiled build classes for caching
    stage('Build Cache Save') {
      when { expression { mode == 'A' } }
      steps {
        script {
          buildCacheSaveStart = System.currentTimeMillis()
          stash name: 'buildClasses', includes: 'build/WEB-INF/classes/**', allowEmpty: true
          buildCacheSaveEnd = System.currentTimeMillis()
        }
      }
    }

    // Back up WAR on the remote server
    stage('Backup WAR') {
      steps {
        sh """
          ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} 'mkdir -p /tmp/jenkins_bak'
          scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:/tmp/jenkins_bak/
        """
      }
    }

    // Restore cached test outputs
    stage('Test Cache Restore') {
      when { expression { mode == 'A' } }
      steps {
        script {
          testCacheRestoreStart = System.currentTimeMillis()
          try { unstash 'testOutput' } catch(e) { /* ignore */ }
          testCacheRestoreEnd = System.currentTimeMillis()
        }
      }
    }

    // Compile and run tests with timing
    stage('Run Tests') {
      steps {
        script {
          def start = System.currentTimeMillis()
          sh 'mkdir -p test_unit && cp src/main/resources/config.properties test_unit/'
          sh 'mkdir -p test_int'

          sh "javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_unit src/main/test/TestAppPart1.java"
          sh "javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_int src/main/test/TestAppPart2.java"

          jvmStartupStart = System.currentTimeMillis()
          sh "java -cp 'test_unit:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > test_unit.log 2>&1 &"
          sh "java -cp 'test_int:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > test_int.log 2>&1 &"
          sh 'wait'
          jvmStartupEnd = System.currentTimeMillis()

          testTime = (System.currentTimeMillis() - start) / 1000
        }
      }
    }

    // Save test output for future use
    stage('Test Cache Save') {
      when { expression { mode == 'A' } }
      steps {
        script {
          testCacheSaveStart = System.currentTimeMillis()
          stash name: 'testOutput', includes: 'test_unit.log,test_int.log', allowEmpty: true
          testCacheSaveEnd = System.currentTimeMillis()
        }
      }
    }

    // Deploy WAR to Tomcat and restart server
    stage('Deploy') {
      steps {
        script {
          def deployStart = System.currentTimeMillis()
          def commitTime = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toInteger()
          def now = (System.currentTimeMillis() / 1000) as Integer
          leadTimeForChanges = now - commitTime

          sh """
            scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} \\
              'sudo rm -rf ${DEPLOY_DIR}/${WAR_NAME}* && sudo ${TOMCAT_HOME}/bin/catalina.sh stop || true && sudo ${TOMCAT_HOME}/bin/catalina.sh start'
          """
          deployTime = (System.currentTimeMillis() - deployStart) / 1000
        }
      }
    }
  }

  // Calculate all metrics and write to CSV for analysis
  post {
    always {
      script {
        pipelineEndTime = System.currentTimeMillis()
        totalTime = (pipelineEndTime - pipelineStartTime) / 1000

        def jvmSetupTime          = (jvmSetupEnd - jvmSetupStart) / 1000
        def buildCacheRestoreTime = (buildCacheRestoreEnd - buildCacheRestoreStart) / 1000
        def buildCacheSaveTime    = (buildCacheSaveEnd - buildCacheSaveStart) / 1000
        def testCacheRestoreTime  = (testCacheRestoreEnd - testCacheRestoreStart) / 1000
        def testCacheSaveTime     = (testCacheSaveEnd - testCacheSaveStart) / 1000
        def jvmStartupTime        = (jvmStartupEnd - jvmStartupStart) / 1000

        def netBuild = buildTime - buildCacheRestoreTime - buildCacheSaveTime
        def netTest  = testTime - jvmStartupTime
        def netTotal = totalTime - jvmSetupTime - buildCacheRestoreTime - buildCacheSaveTime - testCacheRestoreTime - testCacheSaveTime - jvmStartupTime

        echo "=== PIPELINE METRICS (${mode}) ==="
        echo "Build Time         : ${buildTime} sec"
        echo "Test Time          : ${testTime} sec"
        echo "Deploy Time        : ${deployTime} sec"
        echo "Lead Time for Change: ${leadTimeForChanges} sec"
        echo "Total Pipeline Time: ${totalTime} sec"
        echo "Overhead (JVM)     : ${jvmSetupTime} sec"
        echo "➡️ Pure Build Time : ${netBuild} sec"
        echo "➡️ Pure Test Time  : ${netTest} sec"
        echo "➡️ Pure Total Time : ${netTotal} sec"

        def header = '''\
MODE,BUILD,TEST,DEPLOY,LEAD,TOTAL,JVM_SETUP,BC_RESTORE,BC_SAVE,TC_RESTORE,TC_SAVE,JVM_STARTUP,NET_BUILD,NET_TEST,NET_TOTAL
'''
        def line = """\
${mode},${buildTime},${testTime},${deployTime},${leadTimeForChanges},${totalTime},${jvmSetupTime},${buildCacheRestoreTime},${buildCacheSaveTime},${testCacheRestoreTime},${testCacheSaveTime},${jvmStartupTime},${netBuild},${netTest},${netTotal}
"""

        if (!fileExists(CSV_FILE)) {
          writeFile file: CSV_FILE, text: header + line
        } else {
          writeFile file: CSV_FILE, text: readFile(CSV_FILE) + line
        }

        archiveArtifacts artifacts: "${CSV_FILE}", onlyIfSuccessful: false
      }
    }
  }
}
