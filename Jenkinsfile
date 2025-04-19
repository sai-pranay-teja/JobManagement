#!/usr/bin/env groovy
// top‑level timestamp variables
def pipelineStartTime       = 0L
def baselineStartTime       = 0L
def baselineTime            = 0L
def jvmSetupStartTime       = 0L
def jvmSetupEndTime         = 0L
def buildCacheRestoreStart  = 0L
def buildCacheRestoreEnd    = 0L
def buildStartTime          = 0L
def buildTime               = 0L
def buildCacheSaveStart     = 0L
def buildCacheSaveEnd       = 0L
def testCacheRestoreStart   = 0L
def testCacheRestoreEnd     = 0L
def testStartTime           = 0L
def testTime                = 0L
def testCacheSaveStart      = 0L
def testCacheSaveEnd        = 0L
def jvmStartupStartTime     = 0L
def jvmStartupEndTime       = 0L
def deployStartTime         = 0L
def deployTime              = 0L
def leadTimeForChanges      = 0L
def pipelineEndTime         = 0L
def totalPipelineTime       = 0L

pipeline {
  agent any
  environment {
    TOMCAT_HOME          = '/opt/tomcat10'
    WAR_NAME             = 'JobManagement_JENKINS.war'
    DEPLOY_DIR           = "${TOMCAT_HOME}/webapps"
    SSH_KEY              = '/var/lib/jenkins/.ssh/id_rsa'
    SSH_USER             = 'root'
    SSH_HOST             = '40.192.66.15'
    SSH_OPTS             = '-o StrictHostKeyChecking=no'
    BUILD_CACHE_STASH    = 'buildClasses'
    TEST_CACHE_STASH     = 'testOutput'
    TEST_CLASSES_CACHE   = 'test_cache'
    CSV_FILE             = 'stage_metrics.csv'
  }

  options { timestamps() }

  stages {
    stage('Initialize') {
      steps {
        script {
          pipelineStartTime = System.currentTimeMillis()
          echo "✅ Pipeline start: ${pipelineStartTime}"
        }
      }
    }

    stage('Measure Baseline Build+Test') {
      steps {
        script {
          baselineStartTime = System.currentTimeMillis()
          echo "🔍 Measuring baseline build+test…"

          // full compile
          sh '''#!/bin/bash
            mkdir -p build/WEB-INF/classes
            find src/main/java/model -name "*.java" \
              | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" \
                       -d build/WEB-INF/classes
          '''

          // ensure config.properties for tests
          sh 'mkdir -p test_output && cp src/main/resources/config.properties test_output/'

          // compile & run all tests
          sh '''#!/bin/bash
            javac -cp "src/main/webapp/WEB-INF/lib/*:src" \
              -d test_output src/main/test/*.java
            java -cp "test_output:src/main/webapp/WEB-INF/lib/*" \
              org.junit.platform.console.ConsoleLauncher --scan-class-path test_output --details summary || true
          '''

          baselineTime = (System.currentTimeMillis() - baselineStartTime) / 1000
          echo "⏱ Baseline build+test took ${baselineTime} sec"
          env.BASELINE_TIME = baselineTime.toString()
        }
      }
    }

    stage('Decide Mode Dynamically') {
      steps {
        script {
          def t = env.BASELINE_TIME.toDouble()
          def threshold = 5
          if (t >= threshold) {
            echo "⚡ Baseline ≥ ${threshold}s → Mode A (optimized)"
            env.MODE = 'A'
          } else {
            echo "📦 Baseline < ${threshold}s → Mode B (baseline)"
            env.MODE = 'B'
          }
        }
      }
    }

    stage('Validate Mode') {
      steps {
        script {
          if (!(env.MODE in ['A','B'])) {
            error "Invalid MODE=${env.MODE}"
          }
          echo "▶️ Running MODE=${env.MODE}"
        }
      }
    }

    stage('JVM Setup') {
      steps {
        script {
          jvmSetupStartTime = System.currentTimeMillis()
        }
        sh 'java -version || true'
        script {
          // optional pre‑warm in Mode A
          if (env.MODE == 'A') {
            sh '''#!/bin/bash
              java -Xshare:auto -version > /dev/null 2>&1 || true
            '''
          }
          jvmSetupEndTime = System.currentTimeMillis()
        }
      }
    }

    stage('Restore Build Cache') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          buildCacheRestoreStart = System.currentTimeMillis()
          try {
            unstash env.BUILD_CACHE_STASH
          } catch (_){
            echo "⚠️ No previous build cache"
          }
          buildCacheRestoreEnd = System.currentTimeMillis()
        }
      }
    }

    stage('Build') {
      steps {
        script {
          buildStartTime = System.currentTimeMillis()
          sh 'mkdir -p build/WEB-INF/classes'

          if (env.MODE == 'A') {
            // incremental
            def changed = sh(
              script: "find src/main/java/model -name '*.java' -newer build/WEB-INF/classes",
              returnStdout: true
            ).trim()
            if (changed) {
              echo "🔄 Incremental compile"
              sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
            } else {
              echo "🔄 No changes → full compile"
              sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
            }
          } else {
            echo "📦 Mode B full compile"
            sh "find src/main/java/model -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
          }

          sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
          sh 'cp -R src/main/webapp/* build/'
          sh "jar -cvf ${env.WAR_NAME} -C build ."

          buildTime = (System.currentTimeMillis() - buildStartTime) / 1000
          echo "✅ Build took ${buildTime} sec"
        }
      }
    }

    stage('Save Build Cache') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          buildCacheSaveStart = System.currentTimeMillis()
          stash name: env.BUILD_CACHE_STASH, includes: 'build/WEB-INF/classes/**', allowEmpty: true
          buildCacheSaveEnd = System.currentTimeMillis()
        }
      }
    }

    stage('Backup WAR') {
      steps {
        sh """#!/bin/bash
          ssh ${env.SSH_OPTS} -i ${env.SSH_KEY} ${env.SSH_USER}@${env.SSH_HOST} 'mkdir -p ${env.REMOTE_BACKUP_DIR}'
          scp ${env.SSH_OPTS} -i ${env.SSH_KEY} ${env.WAR_NAME} ${env.SSH_USER}@${env.SSH_HOST}:${env.REMOTE_BACKUP_DIR}/${env.WAR_NAME}_bak || true
        """
      }
    }

    stage('Restore Test Cache') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          testCacheRestoreStart = System.currentTimeMillis()
          try {
            unstash env.TEST_CACHE_STASH
          } catch(_) {
            echo "⚠️ No previous test cache"
          }
          testCacheRestoreEnd = System.currentTimeMillis()
        }
      }
    }

    stage('Run Tests') {
      steps {
        script {
          testStartTime = System.currentTimeMillis()
          sh 'mkdir -p test_unit test_int'
          sh "cp src/main/resources/config.properties test_unit/"

          // compile tests
          sh "javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_unit src/main/test/TestAppPart1.java"
          sh "javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_int  src/main/test/TestAppPart2.java"

          jvmStartupStartTime = System.currentTimeMillis()
          // run in parallel
          sh "java -cp 'test_unit:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > test_unit.log 2>&1 &"
          sh "java -cp 'test_int:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > test_int.log 2>&1 &"
          sh 'wait'
          jvmStartupEndTime = System.currentTimeMillis()

          testTime = (System.currentTimeMillis() - testStartTime) / 1000
          echo "✅ Tests took ${testTime} sec"
        }
      }
    }

    stage('Save Test Cache') {
      when { expression { env.MODE == 'A' } }
      steps {
        script {
          testCacheSaveStart = System.currentTimeMillis()
          stash name: env.TEST_CACHE_STASH, includes: 'test_unit.log,test_int.log', allowEmpty: true
          testCacheSaveEnd = System.currentTimeMillis()
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          deployStartTime = System.currentTimeMillis()
          // lead time
          def commitTs = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toInteger()
          leadTimeForChanges = ((deployStartTime/1000) - commitTs).toInteger()

          sh """#!/bin/bash
            scp ${env.SSH_OPTS} -i ${env.SSH_KEY} \
              ${env.WAR_NAME} ${env.SSH_USER}@${env.SSH_HOST}:${env.DEPLOY_DIR}/
            ssh ${env.SSH_OPTS} -i ${env.SSH_KEY} \
              ${env.SSH_USER}@${env.SSH_HOST} \
              'sudo rm -rf ${env.DEPLOY_DIR}/${env.WAR_NAME}* &&
               sudo ${env.TOMCAT_HOME}/bin/catalina.sh stop || true &&
               sudo ${env.TOMCAT_HOME}/bin/catalina.sh start'
          """
          deployTime = (System.currentTimeMillis() - deployStartTime) / 1000
          echo "✅ Deploy took ${deployTime} sec"
        }
      }
    }
  }

  post {
    always {
      script {
        pipelineEndTime    = System.currentTimeMillis()
        totalPipelineTime  = (pipelineEndTime - pipelineStartTime) / 1000

        // overheads
        def jvmSetupTime           = (jvmSetupEndTime    - jvmSetupStartTime   ) / 1000
        def bcRestore              = (buildCacheRestoreEnd - buildCacheRestoreStart) / 1000
        def bcSave                 = (buildCacheSaveEnd    - buildCacheSaveStart   ) / 1000
        def tcRestore              = (testCacheRestoreEnd  - testCacheRestoreStart ) / 1000
        def tcSave                 = (testCacheSaveEnd     - testCacheSaveStart    ) / 1000
        def jvmStartupTime         = (jvmStartupEndTime   - jvmStartupStartTime  ) / 1000

        // pure times
        def netBuild               = buildTime - bcRestore - bcSave
        def netTest                = testTime  - jvmStartupTime
        def netTotal               = totalPipelineTime - jvmSetupTime - bcRestore - bcSave - tcRestore - tcSave - jvmStartupTime

        echo "\n=== PIPELINE METRICS Mode - (${env.MODE}) ==="
        echo "Build Time                      : ${buildTime} sec"
        echo "Test Time                       : ${testTime} sec"
        echo "Deploy Time                     : ${deployTime} sec"
        echo "Lead Time for Change            : ${leadTimeForChanges} sec"
        echo "Total Pipeline Time             : ${totalPipelineTime} sec"
        echo "Overhead (JVM Setup)            : ${jvmSetupTime} sec"
        echo "Overhead (Cache Restore - Build): ${bcRestore} sec"
        echo "Overhead (Cache Save - Build)   : ${bcSave} sec"
        echo "Overhead (Cache Restore - Test) : ${tcRestore} sec"
        echo "Overhead (Cache Save - Test)    : ${tcSave} sec"
        echo "Overhead (JVM Startup)          : ${jvmStartupTime} sec"
        echo "➡️ Pure Build Time               : ${netBuild} sec"
        echo "➡️ Pure Test Time                : ${netTest} sec"
        echo "➡️ Pure Total Time               : ${netTotal} sec\n"

        // write CSV
        def header = "MODE,BUILD,TEST,DEPLOY,LEAD,TOTAL,JVM_SETUP,BC_RESTORE,BC_SAVE,TC_RESTORE,TC_SAVE,JVM_STARTUP,NET_BUILD,NET_TEST,NET_TOTAL\n"
        def line   = "${env.MODE},${buildTime},${testTime},${deployTime},${leadTimeForChanges},${totalPipelineTime},${jvmSetupTime},${bcRestore},${bcSave},${tcRestore},${tcSave},${jvmStartupTime},${netBuild},${netTest},${netTotal}\n"

        if (!fileExists(env.CSV_FILE)) {
          writeFile file: env.CSV_FILE, text: header + line
        } else {
          writeFile file: env.CSV_FILE, text: readFile(env.CSV_FILE) + line
        }

        archiveArtifacts artifacts: env.CSV_FILE, onlyIfSuccessful: false
      }
    }
  }
}
