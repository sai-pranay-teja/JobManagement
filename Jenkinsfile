/*
 * Pattern‑Driven CI/CD Jenkinsfile (Java‑Servlet Job Portal)
 * Enhanced with detailed overhead & “pure” time metrics
 */
pipeline {
  agent any

  environment {
    SSH_USER            = 'root'
    SSH_HOST            = '40.192.66.15'
    SSH_KEY             = '/var/lib/jenkins/.ssh/id_rsa'
    SSH_OPTS            = '-o StrictHostKeyChecking=no'
    REMOTE_BACKUP_DIR   = '/tmp/jenkins_bak'
    WAR_NAME            = 'JobManagement_JENKINS.war'
    WAR_STORAGE         = '.'  // or "${env.WORKSPACE}"
    TEST_CLASSES_CACHE  = 'test_cache'
    // initialize all metric vars with defaults
    COMMIT_TIME                 = '0'
    PIPELINE_START              = '0'
    DEPLOY_START                = '0'
    JVM_SETUP_START             = '0'
    JVM_SETUP_END               = '0'
    BUILD_CACHE_RESTORE_START   = '0'
    BUILD_CACHE_RESTORE_END     = '0'
    BUILD_CACHE_SAVE_START      = '0'
    BUILD_CACHE_SAVE_END        = '0'
    TEST_CACHE_RESTORE_START    = '0'
    TEST_CACHE_RESTORE_END      = '0'
    TEST_CACHE_SAVE_START       = '0'
    TEST_CACHE_SAVE_END         = '0'
    JVM_STARTUP_TIME            = '0'
    BUILD_TIME                  = '0'
    TEST_TIME                   = '0'
  }

  options { timestamps() }

  stages {
    stage('Initialize') {
      steps {
        script {
          env.PIPELINE_START = System.currentTimeMillis().toString()
          echo "📥 PIPELINE_START = ${env.PIPELINE_START}"
        }
      }
    }

    stage('Checkout') {
      steps {
        git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
        script {
          env.COMMIT_TIME = sh(script: "git log -1 --format=%ct", returnStdout: true).trim()
          echo "🔨 COMMIT_TIME   = ${env.COMMIT_TIME}"
        }
      }
    }

    stage('Measure Baseline Build + Test') {
      steps {
        script {
          def start  = System.currentTimeMillis()
          echo "⏱ Measuring baseline build+test…"
          sh 'find src/main/java/model -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
          sh 'cp src/main/resources/config.properties test_output/'
          sh 'java -cp "test_output:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path test_output --details summary || true'
          def end    = System.currentTimeMillis()
          env.BASELINE_TIME = ((end - start)/1000).toString()
          echo "✅ BASELINE_TIME = ${env.BASELINE_TIME} sec"
        }
      }
    }

    stage('Decide Mode Dynamically') {
      steps {
        script {
          def base = env.BASELINE_TIME.toDouble()
          def T    = 5
          if (base >= T) {
            env.MODE = 'A'
          } else {
            env.MODE = 'B'
          }
          echo "🎛 MODE = ${env.MODE}"
        }
      }
    }

    stage('Initialize & JVM Setup') {
      steps {
        script { env.JVM_SETUP_START = System.currentTimeMillis().toString() }
        sh 'java -version || true' 
        script { env.JVM_SETUP_END = System.currentTimeMillis().toString() }
        echo "🖥 JVM_SETUP_START=${env.JVM_SETUP_START}, JVM_SETUP_END=${env.JVM_SETUP_END}"
      }
    }

    stage('Build Cache Restore') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          env.BUILD_CACHE_RESTORE_START = System.currentTimeMillis().toString()
          unstash 'buildClasses'
          env.BUILD_CACHE_RESTORE_END   = System.currentTimeMillis().toString()
          echo "☁️ BUILD_CACHE_RESTORE_START=${env.BUILD_CACHE_RESTORE_START}, END=${env.BUILD_CACHE_RESTORE_END}"
        }
      }
    }

    stage('Build') {
      steps {
        script {
          def start = System.currentTimeMillis()
          if (env.MODE == 'A' && !fileExists("${TEST_CLASSES_CACHE}/test_classes.jar")) {
            echo "🔄 Incremental build"
            sh 'javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes src/main/java/model/*.java'
            sh "jar cf ${TEST_CLASSES_CACHE}/test_classes.jar build/"
          } else {
            echo "🏗 Full compile"
            sh 'find src/main/java/model -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
          }
          sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
          sh 'cp -R src/main/webapp/* build/'
          sh "jar -cvf ${WAR_NAME} -C build ."
          env.BUILD_TIME = ((System.currentTimeMillis() - start)/1000).toString()
          echo "✅ BUILD_TIME = ${env.BUILD_TIME} sec"
        }
      }
    }

    stage('Build Cache Save') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          env.BUILD_CACHE_SAVE_START = System.currentTimeMillis().toString()
          stash name: 'buildClasses', includes: 'build/WEB-INF/classes/**', allowEmpty: true
          env.BUILD_CACHE_SAVE_END   = System.currentTimeMillis().toString()
          echo "☁️ BUILD_CACHE_SAVE_START=${env.BUILD_CACHE_SAVE_START}, END=${env.BUILD_CACHE_SAVE_END}"
        }
      }
    }

    stage('Deploy to Tomcat') {
      steps {
        script {
          env.DEPLOY_START = System.currentTimeMillis().toString()
          echo "🚚 DEPLOY_START = ${env.DEPLOY_START}"
          sh """
            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} 'mkdir -p ${REMOTE_BACKUP_DIR}'
            scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_STORAGE}/${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${REMOTE_BACKUP_DIR}/${WAR_NAME}_bak
            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} 'sudo rm -rf /opt/tomcat10/webapps/JobManagement_JENKINS || true; sudo /opt/tomcat10/bin/catalina.sh stop || true; sudo /opt/tomcat10/bin/catalina.sh start'
          """
        }
      }
    }

    stage('Test Cache Restore') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          env.TEST_CACHE_RESTORE_START = System.currentTimeMillis().toString()
          unstash 'testOutput'
          env.TEST_CACHE_RESTORE_END   = System.currentTimeMillis().toString()
          echo "☁️ TEST_CACHE_RESTORE_START=${env.TEST_CACHE_RESTORE_START}, END=${env.TEST_CACHE_RESTORE_END}"
        }
      }
    }

    stage('Run Tests') {
      steps {
        script {
          def t0 = System.currentTimeMillis()
          sh 'mkdir -p test_unit test_int'
          sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_unit src/main/test/TestAppPart1.java'
          sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_int  src/main/test/TestAppPart2.java'
          def jvm0 = System.currentTimeMillis()
          sh 'java -cp "test_unit:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > test_unit.log 2>&1 &'
          sh 'java -cp "test_int:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > test_int.log 2>&1 &'
          sh 'wait'
          def jvm1 = System.currentTimeMillis()
          env.JVM_STARTUP_TIME = ((jvm1 - jvm0)/1000).toString()
          env.TEST_TIME        = ((System.currentTimeMillis() - t0)/1000).toString()
          echo "✅ JVM_STARTUP_TIME=${env.JVM_STARTUP_TIME} sec, TEST_TIME=${env.TEST_TIME} sec"
        }
      }
    }

    stage('Test Cache Save') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          env.TEST_CACHE_SAVE_START = System.currentTimeMillis().toString()
          stash name: 'testOutput', includes: 'test_unit.log,test_int.log', allowEmpty: true
          env.TEST_CACHE_SAVE_END   = System.currentTimeMillis().toString()
          echo "☁️ TEST_CACHE_SAVE_START=${env.TEST_CACHE_SAVE_START}, END=${env.TEST_CACHE_SAVE_END}"
        }
      }
    }

    stage('Finalize Metrics') {
      steps {
        script {
          // safe parsers
          def safeLong = { v -> v?.isNumber() ? v.toLong() : 0L }
          def safeInt  = { v -> v?.isInteger() ? v.toInteger() : 0 }

          def now    = System.currentTimeMillis()
          def ds     = safeLong(env.DEPLOY_START)
          def cm     = safeLong(env.COMMIT_TIME)
          def ps     = safeLong(env.PIPELINE_START)
          def js0    = safeLong(env.JVM_SETUP_START)
          def je0    = safeLong(env.JVM_SETUP_END)
          def br0    = safeLong(env.BUILD_CACHE_RESTORE_START)
          def br1    = safeLong(env.BUILD_CACHE_RESTORE_END)
          def bs0    = safeLong(env.BUILD_CACHE_SAVE_START)
          def bs1    = safeLong(env.BUILD_CACHE_SAVE_END)
          def tr0    = safeLong(env.TEST_CACHE_RESTORE_START)
          def tr1    = safeLong(env.TEST_CACHE_RESTORE_END)
          def ts0    = safeLong(env.TEST_CACHE_SAVE_START)
          def ts1    = safeLong(env.TEST_CACHE_SAVE_END)
          def jt     = safeInt(env.JVM_STARTUP_TIME)
          def bt     = safeInt(env.BUILD_TIME)
          def tt     = safeInt(env.TEST_TIME)

          def deployTime = (now - ds)/1000
          def leadTime   = (now - cm)/1000
          def totalTime  = (now - ps)/1000
          def jvmSetup   = (je0 - js0)/1000
          def brTime     = (br1 - br0)/1000
          def bsTime     = (bs1 - bs0)/1000
          def trTime     = (tr1 - tr0)/1000
          def tsTime     = (ts1 - ts0)/1000
          def netBuild   = bt - brTime - bsTime
          def netTest    = tt - jt
          def netTotal   = totalTime - jvmSetup - brTime - bsTime - trTime - tsTime - jt

          echo """
=== PIPELINE METRICS (Mode ${env.MODE}) ===
Build Time                   : ${bt} sec
Test Time                    : ${tt} sec
Deploy Time                  : ${deployTime} sec
Lead Time for Change         : ${leadTime} sec
Total Pipeline Time          : ${totalTime} sec
Overhead (JVM Setup)         : ${jvmSetup} sec
Overhead (Cache Restore-B)   : ${brTime} sec
Overhead (Cache Save-B)      : ${bsTime} sec
Overhead (Cache Restore-T)   : ${trTime} sec
Overhead (Cache Save-T)      : ${tsTime} sec
Overhead (JVM Startup)       : ${jt} sec
➡️ Pure Build Time           : ${netBuild} sec
➡️ Pure Test Time            : ${netTest} sec
➡️ Pure Total Time           : ${netTotal} sec
"""

          writeFile file: 'stage_metrics.csv', text: 
            "MODE,BUILD,TEST,DEPLOY,LEAD,TOTAL,JVM_SETUP,BC_R,BC_S,TC_R,TC_S,JVM_START,NET_B,NET_T,NET_ALL\n" +
            "${env.MODE},${bt},${tt},${deployTime},${leadTime},${totalTime},${jvmSetup},${brTime},${bsTime},${trTime},${tsTime},${jt},${netBuild},${netTest},${netTotal}\n"

          archiveArtifacts artifacts: 'stage_metrics.csv', onlyIfSuccessful: false
        }
      }
    }
  }
}
